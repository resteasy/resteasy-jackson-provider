/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.mapper.resources;

import java.time.Instant;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
public class UserRepository extends GenericRepository<User> {
    @Override
    protected Predicate<User> findByIdFilter(final long id) {
        return (user) -> user.getId() == id;
    }

    @Override
    protected void beforeAdd(final long id, final User entry) {
        entry.setId(id);
        entry.setCreated(Instant.now());
    }

    @Override
    protected void beforeUpdate(final User entry) {
        entry.setModified(Instant.now());
    }
}
