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
	private static final String PROPERTY_FILE_KEY = "org.apache.geronimo.config.test.internal.filekey";

    @Deployment
    public static WebArchive deploy() {
        System.setProperty(SOME_KEY, "someval");
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "configProviderTest.jar")
                .addClasses(ProviderTest.class, SomeBean.class, PropertyFileBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap
                .create(WebArchive.class, "providerTest.war")
                .addAsLibrary(testJar);
    }

    private @Inject SomeBean someBean;
	private @Inject PropertyFileBean propertyFileBean;


    @Test
    public void testConfigProvider() {
        System.setProperty(SOME_KEY, "someval");
        String myconfig = someBean.getMyconfig();
        Assert.assertEquals(myconfig, "someval");

        System.setProperty(SOME_KEY, "otherval");
        myconfig = someBean.getMyconfig();
        Assert.assertEquals(myconfig, "otherval");
    }
	
	@Test
    public void testDefaultPropertySourceConfigProvider() {
        Assert.assertEquals(propertyFileBean.getMyfileconfig(), "fileval");
    }


    @RequestScoped
    public static class SomeBean {

        @Inject
        @ConfigProperty(name=SOME_KEY)
        private Provider<String> myconfig;

        public String getMyconfig() {
            return myconfig.get();
        }

    }
	
	@RequestScoped
	 public static class PropertyFileBean {
		
		@Inject
        @ConfigProperty(name=PROPERTY_FILE_KEY)
        private Provider<String> myfileconfig;
		
		public String getMyfileconfig() {
            return myfileconfig.get();
        }

    }
}
