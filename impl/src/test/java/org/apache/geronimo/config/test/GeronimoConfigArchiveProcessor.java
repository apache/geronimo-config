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
package org.apache.geronimo.config.test;

import org.apache.geronimo.config.ConfigImpl;
import org.apache.geronimo.config.DefaultConfigProvider;
import org.apache.geronimo.config.cdi.ConfigInjectionProducer;
import org.apache.geronimo.config.configsource.BaseConfigSource;
import org.apache.geronimo.config.converters.BooleanConverter;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Adds the whole Config implementation classes and resources to the
 * Arqillian deployment archive. This is needed to have the container
 * pick up the beans from within the impl for the TCK tests.
 *
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class GeronimoConfigArchiveProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (applicationArchive instanceof WebArchive) {
            JavaArchive configJar = ShrinkWrap
                    .create(JavaArchive.class, "geronimo-config-impl.jar")
                    .addPackage(ConfigImpl.class.getPackage())
                    .addPackage(BooleanConverter.class.getPackage())
                    .addPackage(BaseConfigSource.class.getPackage())
                    .addPackage(ConfigInjectionProducer.class.getPackage())
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsServiceProvider(ConfigProviderResolver.class, DefaultConfigProvider.class);
            ((WebArchive) applicationArchive).addAsLibraries(configJar);
        }
    }
}
