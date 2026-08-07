/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests that {@link JsonProcessingExceptionMapper} maps Jackson deserialization failures to 400 responses.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(ExceptionMapperTest.ExceptionMapperResource.class)
class ExceptionMapperTest {

    @RestResource
    @RequestPath("/echo")
    private WebTarget target;

    @Test
    void malformedJsonReturns400() {
        try (Response response = target.request()
                .post(Entity.entity("{ invalid json }", MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(400, response.getStatus());
        }
    }

    @Test
    void responseBodyDoesNotExposeDetails() {
        try (Response response = target.request()
                .post(Entity.entity("{ invalid json }", MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(400, response.getStatus());
            final String body = response.readEntity(String.class);
            Assertions.assertFalse(body.contains("JacksonException"),
                    () -> "Response body should not expose exception details: %s".formatted(body));
            Assertions.assertFalse(body.contains("Unexpected"),
                    () -> "Response body should not expose parser details: %s".formatted(body));
        }
    }

    /**
     * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
     */
    @Path("/echo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class ExceptionMapperResource {

        @POST
        public Response echo(final Map<String, Object> body) {
            return Response.ok(body).build();
        }
    }
}
