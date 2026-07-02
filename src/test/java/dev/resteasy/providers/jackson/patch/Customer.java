/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.patch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Test model for patch operations.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class Customer {

    @JsonProperty
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    private String email;

    @JsonProperty
    private String status;

    @JsonProperty
    private List<String> tags = new ArrayList<>();

    public Customer() {
    }

    public Customer(final String id, final String name, final String email, final String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer other)) {
            return false;
        }
        return Objects.equals(id, other.id) &&
                Objects.equals(name, other.name) &&
                Objects.equals(email, other.email) &&
                Objects.equals(status, other.status) &&
                Objects.equals(tags, other.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, status, tags);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", tags=" + tags +
                '}';
    }
}
