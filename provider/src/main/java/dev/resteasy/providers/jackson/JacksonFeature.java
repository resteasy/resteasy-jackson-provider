/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

import dev.resteasy.providers.jackson.patch.JsonMergePatchReader;
import dev.resteasy.providers.jackson.patch.JsonPatchReader;

/**
 * A Jakarta REST {@link Feature} that registers the Jackson provider, exception mapper, and JSON Patch readers.
 * Discovered automatically via {@code META-INF/services/jakarta.ws.rs.core.Feature}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class JacksonFeature implements Feature {
    @Override
    public boolean configure(final FeatureContext context) {
        context.register(ResteasyJacksonProvider.class)
                .register(JsonProcessingExceptionMapper.class)
                .register(JsonPatchReader.class)
                .register(JsonMergePatchReader.class);
        return true;
    }
}
