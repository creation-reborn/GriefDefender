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
package com.griefdefender.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.claim.GDShovelType;
import com.griefdefender.util.RegistryHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ShovelTypeRegistryModule implements CatalogRegistryModule<ShovelType> {

    private static ShovelTypeRegistryModule instance;

    public static ShovelTypeRegistryModule getInstance() {
        return instance;
    }

    private final Map<String, ShovelType> shovelTypeMap = new HashMap<>();

    @Override
    public Optional<ShovelType> getById(String id) {
        return Optional.ofNullable(this.shovelTypeMap.get(checkNotNull(id)));
    }

    @Override
    public Collection<ShovelType> getAll() {
        return this.shovelTypeMap.values();
    }

    @Override
    public void registerDefaults() {
        RegistryHelper.mapFields(ShovelTypes.class, input -> {
            final ShovelType type = new GDShovelType("griefdefender:" + input, input);
            this.shovelTypeMap.put(input.toLowerCase(Locale.ENGLISH), type);
            return type;
        });
    }

    @Override
    public void registerCustomType(ShovelType type) {
        // TODO Auto-generated method stub
        
    }

    static {
        instance = new ShovelTypeRegistryModule();
    }
}
