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

import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

/** Generic Bukkit storage data bridge used when no native adapter is available. */
final class BukkitBlockEntitySupport {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final String INVENTORY_DATA_KEY = "worldedit:bukkit_inventory_yaml";
    private static final String INVENTORY_SIZE_PATH = "inventory-size";
    private static final String INVENTORY_ITEMS_PATH = "items";

    private BukkitBlockEntitySupport() {
    }

    @Nullable
    static LinCompoundTag captureInventory(Block block) {
        Inventory inventory = findInventory(block.getState());
        if (inventory == null) {
            return null;
        }

        try {
            YamlConfiguration data = new YamlConfiguration();
            data.set(INVENTORY_SIZE_PATH, inventory.getSize());
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item != null) {
                    data.set(INVENTORY_ITEMS_PATH + "." + slot, item);
                }
            }
            return LinCompoundTag.builder()
                    .putString("id", block.getBlockData().getMaterial().getKey().toString())
                    .putString(INVENTORY_DATA_KEY, data.saveToString())
                    .build();
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to serialize a Bukkit block inventory at " + block.getLocation(), e);
            return null;
        }
    }

    static boolean hasInventoryData(@Nullable LinCompoundTag nbt) {
        return nbt != null && nbt.findTag(INVENTORY_DATA_KEY, LinTagType.stringTag()) != null;
    }

    static boolean restoreInventory(Block block, @Nullable LinCompoundTag nbt) {
        if (nbt == null) {
            return false;
        }
        LinStringTag serialized = nbt.findTag(INVENTORY_DATA_KEY, LinTagType.stringTag());
        if (serialized == null) {
            return false;
        }

        Inventory inventory = findInventory(block.getState());
        if (inventory == null) {
            return false;
        }

        try {
            YamlConfiguration data = new YamlConfiguration();
            data.loadFromString(serialized.value());
            int sourceSize = Math.max(0, data.getInt(INVENTORY_SIZE_PATH));
            inventory.clear();
            for (int slot = 0; slot < Math.min(sourceSize, inventory.getSize()); slot++) {
                inventory.setItem(slot, data.getItemStack(INVENTORY_ITEMS_PATH + "." + slot));
            }
            return true;
        } catch (InvalidConfigurationException | RuntimeException e) {
            LOGGER.warn("Unable to restore a Bukkit block inventory at " + block.getLocation(), e);
            return false;
        }
    }

    static boolean clearInventory(Block block) {
        Inventory inventory = findInventory(block.getState());
        if (inventory == null) {
            return false;
        }
        inventory.clear();
        return true;
    }

    @Nullable
    private static Inventory findInventory(BlockState state) {
        if (state instanceof Chest chest) {
            return chest.getBlockInventory();
        }
        if (state instanceof InventoryHolder inventoryHolder) {
            return inventoryHolder.getInventory();
        }

        // Hybrid servers sometimes expose a public inventory method without implementing
        // Bukkit's InventoryHolder on their generated block-state wrapper.
        for (String methodName : new String[] { "getInventory", "getSnapshotInventory" }) {
            try {
                Method method = state.getClass().getMethod(methodName);
                if (!Inventory.class.isAssignableFrom(method.getReturnType()) || !method.trySetAccessible()) {
                    continue;
                }
                Object result = method.invoke(state);
                if (result instanceof Inventory inventory) {
                    return inventory;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                     | RuntimeException ignored) {
                // Try the next stable method name.
            }
        }
        return null;
    }
}
