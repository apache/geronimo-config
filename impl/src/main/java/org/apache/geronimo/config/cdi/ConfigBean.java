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
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.config.Config;

public class ConfigBean implements Bean<Config>, PassivationCapable {
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;
    private final String id;
    private Config config;

    ConfigBean() {
        this.qualifiers = new HashSet<>(asList(DefaultLiteral.INSTANCE, AnyLiteral.INSTANCE));
        this.types = new HashSet<>(asList(Config.class, Serializable.class));
        this.id = ConfigBean.class.getName() + "[" + Config.class.getName() + "]";
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
        return Config.class;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Config create(final CreationalContext<Config> context) {
        return config;
    }

    @Override
    public void destroy(final Config instance, final CreationalContext<Config> context) {
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
