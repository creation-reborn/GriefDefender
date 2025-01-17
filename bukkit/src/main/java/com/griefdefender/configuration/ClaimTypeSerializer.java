/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
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
 */
package com.griefdefender.configuration;

import com.google.common.reflect.TypeToken;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.registry.ClaimTypeRegistryModule;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class ClaimTypeSerializer implements TypeSerializer<ClaimType> {

    @Override
    public ClaimType deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
        ClaimType ret = ClaimTypeRegistryModule.getInstance().getById(node.getString().toLowerCase()).orElse(null);
        if (ret == null) {
            throw new ObjectMappingException("Input '" + node.getValue() + "' was not a valid value for type " + type);
        }
        return ret;
    }

    @Override
    public void serialize(TypeToken<?> type, ClaimType obj, ConfigurationNode node) throws ObjectMappingException {
       node.setValue(obj.getName());
    }

}
