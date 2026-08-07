/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.providers.jackson.api.JacksonProviderConfig;

import tools.jackson.jakarta.rs.cfg.JakartaRSFeature;

/**
 * Tests that {@link ResteasyJacksonProvider} correctly adds the {@code X-Content-Type-Options: nosniff} header
 * when {@link JakartaRSFeature#ADD_NO_SNIFF_HEADER} is enabled.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap({ NoSniffHeaderTest.NoSniffResource.class, NoSniffHeaderTest.ConfigContextResolver.class })
class NoSniffHeaderTest {

    @RestResource
    @RequestPath("/nosniff")
    private WebTarget target;

    @Test
    void noSniffHeaderIsPresent() {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("nosniff", response.getHeaderString("X-Content-Type-Options"),
                    "X-Content-Type-Options: nosniff header should be present when ADD_NO_SNIFF_HEADER is enabled");
        }
    }

    public static class ConfigContextResolver implements ContextResolver<JacksonProviderConfig> {

        @Override
        public JacksonProviderConfig getContext(final Class<?> type) {
            return new JacksonProviderConfig(Set.of(JakartaRSFeature.ADD_NO_SNIFF_HEADER), Set.of());
        }
    }

    @Path("/nosniff")
    @Produces(MediaType.APPLICATION_JSON)
    public static class NoSniffResource {

        @GET
        public Map<String, String> get() {
            return Map.of("status", "ok");
        }
    }
}
