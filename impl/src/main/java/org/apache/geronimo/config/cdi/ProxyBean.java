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

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.config.Config;

public class ProxyBean<T> implements Bean<T>, PassivationCapable {
    private static final Set<Annotation> QUALIFIERS = new HashSet<>(
            asList(DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE));
    private final Class<T> beanClass;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final String id;
    private Config config;

    ProxyBean(final Class<T> api) {
        this.beanClass = api;
        this.qualifiers = QUALIFIERS;
        this.types = new HashSet<>(asList(api, Serializable.class));
        this.id = ProxyBean.class.getName() + "[" + api.getName() + "]";
    }

    void init(final Config config) {
        this.config = config;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return emptySet();
    }

    @Override
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(final CreationalContext<T> context) {
        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { beanClass },
                new ConfigurationHandler(config, beanClass));
    }

    @Override
    public void destroy(final T instance, final CreationalContext<T> context) {
        // no-op
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }
}
