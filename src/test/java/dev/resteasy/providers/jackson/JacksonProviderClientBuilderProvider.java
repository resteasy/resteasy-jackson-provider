/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import jakarta.ws.rs.client.ClientBuilder;

import dev.resteasy.junit.extension.api.RestClientBuilderProvider;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class JacksonProviderClientBuilderProvider implements RestClientBuilderProvider {
    @Override
    public ClientBuilder getClientBuilder() {
        return ClientBuilder.newBuilder()
                .register(JacksonFeature.class);
    }
}
