/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.providers.jackson.allowlist.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author bmaxwell
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Path("/test")
@RequestScoped
public class PolymorphicResource {
    @Inject
    private ObjectStore objectStore;

    @Inject
    private UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(final TestPolymorphicType test) {
        objectStore.put(test.toString(), test);
        return Response.created(uriInfo.getRequestUriBuilder().path(test.toString()).build()).entity(test).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public TestPolymorphicType get(@PathParam("id") final String id) {
        return objectStore.get(id, TestPolymorphicType.class);
    }
}
