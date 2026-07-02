/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.mapper.resources;

import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
public class ContactRepository extends GenericRepository<Contact> {
    @Override
    protected Predicate<Contact> findByIdFilter(final long id) {
        return (contact) -> contact.getId() == id;
    }

    @Override
    protected void beforeAdd(final long id, final Contact entry) {
        entry.setId(id);
    }

    @Override
    protected void beforeUpdate(final Contact entry) {
        // Do nothing
    }
}
