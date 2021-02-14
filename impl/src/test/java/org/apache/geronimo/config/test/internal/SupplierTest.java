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

import org.apache.geronimo.config.test.testng.SystemPropertiesLeakProtector;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.function.Supplier;

public class SupplierTest extends Arquillian{
    private static final String SOME_KEY = "org.apache.geronimo.config.test.internal.somekey";
    private static final String SUPPLIER_DEFAULT_VALUE = "supplierDefaultValue";
    private static final String SOME_INT_KEY = "some.supplier.int.key";

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "configSupplierTest.jar")
                .addClasses(SupplierTest.class, SomeBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(
                        new StringAsset(SOME_KEY + "=someval\n"),
                        "microprofile-config.properties")
                .as(JavaArchive.class);

        return ShrinkWrap
                .create(WebArchive.class, "supplierTest.war")
                .addAsLibrary(testJar);
    }

    private @Inject SomeBean someBean;


    @Test
    public void testConfigProvider() {
        final SystemPropertiesLeakProtector fixer = new SystemPropertiesLeakProtector(); // lazy way to reset all the system props manipulated by this test
        fixer.onStart(null);

        String someval = "someval";
        System.setProperty(SOME_KEY, someval);
        String myconfig = someBean.getMyconfig();
        Assert.assertEquals(myconfig, someval);

        String otherval = "otherval";
        System.setProperty(SOME_KEY, otherval);
        myconfig = someBean.getMyconfig();
        Assert.assertEquals(myconfig, otherval);

        Assert.assertEquals(someBean.getAnotherconfig().get(), SUPPLIER_DEFAULT_VALUE);

        System.setProperty(SOME_INT_KEY, "42");
        Assert.assertEquals(someBean.getSomeInt(), 42);

        Assert.assertNull(someBean.getUndefinedValue().get());
        fixer.onFinish(null);
    }

    @RequestScoped
    public static class SomeBean {

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private Supplier<String> myconfig;

        @Inject
        @ConfigProperty(name = SOME_INT_KEY)
        private Supplier<Integer> someIntValue;

        @Inject
        @ConfigProperty(name="missing.key", defaultValue = SUPPLIER_DEFAULT_VALUE)
        private Supplier<String> anotherconfig;

        @Inject
        @ConfigProperty(name = "UNDEFINED_VALUE")
        private Supplier<Integer> undefinedValue;

        public int getSomeInt() {
            return someIntValue.get();
        }

        public String getMyconfig() {
            return myconfig.get();
        }

        public Supplier<String> getAnotherconfig() {
            return anotherconfig;
        }

        public Supplier<Integer> getUndefinedValue() {
            return undefinedValue;
        }
    }
}
