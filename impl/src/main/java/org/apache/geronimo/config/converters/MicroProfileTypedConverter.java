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

public class MicroProfileTypedConverter<T> {
    private final Converter<T> delegate;
    private final int priority;

    public MicroProfileTypedConverter(Converter<T> delegate) {
        this(delegate, readPriority(delegate));
    }

    public MicroProfileTypedConverter(Converter<T> delegate, int priority) {
        this.delegate = delegate;
        this.priority = priority;
    }

    public Converter<T> getDelegate() {
        return delegate;
    }

    public int getPriority() {
        return priority;
    }

    private static <T> int readPriority(Converter<T> delegate) {
        Priority priority = delegate.getClass().getAnnotation(Priority.class);
        if(priority != null) {
            return priority.value();
        } else {
            return 100;
        }
    }

    public T convert(String value) {
        return delegate.convert(value);
    }
}
