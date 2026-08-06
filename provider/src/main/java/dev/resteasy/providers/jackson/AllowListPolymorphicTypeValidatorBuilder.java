/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.StringTokenizer;

import org.jboss.resteasy.spi.config.Configuration;
import org.jboss.resteasy.spi.config.ConfigurationFactory;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * A {@link BasicPolymorphicTypeValidator.Builder} that populates allowed base types and subtypes from configuration
 * properties. Applied as the default validator when no custom {@link tools.jackson.databind.jsontype.PolymorphicTypeValidator
 * PolymorphicTypeValidator} is configured.
 * <p>
 * Supported properties (comma-separated, {@code *} for wildcard):
 * </p>
 * <ul>
 * <li>{@code dev.resteasy.jackson.deserialization.allowlist.allowIfBaseType}</li>
 * <li>{@code dev.resteasy.jackson.deserialization.allowlist.allowIfSubType}</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class AllowListPolymorphicTypeValidatorBuilder extends BasicPolymorphicTypeValidator.Builder {

    private static final String BASE_TYPE_PROP = "dev.resteasy.jackson.deserialization.allowlist.allowIfBaseType";
    private static final String SUB_TYPE_PROP = "dev.resteasy.jackson.deserialization.allowlist.allowIfSubType";

    public AllowListPolymorphicTypeValidatorBuilder() {
        super();
        final String allowIfBaseType = getProperty(BASE_TYPE_PROP);
        if (allowIfBaseType != null) {
            StringTokenizer st = new StringTokenizer(allowIfBaseType, ",", false);
            while (st.hasMoreTokens()) {
                String t = st.nextToken();
                allowIfBaseType("*".equals(t) ? "" : t);
            }
        }
        final String allowIfSubType = getProperty(SUB_TYPE_PROP);
        if (allowIfSubType != null) {
            StringTokenizer st = new StringTokenizer(allowIfSubType, ",", false);
            while (st.hasMoreTokens()) {
                String t = st.nextToken();
                allowIfSubType("*".equals(t) ? "" : t);
            }
        }
    }

    public static AllowListPolymorphicTypeValidatorBuilder builder() {
        return new AllowListPolymorphicTypeValidatorBuilder();
    }

    private static String getProperty(final String name) {
        final Configuration config = ConfigurationFactory.getInstance().getConfiguration();
        return config.getOptionalValue(name, String.class)
                .orElse(null);
    }
}
