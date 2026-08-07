/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Tests that {@link ResteasyJacksonProvider} correctly handles {@link JsonView} annotations on Jakarta REST resource
 * methods for both serialization and deserialization.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(JsonViewTest.JsonViewResource.class)
class JsonViewTest {

    @RestResource
    @RequestPath("/view")
    private WebTarget target;

    @Test
    void writeWithPublicViewExcludesInternalFields() {
        try (Response response = target.path("/public").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            final Map<String, Object> result = response.readEntity(new GenericType<>() {
            });
            Assertions.assertNotNull(result.get("name"), "Public field 'name' should be present");
            Assertions.assertNull(result.get("secret"), "Internal field 'secret' should be excluded in public view");
        }
    }

    @Test
    void writeWithInternalViewIncludesAllFields() {
        try (Response response = target.path("/internal").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            final Map<String, Object> result = response.readEntity(new GenericType<>() {
            });
            Assertions.assertNotNull(result.get("name"), "Public field 'name' should be present");
            Assertions.assertNotNull(result.get("secret"), "Internal field 'secret' should be present in internal view");
        }
    }

    @Test
    void writeWithoutViewIncludesAllFields() {
        try (Response response = target.path("/all").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            final Map<String, Object> result = response.readEntity(new GenericType<>() {
            });
            Assertions.assertNotNull(result.get("name"), "Field 'name' should be present without view");
            Assertions.assertNotNull(result.get("secret"), "Field 'secret' should be present without view");
        }
    }

    @Test
    void readWithPublicViewIgnoresInternalFields() {
        final String json = """
                {"name": "Alice", "secret": "s3cret"}
                """;
        try (Response response = target.path("/read-public")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus());
            final ViewableModel result = response.readEntity(ViewableModel.class);
            Assertions.assertEquals("Alice", result.name, "Public field 'name' should be deserialized");
            Assertions.assertNull(result.secret,
                    "Internal field 'secret' should be null when reading with public view");
        }
    }

    @Test
    void readWithoutViewDeserializesAllFields() {
        final String json = """
                {"name": "Alice", "secret": "s3cret"}
                """;
        try (Response response = target.path("/read-all")
                .request()
                .post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE))) {
            Assertions.assertEquals(200, response.getStatus());
            final ViewableModel result = response.readEntity(ViewableModel.class);
            Assertions.assertEquals("Alice", result.name);
            Assertions.assertEquals("s3cret", result.secret, "All fields should be deserialized without a view");
        }
    }

    @Path("/view")
    @Produces(MediaType.APPLICATION_JSON)
    public static class JsonViewResource {

        @GET
        @Path("/public")
        @JsonView(Views.Public.class)
        public ViewableModel getPublic() {
            return createModel();
        }

        @GET
        @Path("/internal")
        @JsonView(Views.Internal.class)
        public ViewableModel getInternal() {
            return createModel();
        }

        @GET
        @Path("/all")
        public ViewableModel getAll() {
            return createModel();
        }

        @POST
        @Path("/read-public")
        @Consumes(MediaType.APPLICATION_JSON)
        @JsonView(Views.Public.class)
        public ViewableModel readPublic(final ViewableModel model) {
            return model;
        }

        @POST
        @Path("/read-all")
        @Consumes(MediaType.APPLICATION_JSON)
        public ViewableModel readAll(final ViewableModel model) {
            return model;
        }

        private static ViewableModel createModel() {
            final ViewableModel model = new ViewableModel();
            model.name = "Alice";
            model.secret = "s3cret";
            return model;
        }
    }

    public static class Views {
        public static class Public {
        }

        public static class Internal extends Public {
        }
    }

    public static class ViewableModel {
        @JsonView(Views.Public.class)
        public String name;

        @JsonView(Views.Internal.class)
        public String secret;
    }
}
