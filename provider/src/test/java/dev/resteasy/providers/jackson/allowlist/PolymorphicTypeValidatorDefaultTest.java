/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.allowlist;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.providers.jackson.allowlist.resources.PolymorphicVehicleResource;
import dev.resteasy.providers.jackson.allowlist.resources.TestPolymorphicVehicle;
import dev.resteasy.providers.jackson.allowlist.resources.land.Automobile;

/**
 * Verifies that the provider applies the {@link dev.resteasy.providers.jackson.AllowListPolymorphicTypeValidatorBuilder}
 * as the default when no custom {@link tools.jackson.databind.jsontype.PolymorphicTypeValidator} is configured.
 *
 * <p>
 * This test uses {@link dev.resteasy.providers.jackson.allowlist.resources.AbstractVehicle} as the polymorphic base
 * type. Jackson's default {@link tools.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator} would allow this
 * (it only blocks unsafe base types like {@code Object} and {@code Serializable}), but the
 * {@link dev.resteasy.providers.jackson.AllowListPolymorphicTypeValidatorBuilder} with no configured allowlist rejects
 * all subtypes. A 400 response confirms the provider applied the stricter validator.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(PolymorphicVehicleResource.class)
public class PolymorphicTypeValidatorDefaultTest {

    @Test
    public void defaultValidatorShouldRejectPolymorphicType(
            @RestResource @RequestPath("/vehicle") final WebTarget target) {
        final TestPolymorphicVehicle testEntity = new TestPolymorphicVehicle(new Automobile());
        try (Response response = target.request().post(Entity.json(testEntity))) {
            Assertions.assertEquals(400, response.getStatus(),
                    "Polymorphic type should be rejected when no allowlist is configured, proving the provider "
                            + "applies AllowListPolymorphicTypeValidatorBuilder rather than Jackson's default validator");
        }
    }
}
