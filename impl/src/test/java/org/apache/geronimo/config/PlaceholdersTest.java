/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.config;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.geronimo.config.configsource.BaseConfigSource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

public class PlaceholdersTest {
    private final Config config = ConfigProviderResolver.instance()
        .getBuilder()
        .withSources(new BaseConfigSource() {
            private final Map<String, String> properties = new HashMap<String, String>() {{
                put("p1", "v1");
                put("p2", "v2");
            }};

            @Override
            public Map<String, String> getProperties() {
                return properties;
            }

            @Override
            public String getValue(final String s) {
                return getProperties().get(s);
            }

            @Override
            public String getName() {
                return "geronimo-config-test";
            }
        })
        .build();

    @Test
    public void simplePlaceholder() {
        assertEquals("v1", Placeholders.replace(config, "${p1}"));
    }

    @Test
    public void defaultPlaceholder() {
        assertEquals("d1", Placeholders.replace(config, "${missing:d1}"));
    }

    @Test
    public void nestedPlaceholder() {
        assertEquals("v2/foo", Placeholders.replace(config, "${missing:${p2}/foo}"));
    }
}
