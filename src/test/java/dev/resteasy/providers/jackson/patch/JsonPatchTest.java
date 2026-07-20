/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests for Jackson-based JSON Patch (RFC 6902) implementation.
 * Verifies all operations, error handling, and proper HTTP status codes.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(CustomerResource.class)
class JsonPatchTest {

    @RestResource
    @RequestPath("customers/123")
    private WebTarget target;

    @AfterEach
    void reset() {
        CustomerStore.reset();
    }

    // ========== Basic Operations ==========

    @Test
    void addOperation() {
        final String patchJson = """
                [
                  {"op": "add", "path": "/tags/-", "value": "vip"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals(2, customer.getTags().size());
            Assertions.assertTrue(customer.getTags().contains("premium"));
            Assertions.assertTrue(customer.getTags().contains("vip"));
        }
    }

    @Test
    void removeOperation() {
        final String patchJson = """
                [
                  {"op": "remove", "path": "/tags/0"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals(0, customer.getTags().size());
        }
    }

    @Test
    void replaceOperation() {
        final String patchJson = """
                [
                  {"op": "replace", "path": "/email", "value": "newemail@resteasy.dev"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("123", customer.getId());
            Assertions.assertEquals("John Doe", customer.getName());
            Assertions.assertEquals("newemail@resteasy.dev", customer.getEmail());
            Assertions.assertEquals("active", customer.getStatus());
        }
    }

    @Test
    void moveOperation() {
        // Move status value to name field (realistic: both fields exist)
        final String patchJson = """
                [
                  {"op": "move", "from": "/status", "path": "/name"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("active", customer.getName()); // Moved to here (was "John Doe")
            Assertions.assertNull(customer.getStatus()); // Removed from here (was "active")
        }
    }

    @Test
    void copyOperation() {
        // Copy email value to name field (realistic: both fields exist)
        final String patchJson = """
                [
                  {"op": "copy", "from": "/email", "path": "/name"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("john@resteasy.dev", customer.getEmail()); // Original still here
            Assertions.assertEquals("john@resteasy.dev", customer.getName()); // Copied to here (was "John Doe")
        }
    }

    @Test
    void testOperationSuccess() {
        // Test operation should succeed
        final String patchJson = """
                [
                  {"op": "test", "path": "/email", "value": "john@resteasy.dev"},
                  {"op": "replace", "path": "/status", "value": "inactive"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("inactive", customer.getStatus());
        }
    }

    // ========== Test Operation Failures (412) ==========

    @Test
    void testOperationFailureReturns412() {
        // Test operation should fail - wrong value
        final String patchJson = """
                [
                  {"op": "test", "path": "/email", "value": "wrong@resteasy.dev"},
                  {"op": "replace", "path": "/status", "value": "inactive"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            // CRITICAL: Test failures must return 412, not 500 or 400
            Assertions.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testOperationTypeMismatchReturns412() {
        // Test operation fails due to type mismatch (string vs number)
        final String patchJson = """
                [
                  {"op": "test", "path": "/name", "value": 123}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        }
    }

    // ========== Multiple Operations ==========

    @Test
    void multipleOperations() {
        final String patchJson = """
                [
                  {"op": "replace", "path": "/email", "value": "updated@resteasy.dev"},
                  {"op": "replace", "path": "/status", "value": "inactive"},
                  {"op": "add", "path": "/tags/-", "value": "updated"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("updated@resteasy.dev", customer.getEmail());
            Assertions.assertEquals("inactive", customer.getStatus());
            Assertions.assertEquals(2, customer.getTags().size());
            Assertions.assertTrue(customer.getTags().contains("updated"));
        }
    }

    // ========== Error Cases (400) ==========

    @Test
    void invalidOperationReturns400() {
        final String patchJson = """
                [
                  {"op": "invalid", "path": "/email"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void missingOpFieldReturns400() {
        final String patchJson = """
                [
                  {"path": "/email", "value": "test@resteasy.dev"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void missingPathFieldReturns400() {
        final String patchJson = """
                [
                  {"op": "replace", "value": "test@resteasy.dev"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void missingValueFieldReturns400() {
        final String patchJson = """
                [
                  {"op": "add", "path": "/email"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void invalidJsonPointerReturns400() {
        final String patchJson = """
                [
                  {"op": "replace", "path": "invalid-pointer", "value": "test"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void nonExistentPathReturns400() {
        final String patchJson = """
                [
                  {"op": "remove", "path": "/nonexistent"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void arrayIndexOutOfBoundsReturns400() {
        final String patchJson = """
                [
                  {"op": "remove", "path": "/tags/99"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    // ========== Array Operations ==========

    @Test
    void addToArrayBeginning() {
        final String patchJson = """
                [
                  {"op": "add", "path": "/tags/0", "value": "first"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals(2, customer.getTags().size());
            Assertions.assertEquals("first", customer.getTags().get(0));
            Assertions.assertEquals("premium", customer.getTags().get(1));
        }
    }

    @Test
    void addToArrayEnd() {
        final String patchJson = """
                [
                  {"op": "add", "path": "/tags/-", "value": "last"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals(2, customer.getTags().size());
            Assertions.assertEquals("premium", customer.getTags().get(0));
            Assertions.assertEquals("last", customer.getTags().get(1));
        }
    }

    // ========== Edge Cases ==========

    @Test
    void emptyPatchArray() {
        final String patchJson = "[]";

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Empty patch should leave everything unchanged
            final Customer customer = response.readEntity(Customer.class);
            Assertions.assertEquals("123", customer.getId());
            Assertions.assertEquals("John Doe", customer.getName());
            Assertions.assertEquals("john@resteasy.dev", customer.getEmail());
        }
    }

    @Test
    void moveToChildOfItselfFails() {
        final String patchJson = """
                [
                  {"op": "move", "from": "/address", "path": "/address/city"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    // ========== Atomicity ==========

    @Test
    void atomicityOnFailure() {
        // Patch with 3 operations where the 2nd fails
        // Verify document is unchanged (not partially applied)
        final String patchJson = """
                [
                  {"op": "replace", "path": "/status", "value": "pending"},
                  {"op": "test", "path": "/email", "value": "wrong@resteasy.dev"},
                  {"op": "replace", "path": "/name", "value": "New Name"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
        }

        // Verify original data is unchanged
        final Customer customer = target.request(MediaType.APPLICATION_JSON_TYPE).get(Customer.class);
        Assertions.assertEquals("active", customer.getStatus()); // Not "pending"
        Assertions.assertEquals("John Doe", customer.getName()); // Not "New Name"
    }

    // ========== Resource Not Found ==========

    @Test
    void notFound(@RestResource @RequestPath("customers/999") final WebTarget target) {
        final String patchJson = """
                [
                  {"op": "replace", "path": "/email", "value": "new@resteasy.dev"}
                ]
                """;

        try (Response response = target.request()
                .build("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_JSON_PATCH_JSON))
                .invoke()) {

            Assertions.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }
}
