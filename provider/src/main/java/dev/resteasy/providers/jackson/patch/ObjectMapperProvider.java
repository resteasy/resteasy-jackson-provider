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
import tools.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
abstract class ObjectMapperProvider {

    @Context
    private Providers providers;

    private volatile JsonMapper jsonMapper;

    JsonMapper locateMapper(final Class<?> type, final MediaType mediaType) {
        JsonMapper currentObjectMapper = jsonMapper;
        if (currentObjectMapper == null) {
            synchronized (this) {
                currentObjectMapper = jsonMapper;
                if (currentObjectMapper == null) {
                    final JsonMapper contextMapper = resolveContextJsonMapper(type, mediaType);
                    if (contextMapper != null) {
                        if (contextMapper.deserializationConfig()
                                .getPolymorphicTypeValidator() instanceof DefaultBaseTypeLimitingValidator) {
                            currentObjectMapper = contextMapper
                                    .rebuild()
                                    .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                                    .build();
                        } else {
                            currentObjectMapper = contextMapper;
                        }
                    } else {
                        currentObjectMapper = createDefaultObjectMapper();
                    }

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
        return JsonMapper.builder()
                .polymorphicTypeValidator(new AllowListPolymorphicTypeValidatorBuilder().build())
                .build();
    }
}
