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

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

import java.util.Collection;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

public class ProxyTest extends Arquillian {
    private static final String LIST_KEY = SomeProxy.class.getName() + ".list";
    private static final String SOME_KEY = SomeProxy.class.getName() + ".key";
    private static final String SOME_OTHER_KEY = SomeProxy.class.getName() + ".key2";

    @Deployment
    public static WebArchive deploy() {
        System.setProperty("prefix.val", "yes");
        System.setProperty(LIST_KEY, "a,b,1");
        System.setProperty(SOME_KEY, "yeah");
        System.setProperty(SOME_OTHER_KEY, "123");
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "PoxyTest.jar")
                .addClasses(ProxyTest.class, SomeProxy.class, PrefixedSomeProxy.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap
                .create(WebArchive.class, "providerTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    private PrefixedSomeProxy prefixed;

    @Inject
    private SomeProxy proxy;

    @Test
    public void test() {
        assertEquals(proxy.key(), "yeah");
        assertEquals(proxy.renamed(), "yeah");
        assertEquals(proxy.key2(), 123);
        assertEquals(proxy.key3(), "def");
        assertEquals(proxy.list(), asList("a", "b", "1"));
        assertEquals(proxy.listDefaults(), asList(1, 2, 1));
    }

    @Test
    public void prefix() {
        assertEquals(prefixed.val(), "yes");
    }

    @ConfigProperty(name = "prefix.")
    public interface PrefixedSomeProxy {
        @ConfigProperty(name = "val")
        String val();
    }

    public interface SomeProxy {
        @ConfigProperty
        int key2();

        @ConfigProperty(defaultValue = "def")
        String key3();

        @ConfigProperty
        String key();

        @ConfigProperty(name = "org.apache.geronimo.config.test.internal.ProxyTest$SomeProxy.key")
        String renamed();

        @ConfigProperty
        Collection<String> list();

        @ConfigProperty(defaultValue = "1,2,1")
        Collection<Integer> listDefaults();
    }
}
