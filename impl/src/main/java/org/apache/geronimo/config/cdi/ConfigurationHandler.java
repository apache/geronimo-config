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
import static java.util.stream.Collectors.toMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
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
        private final Class type;
        private final boolean optional;

        private MethodMeta(final Method m) {
            final ConfigProperty annotation = m.getAnnotation(ConfigProperty.class);
            optional = Optional.class == m.getReturnType();
            type = optional ?
                    Class.class.cast(ParameterizedType.class.cast(m.getGenericReturnType()).getActualTypeArguments()[0]) :
                    m.getReturnType();
            key = annotation.name().isEmpty() ? m.getDeclaringClass().getName() + "." + m.getName() : annotation.name();
            final boolean hasDefault = !annotation.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE);
            if (type == long.class || type == Long.class) {
                defaultValue = hasDefault ? Long.parseLong(annotation.defaultValue()) : 0L;
            } else if (type == int.class || type == Integer.class) {
                defaultValue = hasDefault ? Integer.parseInt(annotation.defaultValue()) : 0;
            } else if (type == double.class || type == Double.class) {
                defaultValue = hasDefault ? Double.parseDouble(annotation.defaultValue()) : 0.;
            } else if (type == float.class || type == Float.class) {
                defaultValue = hasDefault ? Float.parseFloat(annotation.defaultValue()) : 0f;
            } else if (type == short.class || type == Short.class) {
                defaultValue = hasDefault ? Short.parseShort(annotation.defaultValue()) : (short) 0;
            } else if (type == char.class || type == Character.class) {
                defaultValue = hasDefault ? annotation.defaultValue().charAt(0) : (type == char.class ? (char) 0 : null);
            } else if (type == String.class) {
                defaultValue = hasDefault ? annotation.defaultValue() : null;
            } else if (hasDefault) {
                throw new IllegalArgumentException("Unsupported default for " + m);
            } else {
                defaultValue = null;
            }
        }

        Object read(final Config config) {
            final Optional optionalValue = config.getOptionalValue(key, type);
            if (optional) {
                return optionalValue;
            }
            return optionalValue.orElse(defaultValue);
        }
    }
}
