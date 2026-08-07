/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests for {@link ResteasyJacksonProvider#writeTo} covering generic type serialization, polymorphic types, and
 * response encoding.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(WriteToTest.WriteResource.class)
class WriteToTest {

    @RestResource
    @RequestPath("/write")
    private WebTarget target;

    @Test
    void serializesGenericList() {
        try (Response response = target.path("/list").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            final List<Item> items = response.readEntity(new GenericType<>() {
            });
            Assertions.assertEquals(2, items.size());
            Assertions.assertEquals("first", items.get(0).name);
            Assertions.assertEquals("second", items.get(1).name);
        }
    }

    @Test
    void serializesGenericMap() {
        try (Response response = target.path("/map").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            final Map<String, Item> items = response.readEntity(new GenericType<>() {
            });
            Assertions.assertEquals(2, items.size());
            Assertions.assertNotNull(items.get("a"));
            Assertions.assertEquals("alpha", items.get("a").name);
        }
    }

    @Test
    void serializesNestedGenericType() {
        try (Response response = target.path("/nested").request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            final Wrapper<Item> wrapper = response.readEntity(new GenericType<>() {
            });
            Assertions.assertNotNull(wrapper);
            Assertions.assertEquals("wrapped", wrapper.data.name);
            Assertions.assertEquals(1, wrapper.count);
        }
    }

    @Test
    void nullValueReturnsNoContent() {
        try (Response response = target.path("/null").request().get()) {
            Assertions.assertEquals(204, response.getStatus());
        }
    }

    @Test
    void serializesWithApplicationJsonPlusMediaType() {
        try (Response response = target.path("/list")
                .request()
                .accept("application/vnd.test+json")
                .get()) {
            Assertions.assertEquals(200, response.getStatus());
            final List<Item> items = response.readEntity(new GenericType<>() {
            });
            Assertions.assertEquals(2, items.size());
        }
    }

    @Path("/write")
    @Produces(MediaType.APPLICATION_JSON)
    public static class WriteResource {

        @GET
        @Path("/list")
        @Produces({ MediaType.APPLICATION_JSON, "application/vnd.test+json" })
        public List<Item> getList() {
            return List.of(new Item("first"), new Item("second"));
        }

        @GET
        @Path("/map")
        public Map<String, Item> getMap() {
            return Map.of("a", new Item("alpha"), "b", new Item("beta"));
        }

        @GET
        @Path("/nested")
        public Wrapper<Item> getNested() {
            return new Wrapper<>(new Item("wrapped"), 1);
        }

        @GET
        @Path("/null")
        public Item getNull() {
            return null;
        }
    }

    public static class Item {
        public String name;

        public Item() {
        }

        public Item(final String name) {
            this.name = name;
        }
    }

    public static class Wrapper<T> {
        public T data;
        public int count;

        public Wrapper() {
        }

        public Wrapper(final T data, final int count) {
            this.data = data;
            this.count = count;
        }
    }
}
