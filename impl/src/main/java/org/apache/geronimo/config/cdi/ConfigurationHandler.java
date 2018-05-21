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
package org.apache.geronimo.config.cdi;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.geronimo.config.ConfigImpl;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ConfigurationHandler implements InvocationHandler {
    private final Config config;
    private final Map<Method, MethodMeta> methodMetas;

    ConfigurationHandler(final Config config, final Class<?> api) {
        this.config = config;
        this.methodMetas = Stream.of(api.getMethods())
            .filter(m -> m.isAnnotationPresent(ConfigProperty.class))
            .collect(toMap(identity(), MethodMeta::new));
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            try {
                return method.invoke(this, args);
            } catch (final InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
        final MethodMeta methodMeta = methodMetas.get(method);
        if (methodMeta != null) {
            return methodMeta.read(config);
        }
        return null;
    }

    // todo: list, set etc handling but config API is not that friendly for now (Class vs Type)
    private static class MethodMeta {
        private final String key;
        private final Object defaultValue;
        private final Class lookupType;
        private final Class collectionConversionType;
        private final Collector<Object, ?, ? extends Collection<Object>> collectionCollector;

        private final boolean optional;

        private MethodMeta(final Method m) {
            final ConfigProperty annotation = m.getAnnotation(ConfigProperty.class);
            optional = Optional.class == m.getReturnType();
            final Type type = optional ?
                    ParameterizedType.class.cast(m.getGenericReturnType()).getActualTypeArguments()[0] :
                    m.getGenericReturnType();

            if (Class.class.isInstance(type)) {
                lookupType = Class.class.cast(type);
                collectionCollector = null;
                collectionConversionType = null;
            } else if (ParameterizedType.class.isInstance(type)) {
                final ParameterizedType pt = ParameterizedType.class.cast(type);
                final Type rawType = pt.getRawType();
                if (!Class.class.isInstance(rawType)) {
                    throw new IllegalArgumentException("Unsupported parameterized type: " + type);
                }

                final Class<?> clazz = Class.class.cast(pt.getRawType());
                if (Collection.class.isAssignableFrom(clazz)) {
                    collectionConversionType = Class.class.cast(pt.getActualTypeArguments()[0]);
                    lookupType = String.class;
                    if (Set.class.isAssignableFrom(clazz)) {
                        collectionCollector = toSet();
                    } else {
                        collectionCollector = toList();
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported parameterized type: " + type + ", did you want a Collection?");
                }
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type);
            }

            key = annotation.name().isEmpty() ? m.getDeclaringClass().getName() + "." + m.getName() : annotation.name();
            final boolean hasDefault = !annotation.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE);
            if (lookupType == long.class || lookupType == Long.class) {
                defaultValue = hasDefault ? Long.parseLong(annotation.defaultValue()) : 0L;
            } else if (lookupType == int.class || lookupType == Integer.class) {
                defaultValue = hasDefault ? Integer.parseInt(annotation.defaultValue()) : 0;
            } else if (lookupType == double.class || lookupType == Double.class) {
                defaultValue = hasDefault ? Double.parseDouble(annotation.defaultValue()) : 0.;
            } else if (lookupType == float.class || lookupType == Float.class) {
                defaultValue = hasDefault ? Float.parseFloat(annotation.defaultValue()) : 0f;
            } else if (lookupType == short.class || lookupType == Short.class) {
                defaultValue = hasDefault ? Short.parseShort(annotation.defaultValue()) : (short) 0;
            } else if (lookupType == char.class || lookupType == Character.class) {
                defaultValue = hasDefault ? annotation.defaultValue().charAt(0) : (lookupType == char.class ? (char) 0 : null);
            } else if (collectionCollector != null) {
                defaultValue = hasDefault ? convert(annotation.defaultValue(), ConfigProvider.getConfig()) : null;
            } else if (lookupType == String.class) {
                defaultValue = hasDefault ? annotation.defaultValue() : null;
            } else if (hasDefault) {
                throw new IllegalArgumentException("Unsupported default for " + m);
            } else {
                defaultValue = null;
            }
        }

        Object read(final Config config) {
            final Optional optionalValue = config.getOptionalValue(key, lookupType);
            if (optional) {
                return processOptional(optionalValue, config);
            }
            return processOptional(optionalValue, config).orElse(defaultValue);
        }

        private Optional processOptional(final Optional<?> optionalValue, final Config config) {
            if (collectionCollector != null) {
                return optionalValue.map(String.class::cast).map(v -> convert(v, config));
            }
            return optionalValue;
        }

        private Collection<?> convert(final String o, final Config config) {
            final String[] values = o.split(",");
            return Stream.of(values)
                    .map(v -> mapValue(v, config))
                    .collect(collectionCollector);
        }

        private Object mapValue(final String raw, final Config config) {
            if (String.class == collectionConversionType) {
                return raw;
            }
            if (ConfigImpl.class.isInstance(config)) {
                return ConfigImpl.class.cast(config).convert(raw, collectionConversionType);
            }
            throw new IllegalArgumentException("Unsupported conversion if config instance is not a ConfigImpl: " + collectionConversionType);
        }
    }
}
