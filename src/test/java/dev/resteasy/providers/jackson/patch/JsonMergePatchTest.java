/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import static dev.resteasy.providers.jackson.ResteasyMediaTypes.APPLICATION_MERGE_PATCH_JSON;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests for Jackson-based JSON Merge Patch (RFC 7396) implementation.
 * Verifies merge semantics, null handling, and proper behavior.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(resources = { CustomerResource.class })
class JsonMergePatchTest {

    @RestResource
    @RequestPath("customers/123/merge")
    private WebTarget target;

    @AfterEach
    void reset() {
        CustomerStore.reset();
    }

    // ========== Basic Merge Operations ==========

    @Test
    void simpleMerge() {
        final String mergePatchJson = """
                {
                  "email": "merged@resteasy.dev",
                  "status": "inactive"
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("123", customer.getId());
            Assertions.assertEquals("John Doe", customer.getName()); // Unchanged
            Assertions.assertEquals("merged@resteasy.dev", customer.getEmail()); // Changed
            Assertions.assertEquals("inactive", customer.getStatus()); // Changed
        }
    }

    @Test
    void partialUpdate() {
        final String mergePatchJson = """
                {
                  "email": "newemail@resteasy.dev"
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("123", customer.getId());
            Assertions.assertEquals("John Doe", customer.getName()); // Unchanged
            Assertions.assertEquals("newemail@resteasy.dev", customer.getEmail()); // Changed
            Assertions.assertEquals("active", customer.getStatus()); // Unchanged
            Assertions.assertEquals(1, customer.getTags().size()); // Unchanged
        }
    }

    // ========== Null Handling (Delete Fields) ==========

    @Test
    void mergeWithNull() {
        final String mergePatchJson = """
                {
                  "status": null
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("John Doe", customer.getName()); // Unchanged
            Assertions.assertEquals("john@resteasy.dev", customer.getEmail()); // Unchanged
            Assertions.assertNull(customer.getStatus()); // Removed by null
        }
    }

    @Test
    void mergeMultipleFieldsWithNull() {
        final String mergePatchJson = """
                {
                  "email": "updated@resteasy.dev",
                  "status": null
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("updated@resteasy.dev", customer.getEmail()); // Changed
            Assertions.assertNull(customer.getStatus()); // Removed
        }
    }

    // ========== Array Replacement (Not Merge) ==========

    @Test
    void arrayReplacement() {
        // In merge patch, arrays are replaced entirely, not merged
        final String mergePatchJson = """
                {
                  "tags": ["new-tag1", "new-tag2"]
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals(2, customer.getTags().size());
            Assertions.assertTrue(customer.getTags().contains("new-tag1"));
            Assertions.assertTrue(customer.getTags().contains("new-tag2"));
            Assertions.assertFalse(customer.getTags().contains("premium")); // Old value replaced
        }
    }

    @Test
    void emptyArrayReplacement() {
        final String mergePatchJson = """
                {
                  "tags": []
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals(0, customer.getTags().size());
        }
    }

    // ========== Edge Cases ==========

    @Test
    void emptyMergePatch() {
        final String mergePatchJson = "{}";

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Empty merge patch should leave everything unchanged
            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("123", customer.getId());
            Assertions.assertEquals("John Doe", customer.getName());
            Assertions.assertEquals("john@resteasy.dev", customer.getEmail());
            Assertions.assertEquals("active", customer.getStatus());
        }
    }

    @Test
    void nonObjectPatchReplacesTarget() {
        // Per RFC 7396, if patch is not an object, it replaces the target entirely
        final String mergePatchJson = "\"string value\"";

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            // This would replace the entire customer with a string, which
            // causes a type mismatch error during deserialization
            Assertions.assertTrue(
                    response.getStatus() >= 400 && response.getStatus() < 600,
                    "Non-object patch should result in an error");
        }
    }

    @Test
    void mergeAllFields() {
        final String mergePatchJson = """
                {
                  "name": "Jane Smith",
                  "email": "jane@resteasy.dev",
                  "status": "premium",
                  "tags": ["gold", "verified"]
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("123", customer.getId()); // ID unchanged
            Assertions.assertEquals("Jane Smith", customer.getName());
            Assertions.assertEquals("jane@resteasy.dev", customer.getEmail());
            Assertions.assertEquals("premium", customer.getStatus());
            Assertions.assertEquals(2, customer.getTags().size());
            Assertions.assertTrue(customer.getTags().contains("gold"));
            Assertions.assertTrue(customer.getTags().contains("verified"));
        }
    }

    // ========== Type Safety ==========

    @Test
    void typeMismatchRejected() {
        // Attempting to change a field's type (tags from array to string)
        final String mergePatchJson = """
                {
                  "tags": "not-an-array"
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            // Type mismatches should fail during deserialization
            // This prevents data corruption and maintains type safety
            Assertions.assertTrue(response.getStatus() >= 400 && response.getStatus() < 600,
                    "Type mismatch should be rejected");
        }
    }

    // ========== Resource Not Found ==========

    @Test
    void notFound(@RestResource @RequestPath("customers/999/merge") final WebTarget target) {
        final String mergePatchJson = """
                {
                  "email": "new@resteasy.dev"
                }
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    // ========== Idempotency ==========

    @Test
    void idempotentMerge() {
        // Applying the same merge patch twice should be idempotent
        final String mergePatchJson = """
                {
                  "email": "idempotent@resteasy.dev"
                }
                """;

        // First application
        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("idempotent@resteasy.dev", customer.getEmail());
        }

        // Second application - should produce same result
        try (Response response = target.request()
                .build("PATCH", Entity.entity(mergePatchJson, APPLICATION_MERGE_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("idempotent@resteasy.dev", customer.getEmail());
        }
    }
}
