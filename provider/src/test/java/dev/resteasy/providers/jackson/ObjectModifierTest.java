/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.jakarta.rs.cfg.EndpointConfigBase;
import tools.jackson.jakarta.rs.cfg.ObjectReaderInjector;
import tools.jackson.jakarta.rs.cfg.ObjectReaderModifier;
import tools.jackson.jakarta.rs.cfg.ObjectWriterInjector;
import tools.jackson.jakarta.rs.cfg.ObjectWriterModifier;

/**
 * Tests that {@link ResteasyJacksonProvider} correctly applies {@link ObjectReaderModifier} and
 * {@link ObjectWriterModifier} when injected via {@link ObjectReaderInjector} and {@link ObjectWriterInjector}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap({
        ObjectModifierTest.ModifierResource.class,
        ObjectModifierTest.WriterModifierFilter.class,
        ObjectModifierTest.ReaderModifierFilter.class,
})
class ObjectModifierTest {

    @RestResource
    @RequestPath("/modifier")
    private WebTarget target;

    @Test
    void writerModifierAddsCustomHeader() {
        try (Response response = target.path("/get").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("modified", response.getHeaderString("X-Writer-Modified"),
                    "ObjectWriterModifier should have added the X-Writer-Modified header");
        }
    }

    @Test
    void writerModifierEnablesIndentation() {
        try (Response response = target.path("/get").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            final String body = response.readEntity(String.class);
            Assertions.assertTrue(body.contains("\n"),
                    () -> "ObjectWriterModifier should have enabled indentation, but got: %s".formatted(body));
        }
    }

    @Test
    void readerModifierRejectsUnknownProperties() {
        final String json = """
                {"name": "Alice", "unknownField": "should fail"}
                """;
        try (Response response = target.path("/post")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(400, response.getStatus(),
                    () -> "ObjectReaderModifier should have enabled FAIL_ON_UNKNOWN_PROPERTIES, got %d"
                            .formatted(response.getStatus()));
        }
    }

    @Test
    void readerModifierAllowsKnownProperties() {
        final String json = """
                {"name": "Alice"}
                """;
        try (Response response = target.path("/post")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus());
            final Map<String, Object> result = response.readEntity(new GenericType<Map<String, Object>>() {
            });
            Assertions.assertEquals("Alice", result.get("name"));
        }
    }

    @Provider
    public static class WriterModifierFilter implements ContainerRequestFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext) {
            ObjectWriterInjector.set(new IndentingWriterModifier());
        }
    }

    @Provider
    public static class ReaderModifierFilter implements ContainerRequestFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext) {
            if ("POST".equals(requestContext.getMethod())) {
                ObjectReaderInjector.set(new StrictReaderModifier());
            }
        }
    }

    @Path("/modifier")
    @Produces(MediaType.APPLICATION_JSON)
    public static class ModifierResource {

        @GET
        @Path("/get")
        public SimpleModel get() {
            final SimpleModel model = new SimpleModel();
            model.name = "test";
            return model;
        }

        @POST
        @Path("/post")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response post(final SimpleModel model) {
            return Response.ok(Map.of("name", model.name)).build();
        }
    }

    public static class SimpleModel {
        public String name;
    }

    static class IndentingWriterModifier extends ObjectWriterModifier {
        @Override
        public ObjectWriter modify(final EndpointConfigBase<?> endpoint,
                final MultivaluedMap<String, Object> responseHeaders,
                final Object valueToWrite,
                final ObjectWriter w) throws JacksonException {
            responseHeaders.putSingle("X-Writer-Modified", "modified");
            return w.with(SerializationFeature.INDENT_OUTPUT);
        }
    }

    static class StrictReaderModifier extends ObjectReaderModifier {
        @Override
        public ObjectReader modify(final EndpointConfigBase<?> endpoint,
                final MultivaluedMap<String, String> httpHeaders,
                final JavaType resultType,
                final ObjectReader r,
                final JsonParser p) throws JacksonException {
            return r.with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
    }
}
