/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.allowlist.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
// TODO (jrp) this is likely overkill
@ApplicationScoped
public class ObjectStore {

    private final Map<String, Object> map = new ConcurrentHashMap<>();

    public <T> void put(final String key, final T value) {
        map.put(key, value);
    }

    public <T> T get(final String key, final Class<T> type) {
        return type.cast(map.get(key));
    }

    public <T> T remove(final String key, final Class<T> type) {
        return type.cast(map.remove(key));
    }
}
