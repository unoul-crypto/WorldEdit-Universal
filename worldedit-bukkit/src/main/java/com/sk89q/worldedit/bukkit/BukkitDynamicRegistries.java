/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.data.BlockData;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * Central bridge for dynamic Bukkit registries.
 *
 * <p>Hybrid servers may return valid modded {@link Material} instances from {@link BlockData}
 * without listing them in {@link Registry#MATERIAL}. Every WorldEdit operation and third-party
 * integration must therefore use this bridge instead of querying that registry directly.</p>
 */
final class BukkitDynamicRegistries {

    private static final Map<String, Material> MATERIALS_BY_ID = new ConcurrentHashMap<>();

    private BukkitDynamicRegistries() {
    }

    static void remember(Material material) {
        MATERIALS_BY_ID.put(material.getKey().toString(), material);
    }

    @Nullable
    static Material resolveMaterial(String requestedId) {
        String id = requestedId.toLowerCase(Locale.ROOT);
        Material cached = MATERIALS_BY_ID.get(id);
        if (cached != null) {
            return cached;
        }

        Material material = null;
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key != null) {
            try {
                material = Registry.MATERIAL.get(key);
            } catch (IllegalArgumentException | LinkageError ignored) {
                // The hybrid registry may reject a valid Forge ID here.
            }
        }
        if (material == null) {
            try {
                material = Bukkit.createBlockData(id).getMaterial();
            } catch (IllegalArgumentException | LinkageError ignored) {
                // It may be an item-only material; try Bukkit's legacy-compatible lookup next.
            }
        }
        if (material == null) {
            material = Material.matchMaterial(id);
        }
        if (material != null) {
            remember(material);
        }
        return material;
    }

    @Nullable
    static BlockType resolveBlockType(String requestedId) {
        String id = requestedId.toLowerCase(Locale.ROOT);
        BlockType registered = BlockType.REGISTRY.get(id);
        if (registered != null) {
            return registered;
        }

        try {
            return resolveBlockType(Bukkit.createBlockData(id));
        } catch (IllegalArgumentException | LinkageError ignored) {
            Material material = resolveMaterial(id);
            if (material == null || !material.isBlock()) {
                return null;
            }
            return resolveBlockType(material.createBlockData());
        }
    }

    static BlockType resolveBlockType(BlockData blockData) {
        Material material = blockData.getMaterial();
        remember(material);
        String id = material.getKey().toString();
        BlockType registered = BlockType.REGISTRY.get(id);
        if (registered != null) {
            return registered;
        }

        synchronized (BlockType.REGISTRY) {
            registered = BlockType.REGISTRY.get(id);
            if (registered == null) {
                BlockData defaultData = material.createBlockData();
                registered = BlockType.REGISTRY.register(id, new BlockType(
                        id,
                        state -> BukkitBlockStateProperties.applyProperties(state, defaultData.getAsString())
                ));
            }
        }
        return registered;
    }
}
