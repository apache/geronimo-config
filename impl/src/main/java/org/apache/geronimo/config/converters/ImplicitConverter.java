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
package org.apache.geronimo.config.converters;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A Converter factory + impl for 'common sense converters'
 *
 */
public abstract class ImplicitConverter {

    public static <T> MicroProfileTypedConverter<T> getImplicitConverter(Class<T> clazz) {

        // handle ct with String param
        Converter<T> converter = hasConverterCt(clazz, String.class);
        if (converter == null) {
            converter = hasConverterCt(clazz, CharSequence.class);
        }
        if (converter == null) {
            converter = hasConverterMethod(clazz, "valueOf", String.class);
        }
        if (converter == null) {
            converter = hasConverterMethod(clazz, "valueOf", CharSequence.class);
        }
        if (converter == null) {
            converter = hasConverterMethod(clazz, "parse", String.class);
        }
        if (converter == null) {
            converter = hasConverterMethod(clazz, "parse", CharSequence.class);
        }
        if (converter == null) {
            return null;
        }
        return new MicroProfileTypedConverter<T>(converter, 100);
    }

    private static <T> Converter<T> hasConverterCt(Class<T> clazz, Class<?> paramType) {
        try {
            final Constructor<?> declaredConstructor = clazz.getDeclaredConstructor(paramType);
            if (!declaredConstructor.isAccessible()) {
                declaredConstructor.setAccessible(true);
            }
            return value -> {
                try {
                    return (T)declaredConstructor.newInstance(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException e) {
            // all fine
        }
        return null;
    }

    private static <T> Converter<T> hasConverterMethod(Class<T> clazz, String methodName, Class<?> paramType) {
        // handle valueOf with CharSequence param
        try {
            final Method method = clazz.getMethod(methodName, paramType);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            if (Modifier.isStatic(method.getModifiers())) {
                return value -> {
                    try {
                        return (T)method.invoke(null, value);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        } catch (NoSuchMethodException e) {
            // all fine
        }
        return null;
    }
}
