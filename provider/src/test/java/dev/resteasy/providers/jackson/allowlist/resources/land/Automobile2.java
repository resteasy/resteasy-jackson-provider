/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.providers.jackson.allowlist.resources.land;

import java.util.Objects;

import dev.resteasy.providers.jackson.allowlist.resources.AbstractVehicle;

/**
 * @author bmaxwell
 */
public class Automobile2 extends AbstractVehicle {

    private int speed;

    public Automobile2() {
        super("Automobile2");
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), speed);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Automobile2 other)) {
            return false;
        }
        return super.equals(other) && Objects.equals(speed, other.speed);
    }

    @Override
    public String toString() {
        return String.format("%s speed: %d", super.toString(), speed);
    }
}
