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

import javax.config.ConfigSnapshot;
import javax.config.ConfigAccessor;
import javax.config.spi.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.inject.Typed;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
@Typed
public class ConfigValueImpl<T> implements ConfigAccessor<T> {
    private static final Logger logger = Logger.getLogger(ConfigValueImpl.class.getName());

    private final ConfigImpl config;

    private String keyOriginal;

    private String keyResolved;

    private Class<?> configEntryType = String.class;

    private List<Object> lookupChain;

    private boolean evaluateVariables = true;

    private long cacheTimeNs = -1;
    private volatile long reloadAfter = -1;
    private long lastReloadedAt = -1;

    private T lastValue = null;
    //X will later get added again private ConfigChanged valueChangeListener;
    private boolean isList;
    private boolean isSet;

    private T defaultValue;
    private boolean withDefault;

    /**
     * Alternative Converter to be used instead of the default converter
     */
    private Converter<T> converter;

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
    public ConfigAccessor<List<T>> asList() {
        isList = true;
        ConfigAccessor<List<T>> listTypedResolver = (ConfigAccessor<List<T>>) this;

        if (defaultValue == null)
        {
            // the default for lists is an empty list instead of null
            return listTypedResolver.withDefault(Collections.<T>emptyList());
        }

        return listTypedResolver;
    }

    @Override
    public ConfigAccessor<Set<T>> asSet() {
        isSet = true;
        ConfigAccessor<Set<T>> listTypedResolver = (ConfigAccessor<Set<T>>) this;

        if (defaultValue == null)
        {
            // the default for lists is an empty list instead of null
            return listTypedResolver.withDefault(Collections.<T>emptySet());
        }

        return listTypedResolver;
    }

    @Override
    public ConfigAccessor<T> withDefault(T value) {
        defaultValue = value;
        withDefault = true;
        return this;
    }

    @Override
    public ConfigAccessor<T> withStringDefault(String value) {
        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Empty String or null supplied as string-default value for property "
                    + keyOriginal);
        }
        value = replaceVariables(value);

        if (isList) {
            defaultValue = splitAndConvertListValue(value);
        }
        else {
            defaultValue = convert(value);
        }
        withDefault = true;
        return this;
    }

    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    @Override
    public ConfigAccessor<T> useConverter(Converter<T> converter) {
        this.converter = converter;
        return this;
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

    public ConfigValueImpl<T> addLookupSuffix(String suffixValue) {
        if (lookupChain == null) {
            lookupChain = new ArrayList<>();
        }
        this.lookupChain.add(suffixValue);
        return this;
    }

    public ConfigValueImpl<T> addLookupSuffix(ConfigAccessor<String> suffixAccessor) {
        if (lookupChain == null) {
            lookupChain = new ArrayList<>();
        }
        this.lookupChain.add(suffixAccessor);
        return this;
    }

    @Override
    public Optional<T> getOptionalValue() {
        return Optional.ofNullable(get());
    }

    //X will later get added again @Override
    /*X
    public ConfigValueImpl<T> onChange(ConfigChanged valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
        return this;
    }
    */

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

    @Override
    public T getValue(ConfigSnapshot configSnapshot) {
        ConfigSnapshotImpl snapshotImpl = (ConfigSnapshotImpl) configSnapshot;

        if (!snapshotImpl.getConfigValues().containsKey(this))
        {
            throw new IllegalArgumentException("The TypedResolver for key " + getPropertyName() +
                    " does not belong the given ConfigSnapshot!");
        }

        return (T) snapshotImpl.getConfigValues().get(this);
    }



    private T get(boolean convert) {
        long now = -1;
        if (cacheTimeNs > 0)
        {
            now = System.nanoTime();
            if (now <= reloadAfter)
            {
                // now check if anything in the underlying Config got changed
                long lastCfgChange = config.getLastChanged();
                if (lastCfgChange < lastReloadedAt)
                {
                    return lastValue;
                }
            }
        }

        String valueStr = resolveStringValue();

        if ((valueStr == null || valueStr.isEmpty()) && withDefault) {
            return defaultValue;
        }

        T value;
        if (isList || isSet) {
            value = splitAndConvertListValue(valueStr);
            if (isSet) {
                value = (T) new HashSet((List) value);
            }
        }
        else {
            value = convert ? convert(valueStr) : (T) valueStr;
        }

        //X will later get added again
        /*X
        if (valueChangeListener != null && (value != null && !value.equals(lastValue) || (value == null && lastValue != null)) )
        {
            valueChangeListener.onValueChange(keyOriginal, lastValue, value);
        }
        */

        lastValue = value;

        if (cacheTimeNs > 0)
        {
            reloadAfter = now + cacheTimeNs;
            lastReloadedAt = now;
        }

        return value;
    }

    private String resolveStringValue() {
        String value = null;

        if (lookupChain != null) {
            // first we resolve the value
            List<String> suffixVals = new ArrayList<>();
            for (Object suffix : lookupChain) {
                if (suffix instanceof ConfigAccessor) {
                    suffixVals.add(((ConfigAccessor<String>) suffix).getValue());
                }
                else {
                    suffixVals.add((String) suffix);
                }
            }

            // binary count down
            for (int mask = (1 << suffixVals.size()) - 1; mask > 0; mask--) {
                StringBuilder sb = new StringBuilder(keyOriginal);
                for (int loc = 0; loc < suffixVals.size(); loc++) {
                    int bitPos = 1 << (suffixVals.size() - loc - 1);
                    if ((mask & bitPos) > 0) {
                        sb.append('.').append(suffixVals.get(loc));
                    }
                }

                value = config.getValue(sb.toString());
                if (value != null && value.length() > 0) {
                    keyResolved = sb.toString();
                    break;
                }
            }

        }

        if (value == null) {
            value = config.getValue(keyOriginal);
            this.keyResolved = keyOriginal;
        }

        if (evaluateVariables && value != null)
        {
            value = replaceVariables(value);

        }
        return value;
    }

    private String replaceVariables(String value)
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
        return value;
    }

    //X @Override
    public String getPropertyName() {
        return keyOriginal;
    }

    //X @Override
    public String getResolvedPropertyName() {
        return keyResolved;
    }

    private T convert(String value) {
        if (converter != null) {
            return converter.convert(value);
        }

        if (String.class == configEntryType) {
            return (T) value;
        }

        return (T) config.convert(value, configEntryType);
    }


    private T splitAndConvertListValue(String valueStr) {
        if (valueStr == null) {
            return null;
        }

        List list = new ArrayList();
        StringBuilder currentValue = new StringBuilder();
        int length = valueStr.length();
        for (int i = 0; i < length; i++) {
            char c = valueStr.charAt(i);
            if (c == '\\') {
                if (i < length - 1) {
                    char nextC = valueStr.charAt(i + 1);
                    currentValue.append(nextC);
                    i++;
                }
            }
            else if (c == ',') {
                String trimedVal = currentValue.toString().trim();
                if (trimedVal.length() > 0) {
                    list.add(convert(trimedVal));
                }

                currentValue.setLength(0);
            }
            else {
                currentValue.append(c);
            }
        }

        String trimedVal = currentValue.toString().trim();
        if (trimedVal.length() > 0) {
            list.add(convert(trimedVal));
        }

        return (T) list;
    }

}
