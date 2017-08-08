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

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

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

    /**
     * only access via {@link #getConfig(}
     */
    private Config _config;

    public ConfigInjectionBean(BeanManager bm, Type type) {
        this.bm = bm;

        types = new HashSet<>();
        types.add(type);
        rawType = getRawType(type);
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
        return Collections.EMPTY_SET;
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
        Set<Bean<?>> beans = bm.getBeans(InjectionPoint.class);
        Bean<?> bean = bm.resolve(beans);
        InjectionPoint ip = (InjectionPoint) bm.getReference(bean, InjectionPoint.class,  context);
        if (ip == null) {
            throw new IllegalStateException("Could not retrieve InjectionPoint");
        }
        Annotated annotated = ip.getAnnotated();
        ConfigProperty configProperty = annotated.getAnnotation(ConfigProperty.class);
        String key = getConfigKey(ip, configProperty);
        String defaultValue = configProperty.defaultValue();

        if (annotated.getBaseType() instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) annotated.getBaseType();
            Type rawType = paramType.getRawType();

            // handle Provider<T>
            if (rawType instanceof Class && ((Class) rawType).isAssignableFrom(Provider.class) && paramType.getActualTypeArguments().length == 1) {
                Class clazz = (Class) paramType.getActualTypeArguments()[0]; //X TODO check type again, etc
                return (T) new ConfigValueProvider(getConfig(), key, clazz);
            }

            // handle Optional<T>
            if (rawType instanceof Class && ((Class) rawType).isAssignableFrom(Optional.class) && paramType.getActualTypeArguments().length == 1) {
                Class clazz = (Class) paramType.getActualTypeArguments()[0]; //X TODO check type again, etc
                return (T) getConfig().getOptionalValue(key, clazz);
            }
        }
        else {
            Class clazz = (Class) annotated.getBaseType();
            if (ConfigExtension.isDefaultUnset(defaultValue)) {
                return (T) getConfig().getValue(key, clazz);
            }
            else {
                Config config = getConfig();
                return (T) config.getOptionalValue(key, clazz)
                        .orElse(((ConfigImpl) config).convert(defaultValue, clazz));
            }
        }

        throw new IllegalStateException("unhandled ConfigProperty");
    }


    /**
     * Get the property key to use.
     * In case the {@link ConfigProperty#name()} is empty we will try to determine the key name from the InjectionPoint.
     */
    public static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (key.length() > 0) {
            return key;
        }
        if (ip.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember member = (AnnotatedMember) ip.getAnnotated();
            AnnotatedType declaringType = member.getDeclaringType();
            if (declaringType != null) {
                String[] parts = declaringType.getJavaClass().getCanonicalName().split("\\.");
                String cn = parts[parts.length-1];
                parts[parts.length-1] = Character.toLowerCase(cn.charAt(0)) + (cn.length() > 1 ? cn.substring(1) : "");
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

    public Config getConfig() {
        if (_config == null) {
            _config = ConfigProvider.getConfig();
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
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isAlternative() {
        return true;
    }

    @Override
    public String getId() {
        return "ConfigInjectionBean_" + rawType.getName();
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

    /**
     * A special Provider&lt;T&gt;
     * This concrete class is needed because we need the injected Provider for the ConfigProperty
     * to be Serializable. A Lambda would not work in this case
     */
    public static class ConfigValueProvider<T> implements Provider<T>, Serializable {
        private transient Config config;
        private final String key;
        private final Class<T> type;

        ConfigValueProvider(Config config, String key, Class<T> type) {
            this.config = config;
            this.key = key;
            this.type = type;
        }

        @Override
        public T get() {
            return (T) config.getValue(key, type);
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            config = ConfigProviderResolver.instance().getConfig();
        }

    }
}
