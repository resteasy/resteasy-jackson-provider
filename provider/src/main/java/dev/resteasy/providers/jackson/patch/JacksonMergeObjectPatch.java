/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link ObjectPatch} for JSON Merge Patch (RFC 7396).
 * Uses pure Jackson to apply merge patch operations.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class JacksonMergeObjectPatch implements ObjectPatch {

    private final ObjectMapper objectMapper;
    private final JacksonJsonMergePatch mergePatch;

    JacksonMergeObjectPatch(final ObjectMapper objectMapper, final JacksonJsonMergePatch mergePatch) {
        this.objectMapper = objectMapper;
        this.mergePatch = mergePatch;
    }

    @Override
    public <T> T apply(final T target) {
        // Convert target POJO to JsonNode
        final JsonNode targetJson = objectMapper.convertValue(target, JsonNode.class);

        // Apply the merge patch
        final JsonNode result = mergePatch.apply(targetJson);

        // Convert back to POJO
        @SuppressWarnings("unchecked")
        final Class<T> targetClass = (Class<T>) target.getClass();

        return objectMapper.convertValue(result, targetClass);
    }
}
