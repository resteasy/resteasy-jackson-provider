/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import jakarta.ws.rs.core.MediaType;

/**
 * Media type constants for JSON Patch operations.
 * <p>
 * Note: Jakarta REST 3.1 provides {@link MediaType#APPLICATION_JSON_PATCH_JSON_TYPE} for
 * RFC 6902 (JSON Patch), but does not yet provide constants for RFC 7396 (JSON Merge Patch).
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
// TODO (jrp) maybe use a better name here or potentially just delete it
public final class ResteasyMediaTypes {

    /**
     * A {@link String} representation of {@code application/merge-patch+json} media type (RFC 7396).
     */
    public static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

    /**
     * A {@link MediaType} constant representing {@code application/merge-patch+json} media type (RFC 7396).
     */
    public static final MediaType APPLICATION_MERGE_PATCH_JSON_TYPE = new MediaType("application", "merge-patch+json");

    private ResteasyMediaTypes() {
        // Utility class
    }
}
