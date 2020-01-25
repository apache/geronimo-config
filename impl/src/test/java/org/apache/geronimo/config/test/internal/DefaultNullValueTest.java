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
        assertNull(injected.booleanNullValue);
        assertNull(injected.stringNullValue);
        assertNull(injected.byteNullValue);
        assertNull(injected.integerNullValue);
        assertNull(injected.longNullValue);
        assertNull(injected.shortNullValue);
        assertNull(injected.listNullValue);
        assertNull(injected.classNullValue);
        assertNull(injected.doubleNullValue);
        assertNull(injected.durationNullValue);

        assertFalse(injected.primitiveBooleanNullValue);
        assertEquals(0, injected.primitiveLongNullValue);
        assertEquals(0, injected.primitiveIntegerNullValue);
        assertEquals(0, injected.primitiveShortNullValue);
        assertEquals(0, injected.primitiveByteNullValue);
        assertEquals(0.0F, injected.primitiveFloatNullValue);
        assertEquals(0.0D, injected.primitiveDoubleNullValue);
        assertEquals('\u0000', injected.primitiveCharacterNullValue);

    }

    @ApplicationScoped
    public static class Injected {

        @Inject
        @ConfigProperty(name = "boolean.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Boolean booleanNullValue;

        @Inject
        @ConfigProperty(name = "boolean.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private boolean primitiveBooleanNullValue;

        @Inject
        @ConfigProperty(name = "string.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private String stringNullValue;

        @Inject
        @ConfigProperty(name = "long.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Long longNullValue;

        @Inject
        @ConfigProperty(name = "long.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private long primitiveLongNullValue;

        @Inject
        @ConfigProperty(name = "integer.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Integer integerNullValue;

        @Inject
        @ConfigProperty(name = "integer.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private int primitiveIntegerNullValue;

        @Inject
        @ConfigProperty(name = "float.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Float floatNullValue;

        @Inject
        @ConfigProperty(name = "float.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private float primitiveFloatNullValue;

        @Inject
        @ConfigProperty(name = "double.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Double doubleNullValue;

        @Inject
        @ConfigProperty(name = "double.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private double primitiveDoubleNullValue;

        @Inject
        @ConfigProperty(name = "character.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Character characterNullValue;

        @Inject
        @ConfigProperty(name = "character.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private char primitiveCharacterNullValue;

        @Inject
        @ConfigProperty(name = "short.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Short shortNullValue;

        @Inject
        @ConfigProperty(name = "short.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private short primitiveShortNullValue;

        @Inject
        @ConfigProperty(name = "byte.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Byte byteNullValue;

        @Inject
        @ConfigProperty(name = "byte.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private byte primitiveByteNullValue;

        @Inject
        @ConfigProperty(name = "list.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private List<String> listNullValue;

        @Inject
        @ConfigProperty(name = "class.nullvalue.default", defaultValue =  ConfigProperty.NULL_VALUE)
        private Class classNullValue;

        @Inject
        @ConfigProperty(name = "url.nullvalue.default", defaultValue =  ConfigProperty.NULL_VALUE)
        private URL urlNullValue;

        @Inject
        @ConfigProperty(name = "duration.nullvalue.default", defaultValue = ConfigProperty.NULL_VALUE)
        private Duration durationNullValue;
    }
}
