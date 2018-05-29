/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.geronimo.config.test.internal;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

public class ArrayTypeTest extends Arquillian {
    private static final String SOME_KEY = "org.apache.geronimo.config.test.internal.somekey";
    private static final String SOME_OTHER_KEY = "org.apache.geronimo.config.test.internal.someotherkey";

    @Deployment
    public static WebArchive deploy() {
        System.setProperty(SOME_KEY, "1,2,3");
        System.setProperty(SOME_OTHER_KEY, "1,2\\,3");
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "arrayTest.jar")
                .addClasses(ArrayTypeTest.class, SomeBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap
                .create(WebArchive.class, "arrayTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    private SomeBean someBean;

    @Test
    public void testArraySetListInjection() {
        Assert.assertEquals(someBean.getStringValue(), "1,2,3");
        Assert.assertEquals(someBean.getMyconfig(), new int[]{1,2,3});
        Assert.assertEquals(someBean.getIntValues(), asList(1,2,3));
        Assert.assertEquals(someBean.getIntSet(), new LinkedHashSet<>(asList(1,2,3)));
        Assert.assertEquals(someBean.getIntSetDefault(), new LinkedHashSet<>(asList(1,2,3)));
    }

    @Test
    public void testListWithEscaping() {
        Assert.assertEquals(someBean.getValues(), asList("1","2,3"));
    }

    @RequestScoped
    public static class SomeBean {

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private int[] myconfig;

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private List<Integer> intValues;

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private Set<Integer> intSet;

        @Inject
        @ConfigProperty(name=SOME_KEY, defaultValue = "1,2,3")
        private Set<Integer> intSetDefault;

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private String stringValue;

        @Inject
        @ConfigProperty(name=SOME_OTHER_KEY)
        private List<String> values;

        public Set<Integer> getIntSetDefault() {
            return intSetDefault;
        }

        public String getStringValue() {
            return stringValue;
        }

        public int[] getMyconfig() {
            return myconfig;
        }

        public List<Integer> getIntValues() {
            return intValues;
        }

        public Set<Integer> getIntSet() {
            return intSet;
        }

        public List<String> getValues() {
            return values;
        }
    }
}
