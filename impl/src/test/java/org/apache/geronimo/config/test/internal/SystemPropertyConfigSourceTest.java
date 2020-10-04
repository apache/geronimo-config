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

import static org.testng.Assert.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * It is important to ensure the config is reloaded before going live in case some
 * system properties are set during the starting extension lifecycle.
 */
public class SystemPropertyConfigSourceTest extends Arquillian {
    @Deployment
    public static Archive<?> archive() {
        return ShrinkWrap.create(WebArchive.class, SystemPropertyConfigSourceTest.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "classes/META-INF/beans.xml")
                .addAsServiceProvider(Extension.class, InitInExtension.class)
                .addClasses(SystemPropertyConfigSourceTest.class, Injected.class);
    }

    @Inject
    private Injected injected;

    @Test
    public void testSystemPropsLoadedExtensionValue() {
        assertTrue(injected.getSet());
    }

    @ApplicationScoped
    public static class Injected {
        @Inject
        @ConfigProperty(name = "org.apache.geronimo.config.test.internal.SystemPropertyConfigSourceTest$InitInExtension")
        private Boolean set;

        public Boolean getSet() {
            return set;
        }
    }

    public static class InitInExtension implements Extension {
        private String originalCopy;

        void eagerInit(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
            originalCopy = System.getProperty("org.apache.geronimo.config.configsource.SystemPropertyConfigSource.copy");
            // enfore the default, it is overriden for surefire
            System.setProperty("org.apache.geronimo.config.configsource.SystemPropertyConfigSource.copy", "true");

            // eager load -> loads system props and copy
            ConfigProvider.getConfig();
        }

        // before validation to ensure config validation passes
        void afterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
            // with copy this should get ignored but we will reload it before the validation
            System.setProperty(InitInExtension.class.getName(), "true");
        }

        void beforeShutdown(@Observes final BeforeShutdown beforeShutdown) {
            System.clearProperty(InitInExtension.class.getName());
            if (originalCopy != null) {
                System.setProperty("org.apache.geronimo.config.configsource.SystemPropertyConfigSource.copy", originalCopy);
            } else {
                System.clearProperty("org.apache.geronimo.config.configsource.SystemPropertyConfigSource.copy");
            }
        }
    }
}
