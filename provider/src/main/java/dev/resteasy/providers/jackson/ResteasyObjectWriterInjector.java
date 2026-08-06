/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import tools.jackson.jakarta.rs.cfg.ObjectWriterModifier;

public class ResteasyObjectWriterInjector {
    private static final Map<ClassLoader, ObjectWriterModifier> tcclMap = new WeakHashMap<ClassLoader, ObjectWriterModifier>();

    //optimization
    private static final AtomicBoolean hasBeenSet = new AtomicBoolean(false);

    private ResteasyObjectWriterInjector() {
    }

    public static void set(ClassLoader cl, ObjectWriterModifier mod) {
        if (cl == null) {
            throw new IllegalArgumentException("Null classloader");
        }
        hasBeenSet.set(true);
        // TODO (jrp) let's use a better locking mechanism
        synchronized (tcclMap) {
            ObjectWriterModifier previous = tcclMap.put(cl, mod);
            if (previous != null && previous != mod) {
                tcclMap.put(cl, mod);
                throw new IllegalArgumentException(
                        "A different ObjectWriterModifier is already set for the specified classloader");
            }
        }
    }

    public static ObjectWriterModifier get(ClassLoader cl) {
        if (hasBeenSet.get()) {
            synchronized (tcclMap) {
                return tcclMap.get(cl);
            }
        } else {
            return null;
        }
    }
}
