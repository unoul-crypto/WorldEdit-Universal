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

import com.sk89q.worldedit.world.generation.TreeType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

/** Stable Bukkit fallback for tree types when no native adapter is available. */
final class BukkitTreeTypeSupport {

    private static final Map<String, org.bukkit.TreeType> BUKKIT_TYPES_BY_ID = new HashMap<>();

    private BukkitTreeTypeSupport() {
    }

    static synchronized void initialize() {
        for (org.bukkit.TreeType bukkitType : org.bukkit.TreeType.values()) {
            String enumId = "minecraft:" + bukkitType.name().toLowerCase(Locale.ROOT);
            String canonicalId = canonicalId(bukkitType, enumId);
            TreeType treeType = TreeType.REGISTRY.get(canonicalId);
            if (treeType == null) {
                treeType = TreeType.REGISTRY.register(canonicalId, new TreeType(canonicalId));
            }
            if (!enumId.equals(canonicalId) && TreeType.REGISTRY.get(enumId) == null) {
                TreeType.REGISTRY.register(enumId, treeType);
            }
            BUKKIT_TYPES_BY_ID.put(canonicalId, bukkitType);
            BUKKIT_TYPES_BY_ID.put(enumId, bukkitType);
        }
    }

    @Nullable
    static synchronized org.bukkit.TreeType resolve(TreeType treeType) {
        initialize();
        return BUKKIT_TYPES_BY_ID.get(treeType.id());
    }

    private static String canonicalId(org.bukkit.TreeType type, String fallback) {
        return switch (type.name()) {
            case "TREE" -> "minecraft:oak";
            case "BIG_TREE" -> "minecraft:fancy_oak";
            case "REDWOOD" -> "minecraft:spruce";
            case "TALL_REDWOOD" -> "minecraft:tall_spruce";
            case "MEGA_REDWOOD" -> "minecraft:mega_spruce";
            default -> fallback;
        };
    }
}
