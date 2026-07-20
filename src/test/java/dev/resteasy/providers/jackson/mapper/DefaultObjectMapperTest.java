/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.providers.jackson.mapper;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.providers.jackson.mapper.resources.ContactResource;
import dev.resteasy.providers.jackson.mapper.resources.CrudResource;
import dev.resteasy.providers.jackson.mapper.resources.UserResource;

/**
 * Tests the default {@link tools.jackson.databind.json.JsonMapper} works as expected.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap({
        ContactResource.class,
        CrudResource.class,
        UserResource.class
})
public class DefaultObjectMapperTest extends AbstractObjectMapperTest {

    public DefaultObjectMapperTest() {
        super(false);
    }
}
