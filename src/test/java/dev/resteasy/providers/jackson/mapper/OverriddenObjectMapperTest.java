/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.mapper;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.providers.jackson.mapper.resources.ContactResource;
import dev.resteasy.providers.jackson.mapper.resources.CrudResource;
import dev.resteasy.providers.jackson.mapper.resources.UserResource;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests that a custom {@link JsonMapper} registered via a {@link ContextResolver} is found and used during
 * serialization/deserialization.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap({
        ContactResource.class,
        CrudResource.class,
        UserResource.class,
        OverriddenObjectMapperTest.CustomJsonMapperProvider.class,
})
public class OverriddenObjectMapperTest extends AbstractObjectMapperTest {

    public OverriddenObjectMapperTest() {
        super(true);
    }

    @Provider
    public static class CustomJsonMapperProvider implements ContextResolver<JsonMapper> {

        @Override
        public JsonMapper getContext(final Class<?> type) {
            return JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();
        }
    }
}
