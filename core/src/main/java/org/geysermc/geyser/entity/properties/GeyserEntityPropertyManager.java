/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.entity.properties;

import org.cloudburstmc.protocol.bedrock.data.entity.FloatEntityProperty;
import org.cloudburstmc.protocol.bedrock.data.entity.IntEntityProperty;
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;

import java.util.ArrayList;
import java.util.List;

public class GeyserEntityPropertyManager {

    private final GeyserEntityProperties properties;

    private final List<IntEntityProperty> intEntityProperties = new ArrayList<>();
    private final List<FloatEntityProperty> floatEntityProperties = new ArrayList<>();

    public GeyserEntityPropertyManager(GeyserEntityProperties properties) {
        this.properties = properties;
    }

    public void add(String propertyName, int value) {
        int index = properties.getPropertyIndex(propertyName);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid property name: " + propertyName);
        }

        PropertyType property = properties.getProperties().get(index);
        if (property instanceof IntProperty) {
            intEntityProperties.add(new IntEntityProperty(index, value));
        } else {
            throw new IllegalArgumentException("Property " + propertyName + " is not of type IntProperty.");
        }
    }

    public void add(String propertyName, boolean value) {
        int index = properties.getPropertyIndex(propertyName);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid property name: " + propertyName);
        }

        PropertyType property = properties.getProperties().get(index);
        if (property instanceof BooleanProperty) {
            intEntityProperties.add(new IntEntityProperty(index, value ? 1 : 0));
        } else {
            throw new IllegalArgumentException("Property " + propertyName + " is not of type BooleanProperty.");
        }
    }

    public void add(String propertyName, String value) {
        int index = properties.getPropertyIndex(propertyName);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid property name: " + propertyName);
        }

        PropertyType property = properties.getProperties().get(index);
        if (property instanceof EnumProperty) {
            int enumIndex = ((EnumProperty) property).getIndex(value);
            if (enumIndex == -1) {
                throw new IllegalArgumentException("Invalid enum value: " + value);
            }
            intEntityProperties.add(new IntEntityProperty(index, enumIndex));
        } else {
            throw new IllegalArgumentException("Property " + propertyName + " is not of type EnumProperty.");
        }
    }

    public void add(String propertyName, float value) {
        int index = properties.getPropertyIndex(propertyName);
        if (index == -1) {
            throw new IllegalArgumentException("Invalid property name: " + propertyName);
        }

        PropertyType property = properties.getProperties().get(index);
        if (property instanceof FloatProperty) {
            floatEntityProperties.add(new FloatEntityProperty(index, value));
        } else {
            throw new IllegalArgumentException("Provided value type doesn't match property type for: " + propertyName);
        }
    }

    public boolean hasFloatProperties() {
        return !this.floatEntityProperties.isEmpty();
    }

    public boolean hasIntProperties() {
        return !this.intEntityProperties.isEmpty();
    }

    public List<IntEntityProperty> intProperties() {
        return this.intEntityProperties;
    }

    public void applyIntProperties(List<IntEntityProperty> properties) {
        properties.addAll(intEntityProperties);
        intEntityProperties.clear();
    }

    public List<FloatEntityProperty> floatProperties() {
        return this.floatEntityProperties;
    }

    public void applyFloatProperties(List<FloatEntityProperty> properties) {
        properties.addAll(floatEntityProperties);
        floatEntityProperties.clear();
    }
}
