/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.allowlist.resources;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A polymorphic type with {@link AbstractVehicle} as the base type rather than {@link java.io.Serializable}. This
 * allows tests to distinguish between Jackson's default
 * {@link tools.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator}
 * (which blocks {@code Serializable} but allows {@code AbstractVehicle}) and the
 * {@link dev.resteasy.providers.jackson.AllowListPolymorphicTypeValidatorBuilder} (which blocks everything when
 * unconfigured).
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class TestPolymorphicVehicle {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public AbstractVehicle vehicle;

    public TestPolymorphicVehicle() {
    }

    public TestPolymorphicVehicle(final AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public AbstractVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(final AbstractVehicle vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicle);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TestPolymorphicVehicle other)) {
            return false;
        }
        return Objects.equals(vehicle, other.vehicle);
    }

    @Override
    public String toString() {
        return String.format("vehicle: %s", this.vehicle);
    }
}
