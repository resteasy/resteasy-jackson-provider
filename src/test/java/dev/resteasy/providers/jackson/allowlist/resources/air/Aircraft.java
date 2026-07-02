/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.providers.jackson.allowlist.resources.air;

import java.util.Objects;

import dev.resteasy.providers.jackson.allowlist.resources.AbstractVehicle;

/**
 * @author bmaxwell
 */
public class Aircraft extends AbstractVehicle {

    private int landSpeed;
    private int airSpeed;

    public Aircraft() {
        super("Aircraft");
    }

    public int getLandSpeed() {
        return landSpeed;
    }

    public void setLandSpeed(int landSpeed) {
        this.landSpeed = landSpeed;
    }

    public int getAirSpeed() {
        return airSpeed;
    }

    public void setAirSpeed(int airSpeed) {
        this.airSpeed = airSpeed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), landSpeed, airSpeed);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Aircraft other)) {
            return false;
        }
        return super.equals(other) && Objects.equals(landSpeed, other.landSpeed) && Objects.equals(airSpeed, other.airSpeed);
    }

    @Override
    public String toString() {
        return String.format("%s landSpeed: %d airSpeed: %d", super.toString(), landSpeed, airSpeed);
    }
}
