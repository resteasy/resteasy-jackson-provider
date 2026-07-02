/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.providers.jackson.allowlist.resources;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;

import dev.resteasy.providers.jackson.AllowListPolymorphicTypeValidatorBuilder;
import dev.resteasy.providers.jackson.allowlist.resources.land.Automobile2;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.json.JsonMapper;

@Produces(MediaType.APPLICATION_JSON)
public class JacksonConfig implements ContextResolver<JsonMapper> {
    private final JsonMapper mapper;

    public JacksonConfig() {
        mapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .polymorphicTypeValidator(AllowListPolymorphicTypeValidatorBuilder.builder()
                        .allowIfBaseType(Automobile2.class)
                        .allowIfSubType(Automobile2.class)
                        .build())
                .build();
    }

    @Override
    public JsonMapper getContext(final Class<?> type) {
        return mapper;
    }
}
