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

import javax.config.ConfigValue;
import javax.config.spi.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.inject.Typed;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
@Typed
public class ConfigValueImpl<T> implements ConfigValue<T> {
    private static final Logger logger = Logger.getLogger(ConfigValueImpl.class.getName());

    private final ConfigImpl config;

    private String keyOriginal;

    private String keyResolved;

    private Class<?> configEntryType = String.class;

    private String[] lookupChain;

    private boolean evaluateVariables = false;

    private long cacheTimeNs = -1;
    private volatile long reloadAfter = -1;
    private T lastValue = null;
    private ConfigChanged valueChangeListener;

    public ConfigValueImpl(ConfigImpl config, String key) {
        this.config = config;
        this.keyOriginal = key;
    }

    @Override
    public <N> ConfigValueImpl<N> as(Class<N> clazz) {
        configEntryType = clazz;
        return (ConfigValueImpl<N>) this;
    }


    @Override
    public ConfigValueImpl<T> cacheFor(long value, TimeUnit timeUnit) {
        this.cacheTimeNs = timeUnit.toNanos(value);
        return this;
    }

    @Override
    public ConfigValueImpl<T> evaluateVariables(boolean evaluateVariables) {
        this.evaluateVariables = evaluateVariables;
        return this;
    }

    public ConfigValueImpl<T> withLookupChain(String... postfixNames) {
        this.lookupChain = postfixNames;
        return this;
    }

    @Override
    public Optional<T> getOptionalValue() {
        return Optional.ofNullable(get());
    }

    @Override
    public ConfigValueImpl<T> onChange(ConfigChanged valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
        return this;
    }

    

    //X @Override
    public List<T> getValueList() {
        String rawList = (String) get(false);
        List<T> values = new ArrayList<T>();
        StringBuilder sb = new StringBuilder(64);
        for (int i= 0; i < rawList.length(); i++) {
            char c = rawList.charAt(i);
            if ('\\' == c) {
                if (i == rawList.length()) {
                    throw new IllegalStateException("incorrect escaping of key " + keyOriginal + " value: " + rawList);
                }
                char nextChar = rawList.charAt(i+1);
                if (nextChar == '\\') {
                    sb.append('\\');
                }
                else if (nextChar == ',') {
                    sb.append(',');
                }
                i++;
            }
            else if (',' == c) {
                addListValue(values, sb);
            }
            else {
                sb.append(c);
            }
        }
        addListValue(values, sb);

        return values;
    }

    private void addListValue(List<T> values, StringBuilder sb) {
        String val = sb.toString().trim();
        if (!val.isEmpty()) {
            values.add(convert(val));
        }
        sb.setLength(0);
    }

    public T get() {
        return get(true);
    }

    @Override
    public T getValue() {
        T val = get();
        if (val == null) {
            throw new NoSuchElementException("No config value present for key " + keyOriginal);
        }
        return val;
    }

    private T get(boolean convert) {
        long now = -1;
        if (cacheTimeNs > 0)
        {
            now = System.nanoTime();
            if (now <= reloadAfter)
            {
                return lastValue;
            }
        }

        String valueStr = resolveStringValue();
        T value = convert ? convert(valueStr) : (T) valueStr;

        if (valueChangeListener != null && (value != null && !value.equals(lastValue) || (value == null && lastValue != null)) )
        {
            valueChangeListener.onValueChange(keyOriginal, lastValue, value);
        }

        lastValue = value;

        if (cacheTimeNs > 0)
        {
            reloadAfter = now + cacheTimeNs;
        }

        return value;
    }

    private String resolveStringValue() {
        //X TODO implement lookupChain

        String value = config.getValue(keyOriginal);
        if (evaluateVariables)
        {
            // recursively resolve any ${varName} in the value
            int startVar = 0;
            while ((startVar = value.indexOf("${", startVar)) >= 0)
            {
                int endVar = value.indexOf("}", startVar);
                if (endVar <= 0)
                {
                    break;
                }
                String varName = value.substring(startVar + 2, endVar);
                if (varName.isEmpty())
                {
                    break;
                }
                String variableValue = config.access(varName).evaluateVariables(true).get();
                if (variableValue != null)
                {
                    value = value.replace("${" + varName + "}", variableValue);
                }
                startVar++;
            }
        }
        return value;
    }

    //X @Override
    public String getKey() {
        return keyOriginal;
    }

    //X @Override
    public String getResolvedKey() {
        return keyResolved;
    }

    private T convert(String value) {
        if (String.class == configEntryType) {
            return (T) value;
        }

        Converter converter = config.getConverters().get(configEntryType);
        if (converter == null) {
            throw new IllegalStateException("No Converter for type " + configEntryType);
        }

        return (T) converter.convert(value);
    }

}
