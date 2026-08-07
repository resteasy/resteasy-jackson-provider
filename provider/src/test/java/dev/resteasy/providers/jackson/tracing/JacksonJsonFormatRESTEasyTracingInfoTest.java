/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.tracing;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.tracing.api.RESTEasyTracing;
import org.jboss.resteasy.tracing.api.RESTEasyTracingInfoFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests that {@link JacksonJsonFormatRESTEasyTracingInfo} produces JSON-formatted tracing headers when tracing is
 * enabled and the JSON format is requested.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(application = JacksonJsonFormatRESTEasyTracingInfoTest.TracingApplication.class)
class JacksonJsonFormatRESTEasyTracingInfoTest {

    @Test
    void supportsJsonFormat() {
        final JacksonJsonFormatRESTEasyTracingInfo info = new JacksonJsonFormatRESTEasyTracingInfo();
        Assertions.assertTrue(info.supports(RESTEasyTracingInfoFormat.JSON));
        Assertions.assertFalse(info.supports(RESTEasyTracingInfoFormat.TEXT));
    }

    @Test
    void jsonTracingHeaders(@RestResource @RequestPath("/tracing") final WebTarget target) {
        try (Response response = target.request()
                .header(RESTEasyTracing.HEADER_ACCEPT_FORMAT, "JSON")
                .get()) {
            Assertions.assertEquals(200, response.getStatus());

            boolean hasTracingHeader = false;
            final StringBuilder allHeaders = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : response.getStringHeaders().entrySet()) {
                allHeaders.append(entry.getKey()).append('=').append(entry.getValue()).append("; ")
                        .append(System.lineSeparator());
                if (entry.getKey().startsWith(RESTEasyTracing.HEADER_TRACING_PREFIX)) {

                    hasTracingHeader = true;
                    final String value = entry.getValue().toString();
                    Assertions.assertTrue(value.contains("{") || value.contains("["),
                            () -> "Expected JSON in tracing header, got: %s".formatted(value));
                }
            }
            Assertions.assertTrue(hasTracingHeader,
                    () -> "Response should contain tracing headers. Got: %s".formatted(allHeaders));
        }
    }

    /**
     *
     * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
     */
    @ApplicationPath("/")
    public static class TracingApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TracingResource.class);
        }

        @Override
        public Map<String, Object> getProperties() {
            return Map.of("resteasy.server.tracing.type", "ALL", "resteasy.server.tracing.threshold", "VERBOSE");
        }
    }

    /**
     * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
     */
    @Path("/tracing")
    @Produces(MediaType.APPLICATION_JSON)
    public static class TracingResource {

        @GET
        public String get() {
            return "{\"status\":\"ok\"}";
        }
    }
}
