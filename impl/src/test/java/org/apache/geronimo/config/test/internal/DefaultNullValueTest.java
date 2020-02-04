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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
        assertNull(injected.getFloatNullValue());
        assertNull(injected.getCharacterNullValue());
        assertNull(injected.getUrlNullValue());
        assertNull(injected.getDurationNullValue());

        assertFalse(injected.isPrimitiveBooleanNullValue());
        assertEquals(0, injected.getPrimitiveLongNullValue());
        assertEquals(0, injected.getPrimitiveIntegerNullValue());
        assertEquals(0, injected.getPrimitiveShortNullValue());
        assertEquals(0, injected.getPrimitiveByteNullValue());
        assertEquals(0.0F, injected.getPrimitiveFloatNullValue());
        assertEquals(0.0D, injected.getPrimitiveDoubleNullValue());
        assertEquals('\u0000', injected.getPrimitiveCharacterNullValue());

    }

    @ApplicationScoped
    public static class Injected {

        @Inject
        @ConfigProperty(name = "boolean.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Boolean booleanNullValue;

        @Inject
        @ConfigProperty(name = "boolean.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private boolean primitiveBooleanNullValue;

        @Inject
        @ConfigProperty(name = "string.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private String stringNullValue;

        @Inject
        @ConfigProperty(name = "long.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Long longNullValue;

        @Inject
        @ConfigProperty(name = "long.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private long primitiveLongNullValue;

        @Inject
        @ConfigProperty(name = "integer.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Integer integerNullValue;

        @Inject
        @ConfigProperty(name = "integer.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private int primitiveIntegerNullValue;

        @Inject
        @ConfigProperty(name = "float.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Float floatNullValue;

        @Inject
        @ConfigProperty(name = "float.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private float primitiveFloatNullValue;

        @Inject
        @ConfigProperty(name = "double.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Double doubleNullValue;

        @Inject
        @ConfigProperty(name = "double.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private double primitiveDoubleNullValue;

        @Inject
        @ConfigProperty(name = "character.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Character characterNullValue;

        @Inject
        @ConfigProperty(name = "character.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private char primitiveCharacterNullValue;

        @Inject
        @ConfigProperty(name = "short.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Short shortNullValue;

        @Inject
        @ConfigProperty(name = "short.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private short primitiveShortNullValue;

        @Inject
        @ConfigProperty(name = "byte.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Byte byteNullValue;

        @Inject
        @ConfigProperty(name = "byte.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private byte primitiveByteNullValue;

        @Inject
        @ConfigProperty(name = "list.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private List<String> listNullValue;

        @Inject
        @ConfigProperty(name = "class.nullvalue.default", defaultValue =  "org.apache.geronimo.config.nullvalue")
        private Class classNullValue;

        @Inject
        @ConfigProperty(name = "url.nullvalue.default", defaultValue =  "org.apache.geronimo.config.nullvalue")
        private URL urlNullValue;

        @Inject
        @ConfigProperty(name = "duration.nullvalue.default", defaultValue = "org.apache.geronimo.config.nullvalue")
        private Duration durationNullValue;

        public Boolean getBooleanNullValue() {
            return booleanNullValue;
        }

        public boolean isPrimitiveBooleanNullValue() {
            return primitiveBooleanNullValue;
        }

        public String getStringNullValue() {
            return stringNullValue;
        }

        public Long getLongNullValue() {
            return longNullValue;
        }

        public long getPrimitiveLongNullValue() {
            return primitiveLongNullValue;
        }

        public Integer getIntegerNullValue() {
            return integerNullValue;
        }

        public int getPrimitiveIntegerNullValue() {
            return primitiveIntegerNullValue;
        }

        public Float getFloatNullValue() {
            return floatNullValue;
        }

        public float getPrimitiveFloatNullValue() {
            return primitiveFloatNullValue;
        }

        public Double getDoubleNullValue() {
            return doubleNullValue;
        }

        public double getPrimitiveDoubleNullValue() {
            return primitiveDoubleNullValue;
        }

        public Character getCharacterNullValue() {
            return characterNullValue;
        }

        public char getPrimitiveCharacterNullValue() {
            return primitiveCharacterNullValue;
        }

        public Short getShortNullValue() {
            return shortNullValue;
        }

        public short getPrimitiveShortNullValue() {
            return primitiveShortNullValue;
        }

        public Byte getByteNullValue() {
            return byteNullValue;
        }

        public byte getPrimitiveByteNullValue() {
            return primitiveByteNullValue;
        }

        public List<String> getListNullValue() {
            return listNullValue;
        }

        public Class getClassNullValue() {
            return classNullValue;
        }

        public URL getUrlNullValue() {
            return urlNullValue;
        }

        public Duration getDurationNullValue() {
            return durationNullValue;
        }
    }
}
