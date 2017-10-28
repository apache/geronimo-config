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
package org.apache.geronimo.config.converters;

import javax.config.spi.Converter;

import javax.annotation.Priority;
import javax.enterprise.inject.Vetoed;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * @author <a href="mailto:johndament@apache.org">John D. Ament</a>
 */
@Priority(1)
@Vetoed
public class InstantConverter implements Converter<Instant> {

    public static final InstantConverter INSTANCE = new InstantConverter();

    @Override
    public Instant convert(String value) {
        if (value != null) {
            try {
                return Instant.parse(value);
            }
            catch (DateTimeParseException dtpe) {
                throw new IllegalArgumentException(dtpe);
            }
        }
        return null;
    }
}
