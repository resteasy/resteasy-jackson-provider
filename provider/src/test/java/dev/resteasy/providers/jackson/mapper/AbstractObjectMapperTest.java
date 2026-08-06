/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.mapper;

import java.net.URI;
import java.time.Instant;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.providers.jackson.mapper.resources.Contact;
import dev.resteasy.providers.jackson.mapper.resources.User;

/**
 * An abstract test for using the default {@link tools.jackson.databind.json.JsonMapper} or a custom provider defined
 * as a {@link jakarta.ws.rs.ext.ContextResolver}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("JUnitMalformedDeclaration")
abstract class AbstractObjectMapperTest {

    private final boolean indentedOutput;

    @RestResource
    private Client client;

    AbstractObjectMapperTest(final boolean indentedOutput) {
        this.indentedOutput = indentedOutput;
    }

    @Test
    public void addUser(@RestResource @RequestPath("/user/add") final WebTarget target) {
        final User user = new User();
        user.setName("RESTEasy");
        user.setEmail("resteasy@resteasy.dev");
        try (
                Response response = target.request()
                        .post(Entity.json(user))) {
            Assertions.assertEquals(201, response.getStatus());
            try (Response queryResponse = client.target(response.getLocation()).request().get()) {
                Assertions.assertTrue(queryResponse.bufferEntity(), "Failed to buffer entity");
                final String body = queryResponse.readEntity(String.class);
                Assertions.assertEquals(200, queryResponse.getStatus(),
                        () -> String.format("Expected 200 response from %s, but got %d with body: %s", target.getUri(),
                                response.getStatus(), body));
                if (indentedOutput) {
                    Assertions.assertTrue(body.contains("\n"),
                            () -> "Expected body to be indented and include \\n: " + body);
                } else {
                    Assertions.assertFalse(body.contains("\n"),
                            () -> "Expected body NOT to be indented, however it includes \\n: " + body);
                }
                compareUser(user, queryResponse.readEntity(User.class), true);
            }
        }
    }

    @Test
    public void addContact(@RestResource @RequestPath("/contact/add") final WebTarget target) {
        final Contact contact = new Contact();
        contact.setName("RESTEasy");
        contact.setEmail("resteasy@resteasy.dev");
        try (
                Response response = target.request()
                        .post(Entity.json(contact))) {
            Assertions.assertEquals(201, response.getStatus());
            try (Response queryResponse = client.target(response.getLocation()).request().get()) {
                Assertions.assertTrue(queryResponse.bufferEntity(), "Failed to buffer entity");
                final String body = queryResponse.readEntity(String.class);
                Assertions.assertEquals(200, queryResponse.getStatus(),
                        () -> String.format("Expected 200 response from %s, but got %d with body: %s", target.getUri(),
                                response.getStatus(), body));
                if (indentedOutput) {
                    Assertions.assertTrue(body.contains("\n"),
                            () -> "Expected body to be indented and include \\n: " + body);
                } else {
                    Assertions.assertFalse(body.contains("\n"),
                            () -> "Expected body NOT to be indented, however it includes \\n: " + body);
                }
                compareContact(contact, queryResponse.readEntity(Contact.class), true);
            }
        }
    }

    @Test
    public void modifyUser(@RestResource @RequestPath("/user") final URI uri) {
        final Contact contact = new Contact();
        contact.setName("RESTEasy User");
        contact.setEmail("user@resteasy.dev");
        try (
                Response response = client.target(UriBuilder.fromUri(uri).path("/add"))
                        .request()
                        .post(Entity.json(contact))) {
            Assertions.assertEquals(201, response.getStatus());
            try (Response queryResponse = client.target(response.getLocation()).request().get()) {
                Assertions.assertTrue(queryResponse.bufferEntity(), "Failed to buffer entity");
                final String body = queryResponse.readEntity(String.class);
                if (indentedOutput) {
                    Assertions.assertTrue(body.contains("\n"),
                            () -> "Expected body to be indented and include \\n: " + body);
                } else {
                    Assertions.assertFalse(body.contains("\n"),
                            () -> "Expected body NOT to be indented, however it includes \\n: " + body);
                }
                final User userToUpdate = queryResponse.readEntity(User.class);
                userToUpdate.setPhoneNumber("+1 555.555.5555");
                try (
                        Response updateResponse = client.target(UriBuilder.fromUri(uri).path("/update"))
                                .request()
                                .put(Entity.json(userToUpdate))) {
                    Assertions.assertEquals(204, updateResponse.getStatus());
                    compareUser(client, UriBuilder.fromUri(uri).path(Long.toString(userToUpdate.getId())).build(),
                            userToUpdate, false);
                }
            }
        }
    }

    @Test
    public void modifyContact(@RestResource @RequestPath("/contact") final URI uri) throws Exception {
        final Contact contact = new Contact();
        contact.setName("RESTEasy User");
        contact.setEmail("user@resteasy.dev");
        try (
                Response response = client.target(UriBuilder.fromUri(uri).path("/add"))
                        .request()
                        .post(Entity.json(contact))) {
            Assertions.assertEquals(201, response.getStatus());
            try (Response queryResponse = client.target(response.getLocation()).request().get()) {
                Assertions.assertTrue(queryResponse.bufferEntity(), "Failed to buffer entity");
                final String body = queryResponse.readEntity(String.class);
                if (indentedOutput) {
                    Assertions.assertTrue(body.contains("\n"),
                            () -> "Expected body to be indented and include \\n: " + body);
                } else {
                    Assertions.assertFalse(body.contains("\n"),
                            () -> "Expected body NOT to be indented, however it includes \\n: " + body);
                }
                final Contact contactToUpdate = queryResponse.readEntity(Contact.class);
                contactToUpdate.setPhoneNumber("+1 555.555.5555");
                try (
                        Response updateResponse = client.target(UriBuilder.fromUri(uri).path("/update"))
                                .request()
                                .put(Entity.json(contactToUpdate))) {
                    Assertions.assertEquals(204, updateResponse.getStatus());
                    compareContact(client, UriBuilder.fromUri(uri)
                            .path(Long.toString(contactToUpdate.getId()))
                            .build(),
                            contactToUpdate, false);
                }
            }
        }
    }

    private void compareUser(final User expected, final User found, final boolean added) {
        // Deep compare the user read
        if (!added) {
            Assertions.assertEquals(expected, found);
            Assertions.assertEquals(expected.getCreated(), found.getCreated());
            Assertions.assertTrue(found.getModified().isPresent(),
                    () -> String.format("The modified date was not set in %s", found));
        }
        Assertions.assertEquals(expected.getName(), found.getName());
        Assertions.assertEquals(expected.getEmail(), found.getEmail());
        Assertions.assertEquals(expected.getPhoneNumber().orElse(null), found.getPhoneNumber().orElse(null));
        Assertions.assertNotNull(found.getCreated());
        Assertions.assertTrue(found.getCreated().isBefore(Instant.now()));
    }

    private void compareUser(final Client client, final URI uri, final User expected, final boolean added) {
        try (Response response = client.target(uri).request().get()) {
            Assertions.assertEquals(200, response.getStatus(),
                    () -> String.format("Failed to find user for %s: %s", uri, response.readEntity(String.class)));
            Assertions.assertTrue(response.bufferEntity());
            if (!indentedOutput) {
                final String body = response.readEntity(String.class);
                Assertions.assertFalse(body.contains("\n"),
                        () -> "Expected body NOT to be indented, however it includes \\n: " + body);
            }
            final User found = response.readEntity(User.class);
            // Deep compare the user read
            if (!added) {
                Assertions.assertEquals(expected, found);
                Assertions.assertEquals(expected.getCreated(), found.getCreated());
                Assertions.assertTrue(found.getModified().isPresent(),
                        () -> String.format("The modified date was not set in %s", found));
            }
            Assertions.assertEquals(expected.getName(), found.getName());
            Assertions.assertEquals(expected.getEmail(), found.getEmail());
            Assertions.assertEquals(expected.getPhoneNumber().orElse(null), found.getPhoneNumber().orElse(null));
            Assertions.assertNotNull(found.getCreated());
            Assertions.assertTrue(found.getCreated().isBefore(Instant.now()));
        }
    }

    private void compareContact(final Client client, final URI uri, final Contact expected, final boolean added) {
        try (Response response = client.target(uri).request().get()) {
            Assertions.assertEquals(200, response.getStatus(),
                    () -> String.format("Failed to find user for %s: %s", uri, response.readEntity(String.class)));
            Assertions.assertTrue(response.bufferEntity());
            if (!indentedOutput) {
                final String body = response.readEntity(String.class);
                Assertions.assertFalse(body.contains("\n"),
                        () -> "Expected body NOT to be indented, however it includes \\n: " + body);
            }
            final Contact found = response.readEntity(Contact.class);
            // Deep compare the user read
            if (!added) {
                Assertions.assertEquals(expected, found);
            }
            Assertions.assertEquals(expected.getName(), found.getName());
            Assertions.assertEquals(expected.getEmail(), found.getEmail());
            Assertions.assertEquals(expected.getPhoneNumber().orElse(null), found.getPhoneNumber().orElse(null));
        }
    }

    private void compareContact(final Contact expected, final Contact found, final boolean added) {
        // Deep compare the user read
        if (!added) {
            Assertions.assertEquals(expected, found);
        }
        Assertions.assertEquals(expected.getName(), found.getName());
        Assertions.assertEquals(expected.getEmail(), found.getEmail());
        Assertions.assertEquals(expected.getPhoneNumber().orElse(null), found.getPhoneNumber().orElse(null));
    }
}
