/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import dev.resteasy.providers.jackson.ObjectPatch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link ObjectPatch} for JSON Patch (RFC 6902).
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class JacksonObjectPatch implements ObjectPatch {

    private final ObjectMapper objectMapper;
    private final JacksonJsonPatch patch;

    JacksonObjectPatch(final ObjectMapper objectMapper, final JacksonJsonPatch patch) {
        this.objectMapper = objectMapper;
        this.patch = patch;
    }

    @Override
    public <T> T apply(final T target) {
        final JsonNode targetJson = objectMapper.convertValue(target, JsonNode.class);

        @SuppressWarnings("unchecked")
        final Class<T> targetClass = (Class<T>) target.getClass();
        return objectMapper.convertValue(patch.apply(targetJson), targetClass);
    }
}
