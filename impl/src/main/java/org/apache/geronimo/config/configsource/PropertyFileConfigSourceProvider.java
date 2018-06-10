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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Typed;
import javax.enterprise.inject.Vetoed;

import javax.config.spi.ConfigSource;
import javax.config.spi.ConfigSourceProvider;


/**
 * Register all property files with the given propertyFileName
 * as {@link ConfigSource}.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
@Typed
@Vetoed
public class PropertyFileConfigSourceProvider implements ConfigSourceProvider {
    private static final Logger LOG = Logger.getLogger(PropertyFileConfigSourceProvider.class.getName());

    private List<ConfigSource> configSources = new ArrayList<ConfigSource>();

    public PropertyFileConfigSourceProvider(String propertyFileName, boolean optional, ClassLoader forClassLoader) {
        try {
            Collection<URL> propertyFileUrls = resolvePropertyFiles(forClassLoader, propertyFileName);
            if (!optional && propertyFileUrls.isEmpty()) {
                throw new IllegalStateException(propertyFileName + " wasn't found.");
            }

            for (URL propertyFileUrl : propertyFileUrls) {
                LOG.log(Level.INFO,
                        "Custom config found by GeronimoConfig. Name: ''{0}'', URL: ''{1}''",
                        new Object[]{propertyFileName, propertyFileUrl});
                configSources.add(new PropertyFileConfigSource(propertyFileUrl));
            }
        }
        catch (IOException ioe) {
            throw new IllegalStateException("problem while loading GeronimoConfig property files", ioe);
        }

    }

    public Collection<URL> resolvePropertyFiles(ClassLoader forClassLoader, String propertyFileName) throws IOException {
        // de-duplicate
        Map<String, URL> propertyFileUrls = resolveUrls(propertyFileName, forClassLoader);

        // and once again with preceding a "/"
        propertyFileUrls.putAll(resolveUrls("/" + propertyFileName, forClassLoader));

        return propertyFileUrls.values();
    }

    private Map<String, URL> resolveUrls(String propertyFileName, ClassLoader forClassLoader) throws IOException {
        Map<String, URL> propertyFileUrls = new HashMap<>();
        Enumeration<URL> urls = forClassLoader.getResources(propertyFileName);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            propertyFileUrls.put(url.toExternalForm(), url);
        }
        return propertyFileUrls;
    }


    @Override
    public List<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return configSources;
    }
}
