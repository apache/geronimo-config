/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.config.configsource;

import org.apache.geronimo.config.cdi.configsource.Reloadable;
import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.enterprise.inject.Typed;
import javax.enterprise.inject.Vetoed;
import java.util.Map;

import static java.lang.Boolean.valueOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * {@link ConfigSource} which uses {@link System#getProperties()}
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
@Typed
@Vetoed
public class SystemPropertyConfigSource extends BaseConfigSource implements Reloadable {
    private static final String COPY_PROPERTY = "org.apache.geronimo.config.configsource.SystemPropertyConfigSource.copy";
    private final Map<String, String> instance;
    private final boolean shouldReload;

    public SystemPropertyConfigSource() {
        this(valueOf(System.getProperty(COPY_PROPERTY, "true")));
    }

    public SystemPropertyConfigSource(boolean copy) {
        instance = load(copy);
        shouldReload = copy;
        initOrdinal(400);
    }

    @Override
    public Map<String, String> getProperties() {
        return instance;
    }

    @Override
    public String getValue(String key) {
        return instance.get(key);
    }

    @Override
    public String getName() {
        return "system-properties";
    }

    @Override
    public void reload() {
        if (!shouldReload) {
            return;
        }
        instance.clear();
        instance.putAll(load(true));
    }

    private Map<String, String> load(final boolean copy) {
        return copy ?
                System.getProperties().stringPropertyNames().stream().collect(toMap(identity(), System::getProperty)) :
                Map.class.cast(System.getProperties());
    }
}
