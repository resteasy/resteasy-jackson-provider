/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson;

import java.util.StringTokenizer;

import org.jboss.resteasy.spi.config.Configuration;
import org.jboss.resteasy.spi.config.ConfigurationFactory;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

public class AllowListPolymorphicTypeValidatorBuilder extends BasicPolymorphicTypeValidator.Builder {
    // The documentation does not indicate the ".prefix" part of the property, see RESTEASY-3174. For this reason we're
    // going to allow both the .prefix and non-".prefix" versions.
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
                .or(() -> config.getOptionalValue(name + ".prefix", String.class))
                .orElse(null);
    }
}
