/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.mapper.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/contact")
@RequestScoped
public class ContactResource extends CrudResource<Contact> {

    @Inject
    private ContactRepository contactRepository;

    @Override
    GenericRepository<Contact> repository() {
        return contactRepository;
    }
}
