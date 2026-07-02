/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

/**
 * Format-agnostic interface for applying patches to objects. Implementations handle specific patch formats like
 * JSON Patch (RFC 6902) and JSON Merge Patch (RFC 7386).
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public interface ObjectPatch {

    /**
     * Apply this patch to the given target object.
     *
     * @param <T>    the type of the target object
     * @param target the object to patch
     * @return the patched object
     */
    <T> T apply(final T target);
}
