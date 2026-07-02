/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.providers.jackson.allowlist.resources;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author bmaxwell
 */
public class TestPolymorphicType implements Serializable {

    private String name;

    // Using JsonTypeInfo.Id.CLASS enables polymorphic type handling.
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public Serializable vehicle;

    public TestPolymorphicType() {
    }

    public TestPolymorphicType(final Serializable vehicle) {
        this.vehicle = vehicle;
    }

    public Serializable getVehicle() {
        return vehicle;
    }

    public void setVehicle(Serializable vehicle) {
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
        if (!(obj instanceof TestPolymorphicType other)) {
            return false;
        }
        return Objects.equals(vehicle, other.vehicle);
    }

    @Override
    public String toString() {
        return String.format("name: %s vehicle: %s", this.name, this.vehicle);
    }
}
