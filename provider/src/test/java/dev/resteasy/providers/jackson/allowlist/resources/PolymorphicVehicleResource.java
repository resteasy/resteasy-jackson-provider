/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.allowlist.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Path("/vehicle")
@RequestScoped
public class PolymorphicVehicleResource {

    @Inject
    private UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(final TestPolymorphicVehicle vehicle) {
        return Response.created(uriInfo.getRequestUri()).entity(vehicle).build();
    }
}
