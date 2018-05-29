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
package org.apache.geronimo.config;

import org.eclipse.microprofile.config.Config;

public final class Placeholders {
    private Placeholders() {
        // no-op
    }

    public static String replace(final Config config, final String originalValue) {
        String value = originalValue;
        // recursively resolve any ${varName} in the value
        int startVar = 0;
        while ((startVar = value.indexOf("${", startVar)) >= 0)
        {
            int endVar = value.indexOf("}", startVar);
            if (endVar < startVar)
            {
                return value;
            }
            int endStart = startVar;
            int nestedPlaceholder;
            do {
                nestedPlaceholder = value.indexOf("${", endStart + 1);
                if (nestedPlaceholder > 0 && nestedPlaceholder < endVar) { // then we grabbed the end of this placeholder
                    endVar = value.indexOf("}", nestedPlaceholder + 1); // end nested placeholder
                    if (endVar < startVar) {
                        return value;
                    }
                    endVar = value.indexOf("}", endVar + 1); // end of the enclosing placeholder
                    if (endVar < startVar) { // broken value pby
                        return value;
                    }
                    endStart = endVar;
                } else {
                    break;
                }
            } while (true);

            int defaultSep = value.indexOf(':', startVar);
            boolean hasDefault = defaultSep > 0 && defaultSep < endVar;
            String varName = value.substring(startVar + 2, hasDefault ? defaultSep : endVar);
            if (varName.isEmpty())
            {
                return value;
            }

            String variableValue = config.getOptionalValue(varName, String.class).orElse(null);
            if (variableValue == null && hasDefault) {
                variableValue = value.substring(defaultSep + 1, endVar);
            }

            if (variableValue != null)
            {
                final String old = value;
                value = value.substring(0, startVar) + variableValue + value.substring(endVar + 1);
                if (value.equals(old)) { // avoid loops
                    return value;
                }
            }
        }
        return value;
    }
}
