/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import java.util.function.Function;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import dev.resteasy.providers.jackson._private.JacksonLogger;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON Patch (RFC 6902) implementation using pure Jackson.
 * <p>
 * Provides precise error handling with proper HTTP status codes:
 * </p>
 * <ul>
 * <li>412 (Precondition Failed) - test operation failures</li>
 * <li>400 (Bad Request) - malformed patches or operation failures</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class JacksonJsonPatch implements Function<JsonNode, JsonNode> {

    private final ArrayNode operations;

    /**
     * Constructs a JSON Patch from an array of operations.
     *
     * @param operations the patch operations as a JsonNode array
     */
    JacksonJsonPatch(final ArrayNode operations) {
        this.operations = operations;
    }

    /**
     * Applies this patch to the target document per RFC 6902.
     * <p>
     * Operations are applied sequentially and atomically - if any operation fails, the entire patch is rejected
     * and the target remains unchanged.
     * </p>
     *
     * @param target the target document to patch
     * @return the patched document
     * @throws WebApplicationException with status 412 if a test operation fails, or 400 for other errors
     */
    @Override
    public JsonNode apply(final JsonNode target) {
        // Deep copy to preserve transactional semantics (all-or-nothing on failure)
        JsonNode current = target.deepCopy();

        for (int i = 0; i < operations.size(); i++) {
            final JsonNode operation = operations.get(i);

            if (!operation.isObject()) {
                throw JacksonLogger.LOGGER.operationNotObject(i);
            }

            final JsonNode opNode = operation.get("op");
            if (opNode == null || !opNode.isString()) {
                throw JacksonLogger.LOGGER.missingField("op");
            }

            final String op = opNode.asString();
            current = applyOperation(current, operation, op);
        }

        return current;
    }

    private JsonNode applyOperation(final JsonNode target, final JsonNode operation, final String op) {
        return switch (op) {
            case "add" -> applyAdd(target, operation);
            case "remove" -> applyRemove(target, operation);
            case "replace" -> applyReplace(target, operation);
            case "move" -> applyMove(target, operation);
            case "copy" -> applyCopy(target, operation);
            case "test" -> applyTest(target, operation);
            default -> throw JacksonLogger.LOGGER.unknownPatchOperation(op);
        };
    }

    private JsonNode applyAdd(final JsonNode target, final JsonNode operation) {
        final JsonPointer path = getPointer(operation, "path");
        final JsonNode value = getValue(operation);

        return addValue(target, path, value);
    }

    private JsonNode applyRemove(final JsonNode target, final JsonNode operation) {
        final JsonPointer path = getPointer(operation, "path");

        if (path.toString().isEmpty()) {
            throw JacksonLogger.LOGGER.cannotRemoveRoot();
        }

        return removeValue(target, path);
    }

    private JsonNode applyReplace(final JsonNode target, final JsonNode operation) {
        final JsonPointer path = getPointer(operation, "path");
        final JsonNode value = getValue(operation);

        // Replace is remove + add, but must verify path exists first
        final JsonNode existing = target.at(path);
        if (existing.isMissingNode()) {
            throw JacksonLogger.LOGGER.pathDoesNotExist(path);
        }

        final JsonNode afterRemove = removeValue(target, path);
        return addValue(afterRemove, path, value);
    }

    private JsonNode applyMove(final JsonNode target, final JsonNode operation) {
        final JsonPointer from = getPointer(operation, "from");
        final JsonPointer path = getPointer(operation, "path");

        // Check if 'from' is a proper prefix of 'path'
        if (path.toString().startsWith(from.toString() + "/")) {
            throw JacksonLogger.LOGGER.cannotMoveToChild(from, path);
        }

        // Get the value to move
        final JsonNode value = target.at(from);
        if (value.isMissingNode()) {
            throw JacksonLogger.LOGGER.sourcePathDoesNotExist(from);
        }

        // Remove from source, add to destination
        final JsonNode afterRemove = removeValue(target, from);
        return addValue(afterRemove, path, value);
    }

    private JsonNode applyCopy(final JsonNode target, final JsonNode operation) {
        final JsonPointer from = getPointer(operation, "from");
        final JsonPointer path = getPointer(operation, "path");

        // Get the value to copy
        final JsonNode value = target.at(from);
        if (value.isMissingNode()) {
            throw JacksonLogger.LOGGER.sourcePathDoesNotExist(from);
        }

        return addValue(target, path, value.deepCopy());
    }

    private JsonNode applyTest(final JsonNode target, final JsonNode operation) {
        final JsonPointer path = getPointer(operation, "path");
        final JsonNode expectedValue = getValue(operation);

        final JsonNode actualValue = target.at(path);

        if (!actualValue.equals(expectedValue)) {
            throw JacksonLogger.LOGGER.valueMismatch(Response.Status.PRECONDITION_FAILED, path, expectedValue, actualValue);
        }

        return target; // Test doesn't modify the document
    }

    private JsonNode addValue(final JsonNode target, final JsonPointer path, final JsonNode value) {
        if (path.toString().isEmpty()) {
            // Adding to root replaces the entire document
            return value;
        }

        final JsonPointer parentPath = path.head();
        final String fieldName = getLastSegment(path);

        JsonNode parent = target.at(parentPath);

        if (parent.isMissingNode()) {
            throw JacksonLogger.LOGGER.parentPathDoesNotExist(parentPath);
        }

        if (parent.isObject()) {
            ((ObjectNode) parent).set(fieldName, value);
        } else if (parent.isArray()) {
            final ArrayNode array = (ArrayNode) parent;
            if ("-".equals(fieldName)) {
                array.add(value);
            } else {
                try {
                    final int index = Integer.parseInt(fieldName);
                    if (index < 0 || index > array.size()) {
                        throw JacksonLogger.LOGGER.arrayIndexOutOfBounds(index);
                    }
                    array.insert(index, value);
                } catch (NumberFormatException e) {
                    throw JacksonLogger.LOGGER.invalidArrayIndex(fieldName);
                }
            }
        } else {
            throw JacksonLogger.LOGGER.cannotAddToNode(parentPath);
        }

        return target;
    }

    private JsonNode removeValue(final JsonNode target, final JsonPointer path) {
        final JsonPointer parentPath = path.head();
        final String fieldName = getLastSegment(path);

        final JsonNode parent = target.at(parentPath);

        if (parent.isMissingNode()) {
            throw JacksonLogger.LOGGER.parentPathDoesNotExist(parentPath);
        }

        if (parent.isObject()) {
            final ObjectNode obj = (ObjectNode) parent;
            if (!obj.has(fieldName)) {
                throw JacksonLogger.LOGGER.fieldDoesNotExist(path);
            }
            obj.remove(fieldName);
        } else if (parent.isArray()) {
            final ArrayNode array = (ArrayNode) parent;
            try {
                final int index = Integer.parseInt(fieldName);
                if (index < 0 || index >= array.size()) {
                    throw JacksonLogger.LOGGER.arrayIndexOutOfBounds(index);
                }
                array.remove(index);
            } catch (NumberFormatException e) {
                throw JacksonLogger.LOGGER.invalidArrayIndex(fieldName);
            }
        } else {
            throw JacksonLogger.LOGGER.cannotRemoveFromNode(parentPath);
        }

        return target;
    }

    private JsonPointer getPointer(final JsonNode operation, final String field) {
        final JsonNode pathNode = operation.get(field);
        if (pathNode == null || !pathNode.isString()) {
            throw JacksonLogger.LOGGER.missingField(field);
        }
        try {
            return JsonPointer.compile(pathNode.asString());
        } catch (IllegalArgumentException e) {
            throw JacksonLogger.LOGGER.invalidJsonPointer(field, pathNode.asString());
        }
    }

    private JsonNode getValue(final JsonNode operation) {
        final JsonNode value = operation.get("value");
        if (value == null) {
            throw JacksonLogger.LOGGER.missingField("value");
        }
        return value;
    }

    private String getLastSegment(final JsonPointer pointer) {
        return pointer.last().getMatchingProperty();
    }
}
