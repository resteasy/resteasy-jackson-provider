/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import dev.resteasy.providers.jackson.ResteasyMediaTypes;

/**
 * Test REST resource for patch operations.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @GET
    @Path("/{id}")
    public Customer getCustomer(@PathParam("id") final String id) {
        return CustomerStore.get(id).orElseThrow(() -> new NotFoundException("Customer not found: %s".formatted(id)));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Customer updateCustomer(@PathParam("id") final String id, final Customer customer) {
        customer.setId(id);
        CustomerStore.put(customer);
        return customer;
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    public Customer patchCustomer(@PathParam("id") final String id, final ObjectPatch patch) {
        Customer customer = CustomerStore.get(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: %s".formatted(id)));
        customer = patch.apply(customer);
        CustomerStore.put(customer);
        return customer;
    }

    @PATCH
    @Path("/{id}/merge")
    @Consumes(ResteasyMediaTypes.APPLICATION_MERGE_PATCH_JSON)
    public Customer mergePatchCustomer(@PathParam("id") final String id, final ObjectPatch patch) {
        Customer customer = CustomerStore.get(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: %s".formatted(id)));
        customer = patch.apply(customer);
        CustomerStore.put(customer);
        return customer;
    }
}
