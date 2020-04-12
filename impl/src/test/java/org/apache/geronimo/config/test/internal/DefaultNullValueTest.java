/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.config.test.internal;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertNull;

/**
 * @author <a href="mailto:danielsoro@apache.org">Daniel 'soro' Cunha</a>
 */
public class DefaultNullValueTest extends Arquillian {
    @Deployment
    public static Archive<?> archive() {
        return ShrinkWrap.create(WebArchive.class, DefaultNullValueTest.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "classes/META-INF/beans.xml")
                .addClasses(DefaultNullValueTest.class, DefaultNullValueTest.Injected.class);
    }

    @Inject
    private Injected injected;

    @Test
    public void testDefaultNullValue() {
        assertNull(injected.getBooleanNullValue());
        assertNull(injected.getStringNullValue());
        assertNull(injected.getByteNullValue());
        assertNull(injected.getIntegerNullValue());
        assertNull(injected.getLongNullValue());
        assertNull(injected.getShortNullValue());
        assertNull(injected.getListNullValue());
        assertNull(injected.getClassNullValue());
        assertNull(injected.getDoubleNullValue());
        assertNull(injected.getDurationNullValue());
    }

    @ApplicationScoped
    public static class Injected {

        @Inject
        @ConfigProperty(name = "boolean.nullvalue.default")
        private Optional<Boolean> booleanNullValue;

        @Inject
        @ConfigProperty(name = "string.nullvalue.default")
        private Optional<String> stringNullValue;

        @Inject
        @ConfigProperty(name = "long.nullvalue.default")
        private Optional<Long> longNullValue;

        @Inject
        @ConfigProperty(name = "integer.nullvalue.default")
        private Optional<Integer> integerNullValue;

        @Inject
        @ConfigProperty(name = "float.nullvalue.default")
        private Optional<Float> floatNullValue;

        @Inject
        @ConfigProperty(name = "double.nullvalue.default")
        private Optional<Double> doubleNullValue;

        @Inject
        @ConfigProperty(name = "character.nullvalue.default")
        private Optional<Character> characterNullValue;

        @Inject
        @ConfigProperty(name = "short.nullvalue.default")
        private Optional<Short> shortNullValue;

        @Inject
        @ConfigProperty(name = "byte.nullvalue.default")
        private Optional<Byte> byteNullValue;

        @Inject
        @ConfigProperty(name = "list.nullvalue.default")
        private Optional<List<String>> listNullValue;

        @Inject
        @ConfigProperty(name = "class.nullvalue.default")
        private Optional<Class> classNullValue;

        @Inject
        @ConfigProperty(name = "url.nullvalue.default")
        private Optional<URL> urlNullValue;

        @Inject
        @ConfigProperty(name = "duration.nullvalue.default")
        private Optional<Duration> durationNullValue;

        public Boolean getBooleanNullValue() {
            return booleanNullValue.orElse(null);
        }

        public String getStringNullValue() {
            return stringNullValue.orElse(null);
        }

        public Long getLongNullValue() {
            return longNullValue.orElse(null);
        }

        public Integer getIntegerNullValue() {
            return integerNullValue.orElse(null);
        }

        public Float getFloatNullValue() {
            return floatNullValue.orElse(null);
        }

        public Double getDoubleNullValue() {
            return doubleNullValue.orElse(null);
        }

        public Character getCharacterNullValue() {
            return characterNullValue.orElse(null);
        }

        public Short getShortNullValue() {
            return shortNullValue.orElse(null);
        }

        public Byte getByteNullValue() {
            return byteNullValue.orElse(null);
        }

        public List<String> getListNullValue() {
            return listNullValue.orElse(null);
        }

        public Class getClassNullValue() {
            return classNullValue.orElse(null);
        }

        public URL getUrlNullValue() {
            return urlNullValue.orElse(null);
        }

        public Duration getDurationNullValue() {
            return durationNullValue.orElse(null);
        }
    }
}
