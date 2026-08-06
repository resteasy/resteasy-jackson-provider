/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.providers.jackson.allowlist.resources;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author bmaxwell
 */
public abstract class AbstractVehicle implements Serializable {

    private String type;

    protected AbstractVehicle(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractVehicle other)) {
            return false;
        }
        return Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return String.format("type; %s", this.getType());
    }
}
