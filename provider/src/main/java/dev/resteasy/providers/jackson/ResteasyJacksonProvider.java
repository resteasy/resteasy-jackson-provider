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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.core.messagebody.AsyncBufferedMessageBodyWriter;
import org.jboss.resteasy.plugins.providers.ProviderHelper;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.util.DelegatingOutputStream;

import dev.resteasy.providers.jackson._private.JacksonLogger;
import dev.resteasy.providers.jackson.api.JacksonProviderConfig;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.jakarta.rs.base.util.ClassKey;
import tools.jackson.jakarta.rs.cfg.JakartaRSFeature;
import tools.jackson.jakarta.rs.cfg.ObjectReaderInjector;
import tools.jackson.jakarta.rs.cfg.ObjectReaderModifier;
import tools.jackson.jakarta.rs.cfg.ObjectWriterInjector;
import tools.jackson.jakarta.rs.cfg.ObjectWriterModifier;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;
import tools.jackson.jakarta.rs.json.JsonEndpointConfig;

/**
 * A Jakarta REST provider for JSON serialization and deserialization using Jackson. Extends Jackson's
 * {@link JacksonJsonProvider} with additional media type support ({@code application/*+json}, {@code text/json}),
 * polymorphic type validation via {@link AllowListPolymorphicTypeValidatorBuilder}, and asynchronous write support.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Consumes({ "application/json", "application/*+json", "text/json" })
@Produces({ "application/json", "application/*+json", "text/json" })
public class ResteasyJacksonProvider extends JacksonJsonProvider implements AsyncBufferedMessageBodyWriter<Object> {

    private static final byte[] EMPTY = new byte[0];

    private final ConcurrentHashMap<ClassAnnotationKey, JsonEndpointConfig> readers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ClassAnnotationKey, JsonEndpointConfig> writers = new ConcurrentHashMap<>();

    @Context
    private Providers providers;

    private volatile boolean needsFeatureInit = true;

    @Override
    public Object readFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
            throws JacksonException, NoContentException {
        initializeFeatures(type, mediaType);
        JacksonLogger.LOGGER.debugf("Provider : %s,  Method : readFrom", getClass().getName());
        final ClassAnnotationKey key = new ClassAnnotationKey(new AnnotationArrayKey(annotations), new ClassKey(type));
        JsonEndpointConfig endpoint = readers.get(key);
        // not yet resolved (or not cached any more)? Resolve!
        if (endpoint == null) {
            JsonMapper mapper = locateMapper(type, mediaType);
            if (mapper.deserializationConfig().getPolymorphicTypeValidator() instanceof DefaultBaseTypeLimitingValidator) {
                mapper = mapper.rebuild()
                        .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                        .build();
            }
            endpoint = _configForReading(mapper, annotations, _defaultReadView);
            readers.put(key, endpoint);
        }
        ObjectReader reader = endpoint.getReader();
        final JsonParser p = _createParser(reader, entityStream);

        // If null is returned, considered to be empty stream
        if (p == null || p.nextToken() == null) {
            if (JakartaRSFeature.ALLOW_EMPTY_INPUT.enabledIn(_jakartaRSFeatures)) {
                return null;
            }
            throw _createNoContentException();
        }
        if ((Class<?>) type == JsonParser.class) {
            return p;
        }
        final TypeFactory tf = reader.typeFactory();
        final JavaType resolvedType = tf.constructType(genericType);

        final boolean multiValued = ((Class<?>) type == MappingIterator.class);

        if (multiValued) {
            final JavaType[] contents = tf.findTypeParameters(resolvedType, MappingIterator.class);
            final JavaType valueType = (contents == null || contents.length == 0) ? tf.constructType(Object.class)
                    : contents[0];
            reader = reader.forType(valueType);
        } else {
            reader = reader.forType(resolvedType);
        }

        // Allow modification by filter-injectable thing
        final ObjectReaderModifier mod = ObjectReaderInjector.getAndClear();
        if (mod != null) {
            reader = mod.modify(endpoint, httpHeaders, resolvedType, reader, p);
        }

        if (multiValued) {
            // Advance past START_ARRAY so MappingIterator sees the first element token.
            // readValues(JsonParser) uses managedParser=false, which does not auto-skip it.
            if (p.currentToken() == JsonToken.START_ARRAY) {
                p.nextToken();
            }
            return reader.readValues(p);
        }
        try {
            return reader.readValue(p);
        } finally {
            // Close the parser to return internal buffers to Jackson's recycler pool.
            // AUTO_CLOSE_SOURCE is disabled, so this won't close the container-managed InputStream.
            p.close();
        }
    }

    @Override
    public CompletionStage<Void> asyncWriteTo(final Object t, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
            final AsyncOutputStream entityStream) {
        initializeFeatures(type, mediaType);
        final LazyByteArrayOutputStream bos = new LazyByteArrayOutputStream();
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
    public void writeTo(Object value, final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws JacksonException {
        initializeFeatures(type, mediaType);
        JacksonLogger.LOGGER.debugf("Provider : %s,  Method : writeTo", getClass().getName());
        entityStream = new DelegatingOutputStream(entityStream) {
            @Override
            public void flush() throws IOException {
                // don't flush as this is a performance hit on Undertow.
                // and causes chunked encoding to happen.
            }
        };
        final ClassAnnotationKey key = new ClassAnnotationKey(new AnnotationArrayKey(annotations), new ClassKey(type));
        JsonEndpointConfig endpoint = writers.get(key);

        // not yet resolved (or not cached any more)? Resolve!
        if (endpoint == null) {
            JsonMapper mapper = locateMapper(type, mediaType);
            if (mapper.serializationConfig().getPolymorphicTypeValidator() instanceof DefaultBaseTypeLimitingValidator) {
                mapper = mapper.rebuild()
                        .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                        .build();
            }
            endpoint = _configForWriting(mapper, annotations, _defaultWriteView);

            // and cache for future reuse
            writers.put(key, endpoint);
        }

        // Any headers we should write?
        _modifyHeaders(value, type, genericType, annotations, httpHeaders, endpoint);

        ObjectWriter writer = endpoint.getWriter();

        final JsonEncoding enc = findEncoding(mediaType, httpHeaders);
        JavaType rootType = null;

        if ((genericType != null) && (value != null)) {
            // Only use generic type for actual generic types (ParameterizedType, etc.),
            // not plain Class, to avoid breaking polymorphic type serialization.
            if (!(genericType instanceof Class<?>)) {
                final TypeFactory typeFactory = writer.typeFactory();
                final JavaType baseType = typeFactory.constructType(genericType);
                rootType = typeFactory.constructSpecializedType(baseType, type);
                // A plain TypeVariable resolves to Object.class — ignore it.
                if (rootType.getRawClass() == Object.class) {
                    rootType = null;
                }
            }
        }

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
            writer = mod.modify(endpoint, httpHeaders, value, writer);
        }

        try (JsonGenerator jg = _createGenerator(writer, entityStream, enc)) {
            writer.writeValue(jg, value);
        }
    }

    private void initializeFeatures(final Class<?> type, final MediaType mediaType) {
        if (needsFeatureInit) {
            synchronized (this) {
                if (needsFeatureInit) {
                    final ContextResolver<JacksonProviderConfig> resolver = providers
                            .getContextResolver(JacksonProviderConfig.class, mediaType);
                    if (resolver != null) {
                        final JacksonProviderConfig config = resolver.getContext(type);
                        if (config != null) {
                            config.disabledFeatures().forEach(this::disable);
                            config.enabledFeatures().forEach(this::enable);
                        }
                    }
                    needsFeatureInit = false;
                }
            }
        }
    }

    private record ClassAnnotationKey(AnnotationArrayKey annotations, ClassKey classKey) {
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
            return hash == that.hash && Arrays.equals(annotations, that.annotations);
        }

        private static int calcHash(Annotation[] annotations) {
            int result = annotations.length;
            result = 31 * result + Arrays.hashCode(annotations);
            return result;
        }
    }

    private static final class LazyByteArrayOutputStream extends OutputStream {

        private byte[] buf;
        private int count;

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
    }
}
