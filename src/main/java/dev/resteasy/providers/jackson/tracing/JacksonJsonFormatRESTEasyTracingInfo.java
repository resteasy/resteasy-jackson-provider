/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.tracing;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.tracing.api.RESTEasyTracingInfoFormat;
import org.jboss.resteasy.tracing.api.providers.TextBasedRESTEasyTracingInfo;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class JacksonJsonFormatRESTEasyTracingInfo extends TextBasedRESTEasyTracingInfo {

    // TODO (jrp) can we look this up?
    private static final JsonMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Context
    private Providers providers;

    @Override
    public String[] getMessages() {
        try {
            return new String[] { mapper.writeValueAsString(pop()) };
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supports(RESTEasyTracingInfoFormat format) {
        return format.equals(RESTEasyTracingInfoFormat.JSON);
    }

}
