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
package org.apache.geronimo.config.converters;

import org.eclipse.microprofile.config.spi.Converter;

import javax.annotation.Priority;
import javax.enterprise.inject.Vetoed;

@Priority(1)
@Vetoed
public class ClassConverter implements Converter<Class>{
    public static final Converter<Class> INSTANCE = new ClassConverter();
    @Override
    public Class convert(String value) {
        if(value == null) {
            return null;
        }
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                return Class.forName(value);
            }
            return Class.forName(value, true, loader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
