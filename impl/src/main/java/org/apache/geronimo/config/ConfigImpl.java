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

import org.apache.geronimo.config.converters.ImplicitConverter;
import org.apache.geronimo.config.converters.MicroProfileTypedConverter;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import javax.enterprise.inject.Typed;
import javax.enterprise.inject.Vetoed;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:johndament@apache.org">John D. Ament</a>
 */
@Typed
@Vetoed
public class ConfigImpl implements Config {
    protected Logger logger = Logger.getLogger(ConfigImpl.class.getName());

    protected final List<ConfigSource> configSources = new ArrayList<>();
    protected final Map<Type, MicroProfileTypedConverter> converters = new HashMap<>();
    protected final Map<Type, Converter> implicitConverters = new ConcurrentHashMap<>();
    private static final String ARRAY_SEPARATOR_REGEX = "(?<!\\\\)" + Pattern.quote(",");

    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> asType) {
        String value = getValue(propertyName);
        if (value != null && value.length() == 0) {
            // treat an empty string as not existing
            value = null;
        }
        return Optional.ofNullable(convert(value, asType));
    }

    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        String value = getValue(propertyName);
        if (value == null) {
            throw new NoSuchElementException("No configured value found for config key " + propertyName);
        }

        return convert(value, propertyType);
    }

    public String getValue(String key) {
        for (ConfigSource configSource : configSources) {
            String value = configSource.getValue(key);

            if (value != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "found value {0} for key {1} in ConfigSource {2}.",
                            new Object[]{value, key, configSource.getName()});
                }

                return value;
            }
        }

        return null;
    }

    public <T> T convert(String value, Class<T> asType) {
        if (value != null) {
            if(asType.isArray()) {
                Class<?> elementType = asType.getComponentType();
                List<?> elements = convertList(value, elementType);
                Object arrayInst = Array.newInstance(elementType, elements.size());
                for (int i = 0; i < elements.size(); i++) {
                    Array.set(arrayInst, i, elements.get(i));
                }
                return (T) arrayInst;
            } else {
                Converter<T> converter = getConverter(asType);
                return converter.convert(value);
            }
        }
        return null;
    }

    public <T> List<T> convertList(String rawValue, Class<T> arrayElementType) {
        Converter<T> converter = getConverter(arrayElementType);
        String[] parts = rawValue.split(ARRAY_SEPARATOR_REGEX);
        if(parts.length == 0) {
            return Collections.emptyList();
        }
        List<T> elements = new ArrayList<>(parts.length);
        for (String part : parts) {
            part = part.replace("\\,", ",");
            T converted = converter.convert(part);
            elements.add(converted);
        }
        return elements;
    }

    private <T> Converter getConverter(Class<T> asType) {
        MicroProfileTypedConverter microProfileTypedConverter = converters.get(asType);
        Converter converter = null;
        if(microProfileTypedConverter != null) {
            converter = microProfileTypedConverter.getDelegate();
        }
        if (converter == null) {
            converter = getImplicitConverter(asType);
        }
        if (converter == null) {
            throw new IllegalArgumentException("No Converter registered for class " + asType);
        }
        return converter;
    }

    private <T> Converter getImplicitConverter(Class<T> asType) {
        Converter converter = implicitConverters.get(asType);
        if (converter == null) {
            synchronized (implicitConverters) {
                converter = implicitConverters.get(asType);
                if (converter == null) {
                    // try to check whether the class is an 'implicit converter'
                    converter = ImplicitConverter.getImplicitConverter(asType);
                    if (converter != null) {
                        implicitConverters.putIfAbsent(asType, converter);
                    }
                }
            }
        }
        return converter;
    }

    public ConfigValueImpl<String> access(String key) {
        return new ConfigValueImpl<>(this, key);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return configSources.stream().flatMap(c -> c.getPropertyNames().stream()).collect(Collectors.toSet());
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return Collections.unmodifiableList(configSources);
    }

    public synchronized void addConfigSources(List<ConfigSource> configSourcesToAdd) {
        List<ConfigSource> allConfigSources = new ArrayList<>(configSources);
        allConfigSources.addAll(configSourcesToAdd);

        // finally put all the configSources back into the map
        synchronized (configSources) {
            configSources.clear();
            configSources.addAll(sortDescending(allConfigSources));
        }
    }

    public Map<Type, MicroProfileTypedConverter> getConverters() {
        return converters;
    }


    private List<ConfigSource> sortDescending(List<ConfigSource> configSources) {
        configSources.sort(
                (configSource1, configSource2) -> (configSource1.getOrdinal() > configSource2.getOrdinal()) ? -1 : 1);
        return configSources;

    }

    public void addConverter(Type type, MicroProfileTypedConverter<?> converter) {
        converters.put(type, converter);
    }
}