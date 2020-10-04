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

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Provider;

import org.apache.geronimo.config.ConfigImpl;
import org.apache.geronimo.config.ConfigValueImpl;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ConfigInjectionBean<T> implements Bean<T>, PassivationCapable {

    private final static Set<Annotation> QUALIFIERS = new HashSet<>();
    static {
        QUALIFIERS.add(new ConfigPropertyLiteral());
    }

    private final BeanManager bm;
    private final Class rawType;
    private final Set<Type> types;
    private final String id;

    /**
     * only access via {@link #getConfig(}
     */
    private ConfigImpl _config;

    public ConfigInjectionBean(BeanManager bm, Type type) {
        this.bm = bm;
        types = new HashSet<>();
        types.add(type);
        rawType = getRawType(type);
        this.id = "ConfigInjectionBean_" + types;
    }

    private Class getRawType(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        }
        else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;

            return (Class) paramType.getRawType();
        }

        throw new UnsupportedOperationException("No idea how to handle " + type);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public Class<?> getBeanClass() {
        return rawType;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(CreationalContext<T> context) {
        final InjectionPoint ip = (InjectionPoint)bm.getInjectableReference(new ConfigInjectionPoint(this),context);
        if (ip == null) {
            throw new IllegalStateException("Could not retrieve InjectionPoint");
        }
        final Annotated annotated = ip.getAnnotated();
        final ConfigProperty configProperty = annotated.getAnnotation(ConfigProperty.class);
        final String key = getConfigKey(ip, configProperty);
        final String defaultValue = configProperty.defaultValue();
        final boolean canBeNull = ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue);
        return toInstance(
                annotated.getBaseType(),
                key,
                canBeNull || ConfigExtension.isDefaultUnset(defaultValue) ? null : defaultValue,
                true,
                canBeNull);
    }

    private T toInstance(final Type baseType, final String key,
                         final String defaultValue, final boolean skipProviderLevel,
                         final boolean acceptNull) {
        if (baseType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) baseType;
            Type rawType = paramType.getRawType();
            if (paramType.getActualTypeArguments().length == 0) {
                throw new IllegalArgumentException("No argument to " + paramType);
            }

            Type arg = paramType.getActualTypeArguments()[0];
            if (!Class.class.isInstance(arg)) {
                if (ParameterizedType.class.isInstance(arg)) {
                    ParameterizedType nested = ParameterizedType.class.cast(arg);
                    if (rawType == Optional.class) {
                        return (T) ofNullable(toInstance(nested, key, defaultValue, false, true));
                    }
                    if (rawType == Provider.class) {
                        if (nested.getActualTypeArguments().length != 1) {
                            throw new IllegalArgumentException("Invalid arguments for " + paramType);
                        }
                        return skipProviderLevel ?
                                toInstance(nested, key, defaultValue, false, acceptNull) :
                                (T) (Provider<?>) () -> toInstance(nested, key, defaultValue, false, true);
                    }
                    if (rawType == Supplier.class) {
                        if (nested.getActualTypeArguments().length != 1) {
                            throw new IllegalArgumentException("Invalid arguments for " + paramType);
                        }
                        return (T) (Supplier<?>) () -> toInstance(nested, key, defaultValue, false, true);
                    }
                }
                throw new IllegalArgumentException("Unsupported multiple generics level: " + paramType);
            }

            Class clazzParam = (Class) arg;

            // handle Provider<T>
            if (rawType instanceof Class && rawType == Provider.class && paramType.getActualTypeArguments().length == 1) {
                return skipProviderLevel ?
                        toInstance(clazzParam, key, defaultValue, false, acceptNull) :
                        (T) (Provider<?>) () -> toInstance(clazzParam, key, defaultValue, false, true);
            }

            // handle Optional<T>
            if (rawType instanceof Class && rawType == Optional.class && paramType.getActualTypeArguments().length == 1) {
                return (T) getConfig().getOptionalValue(key, clazzParam);
            }

            if (rawType instanceof Class && rawType == Supplier.class && paramType.getActualTypeArguments().length == 1) {
                return (T) (Supplier<?>) () -> toInstance(clazzParam, key, defaultValue, false, true);
            }

            if (Set.class.equals(rawType)) {
                final List list = getList(key, clazzParam, defaultValue, acceptNull);
                return list == null ? null : (T) new HashSet(list);
            }
            if (List.class.equals(rawType)) {
                return (T) getList(key, clazzParam, defaultValue, acceptNull);
            }
            throw new IllegalStateException("unhandled ConfigProperty");
        }
        Class clazz = (Class) baseType;
        return getConfigValue(key, defaultValue, clazz, acceptNull);
    }

    private List getList(final String key, final Class clazzParam, final String defaultValue, final boolean nullable) {
        final ConfigImpl config = getConfig();
        ConfigValueImpl configValue = config
                .access(key)
                .as(clazzParam)
                .asList()
                .evaluateVariables(true);

        if (defaultValue != null)
        {
            configValue.withStringDefault(defaultValue);
        }
        else if (nullable) // list default is emptyList, reset it for nullable values
        {
            configValue.withDefault(null);
        }

        return (List) configValue.get();
    }

    private T getConfigValue(final String key, final String defaultValue, final Class clazz, final boolean canBeNull) {
        final ConfigImpl config = getConfig();
        if (canBeNull || defaultValue != null) {
            return (T) config.getOptionalValue(key, clazz)
                    .orElse(defaultValue == null ? null : config.convert(defaultValue, clazz));
        }
        return (T) config.getValue(key, clazz);
    }

    /**
     * Get the property key to use.
     * In case the {@link ConfigProperty#name()} is empty we will try to determine the key name from the InjectionPoint.
     */
    static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (key.length() > 0) {
            return key;
        }
        if (ip.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember member = (AnnotatedMember) ip.getAnnotated();
            AnnotatedType declaringType = member.getDeclaringType();
            if (declaringType != null) {
                return declaringType.getJavaClass().getCanonicalName() + "." + member.getJavaMember().getName();
            }
        }

        throw new IllegalStateException("Could not find default name for @ConfigProperty InjectionPoint " + ip);
    }

    public ConfigImpl getConfig() {
        if (_config == null) {
            _config = (ConfigImpl) ConfigProvider.getConfig();
        }
        return _config;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> context) {

    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    private static class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty> implements ConfigProperty {
        @Override
        public String name() {
            return "";
        }

        @Override
        public String defaultValue() {
            return "";
        }
    }
}
