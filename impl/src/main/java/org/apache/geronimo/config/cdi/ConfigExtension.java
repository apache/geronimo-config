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

import org.apache.geronimo.config.ConfigImpl;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ConfigExtension implements Extension {
    private static final Object NULL = new Object();

    private Config config;
    private ConfigProviderResolver resolver;

    private Set<Injection> injections = new HashSet<>();
    private List<Throwable> deploymentProblems = new ArrayList<>();
    private static final Map<Type, Type> REPLACED_TYPES = new HashMap<>();

    static {
        REPLACED_TYPES.put(double.class, Double.class);
        REPLACED_TYPES.put(int.class, Integer.class);
        REPLACED_TYPES.put(float.class, Float.class);
        REPLACED_TYPES.put(long.class, Long.class);
        REPLACED_TYPES.put(boolean.class, Boolean.class);
    }

    void init(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager bm) {
        resolver = ConfigProviderResolver.instance();
        config = getConfig();
    }

    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ?> pip) {
        final InjectionPoint injectionPoint = pip.getInjectionPoint();
        final ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
        if (configProperty != null) {
            Type replacedType = REPLACED_TYPES.getOrDefault(injectionPoint.getType(), injectionPoint.getType());
            Injection injection = new Injection(replacedType);
            final String key = getConfigKey(injectionPoint, configProperty);
            final boolean defaultUnset = isDefaultUnset(configProperty.defaultValue());
            if (!injections.add(injection)) {
                final Injection ref = injection;
                injection = injections.stream().filter(i -> i.equals(ref)).findFirst().get();
            }
            injection.keys.add(key);
            injection.defaultValues.add(configProperty.defaultValue());

            final ConfigImpl configImpl = unwrapConfig();

            // what about lazy runtime lookup, not consistent with tck and system prop usage, for now assume optional=optional ;)
            boolean hasValue = true;
            if (defaultUnset) { // value validation
                if (ParameterizedType.class.isInstance(injection.type)) {
                    final ParameterizedType pt = ParameterizedType.class.cast(injection.type);
                    if (pt.getRawType() != Optional.class && !configImpl.getOptionalValue(key, String.class).isPresent()) {
                        hasValue = false;
                    }
                } else if (!configImpl.getOptionalValue(key, String.class).isPresent()) {
                    hasValue = false;
                }
                if (!hasValue) {
                    deploymentProblems.add(new IllegalArgumentException("Missing converter for '" + key + "' from " + injectionPoint));
                }
            }

            Class<?> instanceType = null;
            if (ParameterizedType.class.isInstance(injection.type)) { // converters validation
                final ParameterizedType pt = ParameterizedType.class.cast(injection.type);
                if (pt.getRawType() == Provider.class && pt.getActualTypeArguments().length == 1 && Class.class.isInstance(pt.getActualTypeArguments()[0])
                        && !configImpl.getConverters().containsKey(Class.class.cast(pt.getActualTypeArguments()[0]))) {
                    instanceType = Class.class.cast(pt.getActualTypeArguments()[0]);
                } // else if Optional it is fine, else we don't know how to process
            } else if (Class.class.isInstance(injection.type)) {
                instanceType = Class.class.cast(injection.type);
            }
            if (instanceType != null) { // validate we have a converter + we can convert the existing value
                if (!configImpl.getConverters().containsKey(instanceType)) {
                    deploymentProblems.add(new IllegalArgumentException("Missing converter for '" + key + "' from " + injectionPoint));
                } else if (hasValue) {
                    try {
                        configImpl.getConverters().get(injection.type).convert(configImpl.getValue(key));
                    } catch (final RuntimeException re) {
                        deploymentProblems.add(re);
                    }
                }
            }
        }
    }

    public void registerConfigProducer(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        injections.stream()
                .flatMap(injection -> {
                    final Function<CreationalContext<?>, String> keyProvider;
                    if (injection.keys.size() == 1) {
                        final String key = injection.keys.iterator().next();
                        keyProvider = ctx -> key;
                    } else {
                        keyProvider = ctx -> getName(findInjectionPoint(bm, ctx));
                    }

                    if (ParameterizedType.class.isInstance(injection.type)) {
                        final ParameterizedType paramType = ParameterizedType.class.cast(injection.type);
                        final Type rawType = paramType.getRawType();

                        // todo: do we care of Instance injection? doesnt make much sense right?
                        if (Provider.class == rawType && paramType.getActualTypeArguments().length == 1) {
                            if (!Class.class.isInstance(paramType.getActualTypeArguments()[0])) {
                                deploymentProblems.add(new IllegalArgumentException("@ConfigProperty can only be used with Provider<T> where T is a Class"));
                                return Stream.empty();
                            }
                            final Class<?> providerType = Class.class.cast(paramType.getActualTypeArguments()[0]);
                            return Stream.of(new ConfigInjectionBean<Provider<?>>(injection.type, true) {
                                @Override
                                public Provider<?> create(final CreationalContext<Provider<?>> context) {
                                    return () -> config.getValue(keyProvider.apply(context), providerType);
                                }
                            });
                        } else if (Optional.class == rawType && paramType.getActualTypeArguments().length == 1) {
                            if (!Class.class.isInstance(paramType.getActualTypeArguments()[0])) {
                                deploymentProblems.add(new IllegalArgumentException("@ConfigProperty can only be used with Optional<T> where T is a Class"));
                                return null;
                            }
                            final Class<?> optionalType = Class.class.cast(paramType.getActualTypeArguments()[0]);
                            return Stream.of(new ConfigInjectionBean<Optional<?>>(injection.type) {
                                @Override
                                public Optional<?> create(final CreationalContext<Optional<?>> context) {
                                    return config.getOptionalValue(keyProvider.apply(context), optionalType);
                                }
                            });
                        } else {
                            deploymentProblems.add(new IllegalArgumentException("Unsupported parameterized type " + paramType));
                            return Stream.empty();
                        }
                    } else if (Class.class.isInstance(injection.type)) {
                        final Class clazz = Class.class.cast(injection.type);
                        final ConfigInjectionBean bean;
                        if (injection.defaultValues.isEmpty()) {
                            bean = new ConfigInjectionBean<Object>(injection.type) {
                                @Override
                                public Object create(final CreationalContext<Object> context) {
                                    return config.getOptionalValue(keyProvider.apply(context), clazz);
                                }
                            };
                        } else if (injection.defaultValues.size() == 1) { // common enough to be optimized
                            final String defVal = injection.defaultValues.iterator().next();
                            final Object alternativeVal = isDefaultUnset(defVal) ? null : unwrapConfig().convert(defVal, clazz);
                            bean = new ConfigInjectionBean<Object>(injection.type) {
                                @Override
                                public Object create(final CreationalContext<Object> context) {
                                    final Optional optionalValue = config.getOptionalValue(keyProvider.apply(context), clazz);
                                    return optionalValue.orElse(alternativeVal);
                                }
                            };
                        } else { // sadly we need to get back to the injection point to know which one we need to use
                            final Map<String, Object> prepared = injection.defaultValues.stream()
                                    .collect(toMap(identity(), k -> isDefaultUnset(k) ? NULL : unwrapConfig().convert(k, clazz), (a, b) -> b));
                            bean = new ConfigInjectionBean<Object>(injection.type) {
                                @Override
                                public Object create(final CreationalContext<Object> context) {
                                    final InjectionPoint ip = findInjectionPoint(bm, context);
                                    if (ip == null) {
                                        throw new IllegalStateException("Could not retrieve InjectionPoint");
                                    }
                                    return config.getOptionalValue(ConfigExtension.this.getName(ip), clazz)
                                            .orElseGet(() -> {
                                                final Object val = prepared.get(ip.getAnnotated().getAnnotation(ConfigProperty.class).defaultValue());
                                                return val == NULL ? null : val;
                                            });
                                }
                            };
                        }

                        final Collection<ConfigInjectionBean<?>> beans = new ArrayList<>();
                        beans.add(bean);

                        // is adding these beans is that useful? we captured them all so only a programmatic lookup would justify it
                        // and not sure it would be done this way anyway
                        final ParameterizedTypeImpl providerType = new ParameterizedTypeImpl(Provider.class, injection.type);
                        if (injections.stream().noneMatch(i -> i.type.equals(providerType))) {
                            beans.add(new ConfigInjectionBean<Provider<?>>(providerType, true) {
                                @Override
                                public Provider<?> create(final CreationalContext<Provider<?>> context) {
                                    return () -> bean.create(context);
                                }
                            });
                        }

                        final ParameterizedTypeImpl optionalType = new ParameterizedTypeImpl(Optional.class, injection.type);
                        if (injections.stream().noneMatch(i -> i.type.equals(optionalType))) {
                            beans.add(new ConfigInjectionBean<Optional<?>>(optionalType) {
                                @Override
                                public Optional<?> create(final CreationalContext<Optional<?>> context) {
                                    return Optional.ofNullable(bean.create(context));
                                }
                            });
                        }

                        return beans.stream();
                    } else {
                        deploymentProblems.add(new IllegalArgumentException("Unknown type " + injection.type));
                        return Stream.empty();
                    }
                })
                .forEach(abd::addBean);
    }

    public void validate(@Observes AfterDeploymentValidation add) {
        deploymentProblems.forEach(add::addDeploymentProblem);
        injections.clear();
        deploymentProblems.clear();
    }

    public void shutdown(@Observes BeforeShutdown bsd) {
        resolver.releaseConfig(config);
    }

    private ConfigImpl unwrapConfig() {
        return ConfigImpl.class.cast(config);
    }

    private String getName(final InjectionPoint ip) {
        final ConfigProperty annotation = ip.getAnnotated().getAnnotation(ConfigProperty.class);
        final String name = annotation.name();
        return isDefaultUnset(name) ? getConfigKey(ip, annotation) : name;
    }

    /**
     * Get the property key to use.
     * In case the {@link ConfigProperty#name()} is empty we will try to determine the key name from the InjectionPoint.
     */
    private static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (!key.isEmpty()) {
            return key;
        }
        if (ip.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember member = (AnnotatedMember) ip.getAnnotated();
            AnnotatedType declaringType = member.getDeclaringType();
            if (declaringType != null) {
                String[] parts = declaringType.getJavaClass().getName().split(".");
                String cn = parts[parts.length - 1];
                parts[parts.length - 1] = Character.toLowerCase(cn.charAt(0)) + (cn.length() > 1 ? cn.substring(1) : "");
                StringBuilder sb = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    sb.append(".").append(parts[i]);
                }

                // now add the field name
                sb.append(".").append(member.getJavaMember().getName());
                return sb.toString();
            }
        }

        throw new IllegalStateException("Could not find default name for @ConfigProperty InjectionPoint " + ip);
    }

    private static boolean isDefaultUnset(String defaultValue) {
        return defaultValue == null || defaultValue.length() == 0 || defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE);
    }

    private static InjectionPoint findInjectionPoint(final BeanManager bm, final CreationalContext<?> ctx) {
        return InjectionPoint.class.cast(
                bm.getReference(bm.resolve(bm.getBeans(InjectionPoint.class)), InjectionPoint.class, ctx));
    }

    private static final class Injection {
        private final Type type;
        private final Collection<String> keys = new ArrayList<>();
        private final Collection<String> defaultValues = new ArrayList<>();

        private Injection(final Type type) {
            this.type = type;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || Injection.class != o.getClass()) {
                return false;
            }
            final Injection injection = Injection.class.cast(o);
            return Objects.equals(type, injection.type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }
    }

    private class ParameterizedTypeImpl implements ParameterizedType {
        private final Type rawType;
        private final Type[] types;

        private ParameterizedTypeImpl(final Type raw, final Type... types) {
            this.rawType = raw;
            this.types = types;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return types.clone();
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(types) ^ rawType.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (ParameterizedType.class.isInstance(obj)) {
                final ParameterizedType that = ParameterizedType.class.cast(obj);
                final Type thatRawType = that.getRawType();
                return (rawType == null ? thatRawType == null : rawType.equals(thatRawType))
                        && Arrays.equals(types, that.getActualTypeArguments());
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(Class.class.cast(rawType).getName());
            final Type[] actualTypes = getActualTypeArguments();
            if (actualTypes.length > 0) {
                buffer.append("<");
                final int length = actualTypes.length;
                for (int i = 0; i < length; i++) {
                    if (actualTypes[i] instanceof Class) {
                        buffer.append(((Class<?>) actualTypes[i]).getSimpleName());
                    } else {
                        buffer.append(actualTypes[i].toString());
                    }
                    if (i != actualTypes.length - 1) {
                        buffer.append(",");
                    }
                }

                buffer.append(">");
            }
            return buffer.toString();
        }
    }
}
