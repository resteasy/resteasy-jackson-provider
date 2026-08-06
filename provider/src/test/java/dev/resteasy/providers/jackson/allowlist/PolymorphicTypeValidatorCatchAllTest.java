/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.allowlist;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.providers.jackson.allowlist.resources.PolymorphicResource;
import dev.resteasy.providers.jackson.allowlist.resources.TestPolymorphicType;
import dev.resteasy.providers.jackson.allowlist.resources.air.Aircraft;
import dev.resteasy.providers.jackson.allowlist.resources.land.Automobile;

/**
 * Send a POST request with a polymorphic type enabled by configuration of
 * {@link dev.resteasy.providers.jackson.AllowListPolymorphicTypeValidatorBuilder}. Each test sends a different
 * serializable type to ensure they can be serialized with the validator.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(PolymorphicResource.class)
public class PolymorphicTypeValidatorCatchAllTest {

    @BeforeAll
    public static void configureSystemProperty() {
        // We need to set this as a system property until RESTEASY-3747 is resolved. Then we can implement a
        // dev.resteasy.junit.extension.api.ConfigurationProvider which sets the configuration parameter.
        System.setProperty("dev.resteasy.jackson.deserialization.allowlist.allowIfSubType.prefix", "*");
    }

    @AfterAll
    public static void resetSystemProperty() {
        System.clearProperty("dev.resteasy.jackson.deserialization.allowlist.allowIfSubType.prefix");
    }

    @Test
    public void checkAutomobile(@RestResource @RequestPath("/test") final WebTarget target) {
        check(target, new TestPolymorphicType(new Automobile()));
    }

    @Test
    public void checkAircraft(@RestResource @RequestPath("/test") final WebTarget target) {
        check(target, new TestPolymorphicType(new Aircraft()));
    }

    private void check(final WebTarget target, final TestPolymorphicType testEntity) {
        try (Response response = target.request().post(Entity.json(testEntity))) {
            Assertions.assertNotNull(response);
            Assertions.assertEquals(201, response.getStatus(),
                    () -> "Expected 201 but got %d: %s".formatted(response.getStatus(), response.readEntity(String.class)));
            Assertions.assertEquals(testEntity, response.readEntity(TestPolymorphicType.class));
        }
    }
}
