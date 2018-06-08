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

import javax.config.Config;
import javax.config.ConfigProvider;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * This needs to have some ENV settings set up.
 * This is usually done via maven.
 * For running the test yourself you have to set the following environment properties:
 *
 * A_b_c=1
 * A_B_C=2
 * A_B_D=3
 * A_B_e=4
 */
public class SystemEnvConfigSourceTest {
    @Test
    public void testEnvReplacement() {
        Config config = ConfigProvider.getConfig();

        Assert.assertEquals(config.getValue("A.b#c", Integer.class), Integer.valueOf(1));

        Assert.assertEquals(config.getValue("a.b.c", Integer.class), Integer.valueOf(2));

        Assert.assertEquals(config.getValue("a.b.d", Integer.class), Integer.valueOf(3));

        Assert.assertEquals(config.getValue("a.b.e", Integer.class), Integer.valueOf(4));
    }
}
