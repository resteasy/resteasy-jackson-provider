/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * MessageBodyReader for JSON Patch (RFC 6902).
 * Handles {@code application/json-patch+json} media type.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
public class JsonPatchReader extends ObjectMapperProvider implements MessageBodyReader<ObjectPatch> {

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        return ObjectPatch.class == type;
    }

    @Override
    public ObjectPatch readFrom(final Class<ObjectPatch> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
            throws WebApplicationException {

        final ObjectMapper mapper = locateMapper(type, mediaType);
        final JsonNode node = mapper.readTree(entityStream);
        if (node.isArray()) {
            return new JacksonObjectPatch(mapper, new JacksonJsonPatch((ArrayNode) node));
        }
        throw new WebApplicationException("JSON patch data is not a valid array");
    }
}
