/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class CustomerStore {

    private static final Map<String, Customer> CUSTOMERS = new ConcurrentHashMap<>();

    static {
        reset();
    }

    static void put(final Customer customer) {
        CUSTOMERS.put(customer.getId(), customer);
    }

    static Optional<Customer> get(final String id) {
        return Optional.ofNullable(CUSTOMERS.get(id));
    }

    static void reset() {
        CUSTOMERS.clear();
        final Customer customer = new Customer("123", "John Doe", "john@resteasy.dev", "active");
        customer.getTags().add("premium");
        put(customer);
    }
}
