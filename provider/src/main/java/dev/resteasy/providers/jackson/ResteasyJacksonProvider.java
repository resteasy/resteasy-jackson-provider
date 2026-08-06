/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.core.interception.jaxrs.DecoratorMatcher;
import org.jboss.resteasy.core.messagebody.AsyncBufferedMessageBodyWriter;
import org.jboss.resteasy.plugins.providers.ProviderHelper;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.util.DelegatingOutputStream;

import dev.resteasy.providers.jackson._private.JacksonLogger;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.jakarta.rs.base.util.ClassKey;
import tools.jackson.jakarta.rs.cfg.ObjectWriterInjector;
import tools.jackson.jakarta.rs.cfg.ObjectWriterModifier;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;
import tools.jackson.jakarta.rs.json.JsonEndpointConfig;

/**
 * Only different from Jackson one is *+json in @Produces/@Consumes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Consumes({ "application/json", "application/*+json", "text/json" })
@Produces({ "application/json", "application/*+json", "text/json" })
public class ResteasyJacksonProvider extends JacksonJsonProvider implements AsyncBufferedMessageBodyWriter<Object> {

    // TODO (jrp) no need for these to be exposed
    private final ConcurrentHashMap<ClassAnnotationKey, JsonEndpointConfig> _readers = new ConcurrentHashMap<ClassAnnotationKey, JsonEndpointConfig>();
    private final ConcurrentHashMap<ClassAnnotationKey, Boolean> decorators = new ConcurrentHashMap<ClassAnnotationKey, Boolean>();

    private final DecoratorMatcher decoratorMatcher = new DecoratorMatcher();

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return super.isReadable(aClass, type, annotations, mediaType);
    }

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return super.isWriteable(aClass, type, annotations, mediaType);
    }

    // Currently we need to override readFrom and writeTo because Jackson 2.2.1 does not cache correctly
    // It does not allow to have a ContextResolver that chooses different mappers per Java type.

    private static class ClassAnnotationKey {
        private AnnotationArrayKey annotations;
        private ClassKey classKey;
        private int hash;

        private ClassAnnotationKey(final Class<?> clazz, final Annotation[] annotations) {
            this.annotations = new AnnotationArrayKey(annotations);
            this.classKey = new ClassKey(clazz);
            hash = this.annotations.hashCode();
            hash = 31 * hash + classKey.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ClassAnnotationKey that = (ClassAnnotationKey) o;

            if (!annotations.equals(that.annotations))
                return false;
            if (!classKey.equals(that.classKey))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    // Alternative to Jackson's AnnotationBundleKey that uses object equality
    // instead of referential equality (==) due to how parameter annotations are proxied and not cached.
    private static class AnnotationArrayKey {
        private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

        private final Annotation[] annotations;
        private final int hash;

        private AnnotationArrayKey(final Annotation[] annotations) {
            if (annotations == null || annotations.length == 0) {
                this.annotations = NO_ANNOTATIONS;
            } else {
                this.annotations = annotations;
            }
            this.hash = calcHash(this.annotations);
        }

        private static int calcHash(Annotation[] annotations) {
            int result = annotations.length;
            result = 31 * result + Arrays.hashCode(annotations);
            return result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object)
                return true;
            if (object == null || getClass() != object.getClass())
                return false;
            AnnotationArrayKey that = (AnnotationArrayKey) object;
            return hash == that.hash && java.util.Arrays.equals(annotations, that.annotations);
        }
    }

    @Override
    public Object readFrom(Class<Object> type, final Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws JacksonException {
        JacksonLogger.LOGGER.debugf("Provider : %s,  Method : readFrom", getClass().getName());
        ClassAnnotationKey key = new ClassAnnotationKey(type, annotations);
        JsonEndpointConfig endpoint;
        endpoint = _readers.get(key);
        // not yet resolved (or not cached any more)? Resolve!
        if (endpoint == null) {
            JsonMapper mapper = locateMapper(type, mediaType)
                    .rebuild()
                    .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                    .build();
            // TODO (jrp) for now we will always add our AllowListPolymorphicTypeValidatorBuilder, but we need to
            // TODO (jrp) determine if that is correct.
            //PolymorphicTypeValidator ptv = mapper.getPolymorphicTypeValidator();
            //the check is protected by test dev.resteasy.providers.jackson.allowlist.JacksonConfig,
            //be sure to keep that in synch if changing anything here.
            //if (ptv == null || ptv instanceof LaissezFaireSubTypeValidator) {
            //    mapper.setPolymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build());
            //}
            endpoint = _configForReading(mapper, annotations, null);
            _readers.put(key, endpoint);
        }
        final ObjectReader reader = endpoint.getReader();
        try (JsonParser jp = _createParser(reader, entityStream)) {
            // If null is returned, considered to be empty stream
            if (jp == null) {
                return null;
            } else if (jp.nextToken() == null) {
                return null;
            }

            // [Issue#1]: allow 'binding' to JsonParser
            if (((Class<?>) type) == JsonParser.class) {
                return jp;
            }
            return reader.forType(reader.getTypeFactory().constructType(genericType)).readValue(jp);
        }
    }

    // TODO (jrp) no need to expose this
    protected final ConcurrentHashMap<ClassAnnotationKey, JsonEndpointConfig> _writers = new ConcurrentHashMap<ClassAnnotationKey, JsonEndpointConfig>();

    private static final class LazyByteArrayOutputStream extends OutputStream {

        private byte[] buf;
        private int count;

        private void ensureCapacity(int minCapacity) {
            if (minCapacity < 0) {
                throw new OutOfMemoryError();
            }
            if (buf == null) {
                buf = new byte[minCapacity];
                return;
            }
            int oldCapacity = buf.length;
            int minGrowth = minCapacity - oldCapacity;
            if (minGrowth > 0) {
                grow(minGrowth, oldCapacity);
            }
        }

        private void grow(int minGrowth, int oldCapacity) {
            int newCapacity = oldCapacity + Math.max((oldCapacity >> 1), minGrowth);
            if (newCapacity < 0) {
                // if we cannot grow as much as we want, let's just grow to what we need
                newCapacity = oldCapacity + minGrowth;
                if (newCapacity < 0) {
                    throw new OutOfMemoryError();
                }
            }
            buf = Arrays.copyOf(buf, newCapacity);
        }

        @Override
        public void write(int b) {
            ensureCapacity(count + 1);
            buf[count] = (byte) b;
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ensureCapacity(count + len);
            System.arraycopy(b, off, buf, count, len);
            count += len;
        }
    }

    private static final byte[] EMPTY = new byte[0];

    @Override
    public CompletionStage<Void> asyncWriteTo(Object t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, AsyncOutputStream entityStream) {
        LazyByteArrayOutputStream bos = new LazyByteArrayOutputStream();
        try {
            writeTo(t, type, genericType, annotations, mediaType, httpHeaders, bos);
            byte[] array = bos.buf;
            if (array == null) {
                array = EMPTY;
            }
            bos.buf = null;
            return entityStream.asyncWrite(array, 0, bos.count);
        } catch (WebApplicationException | JacksonException e) {
            return ProviderHelper.completedException(e);
        }
    }

    @Override
    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws JacksonException {
        JacksonLogger.LOGGER.debugf("Provider : %s,  Method : writeTo", getClass().getName());
        entityStream = new DelegatingOutputStream(entityStream) {
            @Override
            public void flush() throws IOException {
                // don't flush as this is a performance hit on Undertow.
                // and causes chunked encoding to happen.
            }
        };
        ClassAnnotationKey key = new ClassAnnotationKey(type, annotations);
        JsonEndpointConfig endpoint;
        endpoint = _writers.get(key);

        // not yet resolved (or not cached any more)? Resolve!
        if (endpoint == null) {
            JsonMapper mapper = locateMapper(type, mediaType)
                    .rebuild()
                    .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                    .build();
            // TODO (jrp) for now we will always add our AllowListPolymorphicTypeValidatorBuilder, but we need to
            // TODO (jrp) determine if that is correct.
            //PolymorphicTypeValidator ptv = mapper.getPolymorphicTypeValidator();
            //the check is protected by test dev.resteasy.providers.jackson.allowlist.JacksonConfig,
            //be sure to keep that in synch if changing anything here.
            //if (ptv == null || ptv instanceof LaissezFaireSubTypeValidator) {
            //    mapper.setPolymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build());
            //}
            endpoint = _configForWriting(mapper, annotations, null);

            // and cache for future reuse
            _writers.put(key, endpoint);
        }

        ObjectWriter writer = endpoint.getWriter()
                .withFeatures(StreamWriteFeature.AUTO_CLOSE_TARGET);

        /*
         * 27-Feb-2009, tatu: Where can we find desired encoding? Within
         * HTTP headers?
         */
        JsonEncoding enc = findEncoding(mediaType, httpHeaders);

        try (JsonGenerator jg = writer.createGenerator(entityStream, enc)) {
            // Want indentation?
            // TODO (jrp) verify if we also need to check the mapper here
            if (writer.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
                //jg.useDefaultPrettyPrinter();
            }
            // 04-Mar-2010, tatu: How about type we were given? (if any)
            JavaType rootType = null;

            if (genericType != null && value != null) {
                /*
                 * 10-Jan-2011, tatu: as per [JACKSON-456], it's not safe to just force root
                 * type since it prevents polymorphic type serialization. Since we really
                 * just need this for generics, let's only use generic type if it's truly
                 * generic.
                 */
                if (genericType.getClass() != Class.class) { // generic types are other impls of 'java.lang.reflect.Type'
                    /*
                     * This is still not exactly right; should root type be further
                     * specialized with 'value.getClass()'? Let's see how well this works before
                     * trying to come up with more complete solution.
                     */
                    rootType = writer.getTypeFactory().constructType(genericType);
                    /*
                     * 26-Feb-2011, tatu: To help with [JACKSON-518], we better recognize cases where
                     * type degenerates back into "Object.class" (as is the case with plain TypeVariable,
                     * for example), and not use that.
                     */
                    if (rootType.getRawClass() == Object.class) {
                        rootType = null;
                    }
                }
            }

            // Most of the configuration now handled through EndpointConfig, ObjectWriter
            // but we may need to force root type:
            if (rootType != null) {
                writer = writer.forType(rootType);
            }
            value = endpoint.modifyBeforeWrite(value);
            ObjectWriterModifier mod = ObjectWriterInjector.getAndClear();
            if (mod == null) {
                final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                mod = ResteasyObjectWriterInjector.get(tccl);
            }
            if (mod != null) {
                // TODO (jrp) previously we added the JsonGenerator as the last argument here and we need to figure out
                // TODO (jrp) why that was and if we still need to do that.
                writer = mod.modify(endpoint, httpHeaders, value, writer);
            }

            // [RESTEASY-1317] Support Jackson in Atom links
            Boolean hasDecorator = decorators.get(key);
            if (hasDecorator == null) {
                if (decoratorMatcher.hasDecorator(DecoratedEntityContainer.class, annotations)) {
                    decoratorMatcher
                            .decorate(DecoratedEntityContainer.class, new DecoratedEntityContainer(value), type, annotations,
                                    mediaType);
                    decorators.put(key, Boolean.TRUE);
                } else {
                    decorators.put(key, Boolean.FALSE);
                }
            } else {
                if (hasDecorator) {
                    decoratorMatcher
                            .decorate(DecoratedEntityContainer.class, new DecoratedEntityContainer(value), type, annotations,
                                    mediaType);
                }
            }
            writer.writeValue(jg, value);
        }
    }
}
