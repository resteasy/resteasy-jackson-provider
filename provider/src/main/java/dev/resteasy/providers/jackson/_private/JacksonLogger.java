/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson._private;

import java.lang.invoke.MethodHandles;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.Signature;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "RESTEASY-JACKSON")
public interface JacksonLogger extends BasicLogger {

    JacksonLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), JacksonLogger.class,
            "dev.resteasy.providers.jackson");

    /**
     * Returns a message indicating the data could not be deserialized.
     *
     * @return a message indicating the data could not be deserialized
     */
    // Note we must use Message.NONE here. Otherwise, a prefix is added, and we do not want to expose where this is
    // coming from in the response.
    @Message(id = Message.NONE, value = "Not able to deserialize data provided.")
    String cannotDeserialize();

    /**
     * Logs a message indicating the data could not be deserialized.
     *
     * @param cause the cause of the error
     */
    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 100, value = "Not able to deserialize data provided")
    void logCannotDeserialize(@Cause Throwable cause);

    @Message(id = 200, value = "Value at '%s' does not match. Expected: %s, Actual: %s")
    @Signature({ String.class, Response.Status.class })
    WebApplicationException valueMismatch(@Param Response.Status status, JsonPointer path, JsonNode expectedValue,
            JsonNode actualValue);

    @Message(id = 201, value = "Unknown patch operation %s")
    BadRequestException unknownPatchOperation(String op);

    @Message(id = 202, value = "Patch operations is missing field '%s'")
    BadRequestException missingField(String field);

    @Message(id = 203, value = "Operation at index %d is not an object")
    BadRequestException operationNotObject(int index);

    @Message(id = 204, value = "Path does not exist: %s")
    BadRequestException pathDoesNotExist(JsonPointer path);

    @Message(id = 205, value = "Source path does not exist: %s")
    BadRequestException sourcePathDoesNotExist(JsonPointer from);

    @Message(id = 206, value = "Parent path does not exist: %s")
    BadRequestException parentPathDoesNotExist(JsonPointer parentPath);

    @Message(id = 207, value = "Field does not exist: %s")
    BadRequestException fieldDoesNotExist(JsonPointer path);

    @Message(id = 208, value = "Cannot move '%s' to a child of itself: %s")
    BadRequestException cannotMoveToChild(JsonPointer from, JsonPointer path);

    @Message(id = 209, value = "Array index out of bounds: %d")
    BadRequestException arrayIndexOutOfBounds(int index);

    @Message(id = 210, value = "Invalid array index: %s")
    BadRequestException invalidArrayIndex(String index);

    @Message(id = 211, value = "Cannot add to a non-object, non-array node at: %s")
    BadRequestException cannotAddToNode(JsonPointer parentPath);

    @Message(id = 212, value = "Cannot remove from a non-object, non-array node at: %s")
    BadRequestException cannotRemoveFromNode(JsonPointer parentPath);

    @Message(id = 213, value = "Cannot remove root document")
    BadRequestException cannotRemoveRoot();

    @Message(id = 214, value = "Invalid JSON Pointer in '%s': %s")
    BadRequestException invalidJsonPointer(String field, String value);
}
