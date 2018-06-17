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
package org.apache.geronimo.config.configsource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class PropertyFileConfigSource extends BaseConfigSource {
    private static final Logger LOG = Logger.getLogger(PropertyFileConfigSource.class.getName());
    private Map<String, String> properties;
    private String fileName;

    public PropertyFileConfigSource(URL propertyFileUrl) {
        fileName = propertyFileUrl.toExternalForm();
        properties = loadProperties(propertyFileUrl);
        initOrdinal(100);
    }

    /**
     * The given key gets used for a lookup via a properties file
     *
     * @param key for the property
     * @return value for the given key or null if there is no configured value
     */
    @Override
    public String getValue(String key) {
        return properties.get(key);
    }

    @Override
    public String getName() {
        return fileName;
    }


    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    private Map<String, String> loadProperties(URL url) {
        Properties props = new Properties();

        try (InputStream inputStream = url.openStream()) {

            props.load(inputStream);
        } catch (IOException e) {
            // don't return null on IOException
            LOG.log(Level.WARNING, "Unable to read URL " + url, e);
        }

        return (Map) props;
    }

}
