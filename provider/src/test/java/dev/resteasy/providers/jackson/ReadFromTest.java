/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.MappingIterator;

/**
 * Tests for {@link ResteasyJacksonProvider#readFrom} covering empty input handling, {@link JsonParser} as an entity
 * type, {@link MappingIterator} streaming, and generic type deserialization.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(ReadFromTest.ReadFromResource.class)
class ReadFromTest {

    @RestResource
    @RequestPath("/read")
    private WebTarget target;

    @Test
    void emptyBodyReturnsNull() {
        try (Response response = target.path("/echo")
                .request()
                .post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(204, response.getStatus(),
                    () -> "Expected 204 No Content for empty input, got %d: %s"
                            .formatted(response.getStatus(), response.readEntity(String.class)));
        }
    }

    @Test
    void deserializesJsonObject() {
        final String json = """
                {"name": "Alice", "value": 42}
                """;
        try (Response response = target.path("/echo")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus());
            final Map<?, ?> result = response.readEntity(new GenericType<Map<String, Object>>() {
            });
            Assertions.assertEquals("Alice", result.get("name"));
            Assertions.assertEquals(42, ((Number) result.get("value")).intValue());
        }
    }

    @Test
    void jsonParserAsEntityType() {
        final String json = """
                {"x": 10, "y": 20}
                """;
        try (Response response = target.path("/parser")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus());
            final Map<?, ?> result = response.readEntity(new GenericType<Map<String, Object>>() {
            });
            Assertions.assertEquals(10, ((Number) result.get("x")).intValue());
            Assertions.assertEquals(20, ((Number) result.get("y")).intValue());
        }
    }

    @Test
    void mappingIterator() {
        final String json = """
                [{"x": 1, "y": 2}, {"x": 3, "y": 4}, {"x": 5, "y": 6}]
                """;
        try (Response response = target.path("/iterator")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus(),
                    () -> "MappingIterator deserialization failed: %s".formatted(response.readEntity(String.class)));
            final List<Point> points = response.readEntity(new GenericType<>() {
            });
            Assertions.assertEquals(3, points.size(), () -> "Expected 3 points, got %s".formatted(points));
            Assertions.assertEquals(1, points.get(0).x);
            Assertions.assertEquals(6, points.get(2).y);
        }
    }

    @Test
    void deserializesGenericList() {
        final String json = """
                [{"x": 10, "y": 20}, {"x": 30, "y": 40}]
                """;
        try (Response response = target.path("/list")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus());
            final List<Point> points = response.readEntity(new GenericType<>() {
            });
            Assertions.assertEquals(2, points.size());
            Assertions.assertEquals(10, points.get(0).x);
            Assertions.assertEquals(40, points.get(1).y);
        }
    }

    @Path("/read")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class ReadFromResource {

        @POST
        @Path("/echo")
        public Response echo(final Map<String, Object> body) {
            if (body == null) {
                return Response.noContent().build();
            }
            return Response.ok(body).build();
        }

        @POST
        @Path("/parser")
        public Response processParser(final JsonParser parser) {
            final Map<String, Object> result = new LinkedHashMap<>();
            try {
                // The provider already called nextToken(), so we're at START_OBJECT
                if (parser.currentToken() == JsonToken.START_OBJECT) {
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        final String fieldName = parser.currentName();
                        parser.nextToken();
                        result.put(fieldName, parser.getIntValue());
                    }
                }
            } catch (Exception e) {
                return Response.serverError().entity(Map.of("error", e.getMessage())).build();
            }
            return Response.ok(result).build();
        }

        @POST
        @Path("/iterator")
        public Response processIterator(final MappingIterator<Point> iterator) {
            final List<Point> points = new ArrayList<>();
            while (iterator.hasNext()) {
                points.add(iterator.next());
            }
            return Response.ok(points).build();
        }

        @POST
        @Path("/list")
        public Response processList(final List<Point> points) {
            return Response.ok(points).build();
        }
    }

    public static class Point {
        public int x;
        public int y;

        public Point() {
        }

        public Point(final int x, final int y) {
            this.x = x;
            this.y = y;
        }
    }
}
