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
package org.apache.geronimo.config.test.internal;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ProviderTest extends Arquillian {
    private static final String SOME_KEY = "org.apache.geronimo.config.test.internal.somekey";

    @Deployment
    public static WebArchive deploy() {
        System.setProperty(SOME_KEY, "someval");
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "configProviderTest.jar")
                .addClasses(ProviderTest.class, SomeBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap
                .create(WebArchive.class, "providerTest.war")
                .addAsLibrary(testJar);
    }

    private @Inject SomeBean someBean;


    @Test
    public void testConfigProvider() {
        System.setProperty(SOME_KEY, "someval");
        String myconfig = someBean.getMyconfig();
        Assert.assertEquals(myconfig, "someval");
        Assert.assertEquals(someBean.getOptionalProvider().get().get(), "someval");
        Assert.assertEquals(someBean.getProviderOptional().get().get(), "someval");

        System.setProperty(SOME_KEY, "otherval");
        myconfig = someBean.getMyconfig();
        Assert.assertEquals(myconfig, "otherval");
    }


    @RequestScoped
    public static class SomeBean {

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private Provider<String> myconfig;

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private Optional<Provider<String>> optionalProvider;

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private Provider<Optional<String>> providerOptional;

        public Optional<Provider<String>> getOptionalProvider() {
            return optionalProvider;
        }

        public Provider<Optional<String>> getProviderOptional() {
            return providerOptional;
        }

        public String getMyconfig() {
            return myconfig.get();
        }

    }
}
