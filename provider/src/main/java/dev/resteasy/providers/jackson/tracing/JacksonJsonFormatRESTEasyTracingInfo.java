/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.tracing;

import org.jboss.resteasy.tracing.api.RESTEasyTracingInfoFormat;
import org.jboss.resteasy.tracing.api.providers.TextBasedRESTEasyTracingInfo;

import dev.resteasy.providers.jackson._private.JacksonLogger;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Provides JSON-formatted RESTEasy tracing output using Jackson.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class JacksonJsonFormatRESTEasyTracingInfo extends TextBasedRESTEasyTracingInfo {

    private static final JsonMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Override
    public String[] getMessages() {
        try {
            return new String[] { mapper.writeValueAsString(pop()) };
        } catch (JacksonException e) {
            throw JacksonLogger.LOGGER.failedToSerializeTracingMessage(e);
        }
    }

    @Override
    public boolean supports(RESTEasyTracingInfoFormat format) {
        return format.equals(RESTEasyTracingInfoFormat.JSON);
    }

}
