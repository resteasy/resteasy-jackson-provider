/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.api;

import java.util.Set;

import tools.jackson.jakarta.rs.cfg.JakartaRSFeature;

/**
 * Configuration for the {@link dev.resteasy.providers.jackson.ResteasyJacksonProvider} specifying which
 * {@link JakartaRSFeature}s to enable or disable. Register an implementation of
 * {@link jakarta.ws.rs.ext.ContextResolver ContextResolver&lt;JacksonProviderConfig&gt;} to supply configuration
 * to the provider.
 *
 * <p>
 * Example:
 * </p>
 *
 * <pre>
 * &#64;Provider
 * public class MyJacksonConfig implements ContextResolver&lt;JacksonProviderConfig&gt; {
 *     &#64;Override
 *     public JacksonProviderConfig getContext(Class&lt;?&gt; type) {
 *         return new JacksonProviderConfig(
 *                 Set.of(JakartaRSFeature.ADD_NO_SNIFF_HEADER),
 *                 Set.of(JakartaRSFeature.ALLOW_EMPTY_INPUT));
 *     }
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public record JacksonProviderConfig(Set<JakartaRSFeature> enabledFeatures, Set<JakartaRSFeature> disabledFeatures) {

    /**
     * Creates a new configuration with defensive copies of the provided feature sets.
     *
     * @param enabledFeatures  the features to enable on the provider
     * @param disabledFeatures the features to disable on the provider
     */
    public JacksonProviderConfig(final Set<JakartaRSFeature> enabledFeatures, final Set<JakartaRSFeature> disabledFeatures) {
        this.enabledFeatures = Set.copyOf(enabledFeatures);
        this.disabledFeatures = Set.copyOf(disabledFeatures);
    }
}
