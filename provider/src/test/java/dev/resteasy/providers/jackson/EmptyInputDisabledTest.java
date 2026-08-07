/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

import tools.jackson.jakarta.rs.cfg.JakartaRSFeature;

/**
 * Tests that {@link ResteasyJacksonProvider} correctly throws
 * {@link jakarta.ws.rs.core.NoContentException} when
 * {@link JakartaRSFeature#ALLOW_EMPTY_INPUT} is disabled and the request body is empty.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap({ EmptyInputDisabledTest.StrictResource.class, EmptyInputDisabledTest.ConfigContextResolver.class })
class EmptyInputDisabledTest {

    @RestResource
    @RequestPath("/strict")
    private WebTarget target;

    @Test
    void emptyBodyReturnsBadRequest() {
        try (Response response = target.request()
                .post(Entity.entity("", MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(400, response.getStatus(),
                    () -> "Expected 400 Bad Request when ALLOW_EMPTY_INPUT is disabled, got %d"
                            .formatted(response.getStatus()));
        }
    }

    @Test
    void nonEmptyBodySucceeds() {
        final String json = """
                {"message": "hello"}
                """;
        try (Response response = target.request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus());
        }
    }

    public static class ConfigContextResolver implements ContextResolver<JacksonProviderConfig> {

        @Override
        public JacksonProviderConfig getContext(final Class<?> type) {
            return new JacksonProviderConfig(Set.of(), Set.of(JakartaRSFeature.ALLOW_EMPTY_INPUT));
        }
    }

    @Path("/strict")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class StrictResource {

        @POST
        public Response echo(final Map<String, Object> body) {
            return Response.ok(body).build();
        }
    }
}
