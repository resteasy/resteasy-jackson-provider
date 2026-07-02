/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON Merge Patch (RFC 7396) implementation using pure Jackson. Provides a simpler patching mechanism than JSON Patch
 * - just send the fields to merge.
 *
 * <p>
 * Merge semantics per RFC 7396:
 * </p>
 * <ul>
 * <li>If patch is not an object, it replaces the target entirely</li>
 * <li>If patch is an object, for each field:
 * <ul>
 * <li>If value is null, remove the field from target</li>
 * <li>If value is an object and target field is an object, recursively merge</li>
 * <li>Otherwise, replace or add the field value</li>
 * </ul>
 * </li>
 * <li>Arrays are always replaced entirely, never merged element-by-element</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class JacksonJsonMergePatch {

    private final JsonNode patch;

    /**
     * Constructs a JSON Merge Patch.
     *
     * @param patch the merge patch document
     */
    JacksonJsonMergePatch(final JsonNode patch) {
        this.patch = patch;
    }

    /**
     * Applies this merge patch to the target document.
     * <p>
     * Per RFC 7396, merge patch is simpler than JSON Patch:
     * </p>
     * <ul>
     * <li>Objects are merged recursively</li>
     * <li>null values delete fields</li>
     * <li>Arrays are replaced entirely</li>
     * <li>All other values replace</li>
     * </ul>
     *
     * @param target the target document to patch
     * @return the merged document
     */
    public JsonNode apply(final JsonNode target) {
        return merge(target, patch);
    }

    /**
     * Recursively merges patch into target per RFC 7396 semantics.
     * <p>
     * RFC 7396 algorithm:
     * <ul>
     * <li>If patch is not an object: return patch (replaces target entirely)</li>
     * <li>If patch is an object but target is not: RFC says to replace target with empty object {}, then merge all
     * patch fields into it. Since merging all patch fields into {} produces patch, this is optimized to return
     * patch directly.</li>
     * <li>If both are objects: merge each patch field into target</li>
     * </ul>
     * </p>
     *
     * @param target the target node
     * @param patch  the patch node
     * @return the merged result
     */
    private JsonNode merge(final JsonNode target, final JsonNode patch) {
        // RFC 7396: If patch is not an object, it replaces the target entirely
        if (!patch.isObject()) {
            return patch;
        }

        // RFC 7396: If target is not an object but patch is, RFC algorithm says to replace target with {},
        // then merge all patch fields into it. This is optimized: merging all fields into {} produces patch.
        if (!target.isObject()) {
            return patch;
        }

        // Both are objects - merge them field by field.
        // Deep copy target to preserve transactional semantics (all-or-nothing on failure).
        final ObjectNode result = target.deepCopy().asObject();
        for (final String fieldName : patch.propertyNames()) {
            final JsonNode patchValue = patch.get(fieldName);

            if (patchValue.isNull()) {
                // null in patch means remove the field
                result.remove(fieldName);
            } else {
                final JsonNode targetValue = result.get(fieldName);

                if (patchValue.isObject() && targetValue != null && targetValue.isObject()) {
                    // Both are objects - recursively merge
                    result.set(fieldName, merge(targetValue, patchValue));
                } else {
                    // Otherwise replace/add the value
                    // Note: Arrays are replaced entirely, not merged
                    result.set(fieldName, patchValue);
                }
            }
        }

        return result;
    }
}
