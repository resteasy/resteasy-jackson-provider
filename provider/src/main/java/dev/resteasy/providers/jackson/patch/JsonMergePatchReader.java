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

import dev.resteasy.providers.jackson.ResteasyMediaTypes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * MessageBodyReader for JSON Merge Patch (RFC 7396).
 * Handles {@code application/merge-patch+json} media type.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Consumes(ResteasyMediaTypes.APPLICATION_MERGE_PATCH_JSON)
public class JsonMergePatchReader extends ObjectMapperProvider implements MessageBodyReader<ObjectPatch> {

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

        // Read the merge patch document using Jackson
        final JsonNode patchNode = mapper.readTree(entityStream);

        // Create Jackson-based merge patch
        return new JacksonMergeObjectPatch(mapper, new JacksonJsonMergePatch(patchNode));
    }
}
