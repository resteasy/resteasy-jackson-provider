/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import dev.resteasy.providers.jackson._private.JacksonLogger;

import tools.jackson.core.JacksonException;

/**
 * Maps {@link JacksonException} to a {@code 400 Bad Request} response. The exception details are logged but not
 * exposed in the response body.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class JsonProcessingExceptionMapper implements ExceptionMapper<JacksonException> {
    @Override
    public Response toResponse(final JacksonException exception) {
        JacksonLogger.LOGGER.logCannotDeserialize(exception);
        return Response.status(Response.Status.BAD_REQUEST).entity(JacksonLogger.LOGGER.cannotDeserialize()).build();
    }
}
