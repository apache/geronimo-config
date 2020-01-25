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

import static java.util.stream.Collectors.toList;

import org.apache.geronimo.config.cdi.configsource.Reloadable;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ConfigExtension implements Extension {
    private Config config;

    private static final Predicate<InjectionPoint> NOT_PROVIDERS = ip -> (ip.getType() instanceof Class) || (ip.getType() instanceof ParameterizedType && ((ParameterizedType)ip.getType()).getRawType() != Provider.class);
    private static final Map<Type, Type> REPLACED_TYPES = new HashMap<>();

    static {
        REPLACED_TYPES.put(double.class, Double.class);
        REPLACED_TYPES.put(int.class, Integer.class);
        REPLACED_TYPES.put(float.class, Float.class);
        REPLACED_TYPES.put(long.class, Long.class);
        REPLACED_TYPES.put(boolean.class, Boolean.class);
        REPLACED_TYPES.put(byte.class, Byte.class);
        REPLACED_TYPES.put(short.class, Short.class);
        REPLACED_TYPES.put(char.class, Character.class);
    }

    private Set<InjectionPoint> injectionPoints = new HashSet<>();
    private Set<Class<?>> proxies = new HashSet<>();
    private List<Class<?>> validProxies;
    private List<ProxyBean<?>> proxyBeans;
    private boolean hasConfigProxy;
    private ConfigBean configBean;

    public ConfigExtension() {
        config = ConfigProvider.getConfig(); // ensure to store the ref the whole lifecycle, java gc is aggressive now
    }

    public void findProxies(@Observes ProcessAnnotatedType<?> pat) {
        final Class<?> javaClass = pat.getAnnotatedType().getJavaClass();
        if (javaClass.isInterface() &&
                Stream.of(javaClass.getMethods()).anyMatch(m -> m.isAnnotationPresent(ConfigProperty.class))) {
            proxies.add(javaClass);
        }
    }


    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ?> pip) {
        ConfigProperty configProperty = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (configProperty != null) {
            injectionPoints.add(pip.getInjectionPoint());
        }
    }

    public void registerConfigProducer(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        Set<Type> types = injectionPoints.stream()
                .filter(NOT_PROVIDERS)
                .map(ip -> REPLACED_TYPES.getOrDefault(ip.getType(), ip.getType()))
                .collect(Collectors.toSet());

        Set<Type> providerTypes = injectionPoints.stream()
                .filter(NOT_PROVIDERS.negate())
                .map(ip -> ((ParameterizedType)ip.getType()).getActualTypeArguments()[0])
                .collect(Collectors.toSet());

        types.addAll(providerTypes);

        types.stream()
                .peek(type -> {
                    if (type == Config.class) {
                        hasConfigProxy = true;
                    }
                })
                .map(type -> new ConfigInjectionBean(bm, type))
                .forEach(abd::addBean);

        validProxies = proxies.stream()
                .filter(this::isValidProxy)
                .collect(toList());
        if (validProxies.size() == proxies.size()) {
            proxyBeans = validProxies.stream()
                                     .map((Function<Class<?>, ? extends ProxyBean<?>>) ProxyBean::new)
                                     .collect(toList());
            proxyBeans.forEach(abd::addBean);
        } // else there are errors

        if (!hasConfigProxy) {
            configBean = new ConfigBean();
            abd.addBean(configBean);
        }
    }

    public void validate(@Observes AfterDeploymentValidation add) {
        List<String> deploymentProblems = new ArrayList<>();

        StreamSupport.stream(config.getConfigSources().spliterator(), false)
                     .filter(Reloadable.class::isInstance)
                     .map(Reloadable.class::cast)
                     .forEach(Reloadable::reload);

        if (!hasConfigProxy) {
            configBean.init(config);
        }
        proxyBeans.forEach(b -> b.init(config));
        proxyBeans.clear();

        for (InjectionPoint injectionPoint : injectionPoints) {
            Type type = injectionPoint.getType();

            // replace native types with their Wrapper types
            type = REPLACED_TYPES.getOrDefault(type, type);

            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            if (type instanceof Class) {
                // a direct injection of a ConfigProperty
                // that means a Converter must exist.
                String key = ConfigInjectionBean.getConfigKey(injectionPoint, configProperty);
                if ((isDefaultUnset(configProperty.defaultValue()))
                        && !config.getOptionalValue(key, (Class) type).isPresent()) {
                    deploymentProblems.add("No Config Value exists for " + key);
                }
            }
        }

        if (!deploymentProblems.isEmpty()) {
            add.addDeploymentProblem(new DeploymentException("Error while validating Configuration\n"
                                                             + String.join("\n", deploymentProblems)));
        }

        if (validProxies.size() != proxies.size()) {
            proxies.stream()
                   .filter(p -> !validProxies.contains(p))
                   .forEach(p -> add.addDeploymentProblem(
                           new DeploymentException("Invalid proxy: " + p + ". All method should have @ConfigProperty.")));
        }
        proxies.clear();
    }

    public void shutdown(@Observes BeforeShutdown bsd) {
        ConfigProviderResolver.instance().releaseConfig(config);
    }

    private boolean isValidProxy(final Class<?> api) {
        return Stream.of(api.getMethods())
                     .allMatch(m -> m.isAnnotationPresent(ConfigProperty.class) || Object.class == m.getDeclaringClass());
    }

    static boolean isDefaultUnset(String defaultValue) {
        return defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE);
    }

    static boolean isDefaultNullValue(String defaultValue) {
        return defaultValue.equals(ConfigProperty.NULL_VALUE);
    }
}
