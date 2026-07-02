/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import dev.resteasy.providers.jackson.AllowListPolymorphicTypeValidatorBuilder;

import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
abstract class ObjectMapperProvider {

    @Context
    private Providers providers;

    private volatile JsonMapper jsonMapper;

    JsonMapper locateMapper(final Class<?> type, final MediaType mediaType) {
        // TODO (jrp) look at the tools.jackson.jakarta.rs.json.JacksonJsonProvider.locateMapper() and do something
        // TODO (jrp) similar
        JsonMapper currentObjectMapper = jsonMapper;
        if (currentObjectMapper == null) {
            synchronized (this) {
                currentObjectMapper = jsonMapper;
                if (currentObjectMapper == null) {
                    final JsonMapper contextMapper = resolveContextJsonMapper(type, mediaType);
                    if (contextMapper != null) {
                        currentObjectMapper = contextMapper
                                .rebuild()
                                .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                                .build();

                    } else {
                        currentObjectMapper = createDefaultObjectMapper();
                    }
                    // TODO (jrp) for now we will always add our AllowListPolymorphicTypeValidatorBuilder, but we need to
                    // TODO (jrp) determine if that is correct.
                    //PolymorphicTypeValidator ptv = mapper.getPolymorphicTypeValidator();
                    //the check is protected by test dev.resteasy.providers.jackson.allowlist.JacksonConfig,
                    //be sure to keep that in synch if changing anything here.
                    //if (ptv == null || ptv instanceof LaissezFaireSubTypeValidator) {
                    //    mapper.setPolymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build());
                    //}

                    this.jsonMapper = currentObjectMapper;
                }
            }
        }
        return currentObjectMapper;
    }

    private JsonMapper resolveContextJsonMapper(final Class<?> type, final MediaType mediaType) {
        if (providers == null) {
            return null;
        }
        final ContextResolver<JsonMapper> resolver = providers.getContextResolver(JsonMapper.class, mediaType);
        if (resolver == null) {
            return null;
        }
        return resolver.getContext(type);
    }

    private JsonMapper createDefaultObjectMapper() {
        JsonMapper mapper = JsonMapper.builder()
                .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                .build();
        // TODO (jrp) for now we will always add our AllowListPolymorphicTypeValidatorBuilder, but we need to
        // TODO (jrp) determine if that is correct.
        //PolymorphicTypeValidator ptv = mapper.getPolymorphicTypeValidator();
        //the check is protected by test dev.resteasy.providers.jackson.allowlist.JacksonConfig,
        //be sure to keep that in synch if changing anything here.
        //if (ptv == null || ptv instanceof LaissezFaireSubTypeValidator) {
        //   mapper.setPolymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build());
        //}
        return mapper;
    }
}
