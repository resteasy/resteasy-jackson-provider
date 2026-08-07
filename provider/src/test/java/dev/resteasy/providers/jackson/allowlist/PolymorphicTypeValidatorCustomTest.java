/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.allowlist;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.providers.jackson.allowlist.resources.PolymorphicResource;
import dev.resteasy.providers.jackson.allowlist.resources.TestPolymorphicType;
import dev.resteasy.providers.jackson.allowlist.resources.air.Aircraft;
import dev.resteasy.providers.jackson.allowlist.resources.land.Automobile2;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Verifies that when a user provides a custom {@link tools.jackson.databind.jsontype.PolymorphicTypeValidator} via a
 * {@link ContextResolver}, the provider respects it rather than overriding it with the default
 * {@link dev.resteasy.providers.jackson.AllowListPolymorphicTypeValidatorBuilder}.
 *
 * <p>
 * The custom validator only allows {@link Automobile2} as a subtype. Sending an {@link Aircraft} should be rejected
 * by the server.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap({
        PolymorphicResource.class,
        PolymorphicTypeValidatorCustomTest.CustomValidatorJsonMapperProvider.class,
})
public class PolymorphicTypeValidatorCustomTest {

    @Test
    public void allowedTypeShouldSucceed(@RestResource @RequestPath("/test") final WebTarget target) {
        final TestPolymorphicType testEntity = new TestPolymorphicType(new Automobile2());
        try (Response response = target.request().post(Entity.json(testEntity))) {
            Assertions.assertEquals(201, response.getStatus(),
                    () -> "Expected 201 but got %d: %s".formatted(response.getStatus(),
                            response.readEntity(String.class)));
        }
    }

    @Test
    public void disallowedTypeShouldFail(@RestResource @RequestPath("/test") final WebTarget target) {
        final TestPolymorphicType testEntity = new TestPolymorphicType(new Aircraft());
        try (Response response = target.request().post(Entity.json(testEntity))) {
            Assertions.assertEquals(400, response.getStatus(),
                    "Aircraft should be rejected by the custom validator that only allows Automobile2");
        }
    }

    @Provider
    @Produces(MediaType.APPLICATION_JSON)
    public static class CustomValidatorJsonMapperProvider implements ContextResolver<JsonMapper> {
        private final JsonMapper mapper;

        public CustomValidatorJsonMapperProvider() {
            mapper = JsonMapper.builder()
                    .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                            .allowIfSubType(Automobile2.class)
                            .build())
                    .build();
        }

        @Override
        public JsonMapper getContext(final Class<?> type) {
            return mapper;
        }
    }
}
