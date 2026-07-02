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
@Path("/user")
@RequestScoped
public class UserResource extends CrudResource<User> {

    @Inject
    private UserRepository userRepository;

    @Override
    GenericRepository<User> repository() {
        return userRepository;
    }
}
