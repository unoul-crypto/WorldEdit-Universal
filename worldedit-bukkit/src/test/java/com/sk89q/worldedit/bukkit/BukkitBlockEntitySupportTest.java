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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BukkitBlockEntitySupportTest {

    @Test
    void roundTripsInventoryContentsThroughClipboardNbt() throws ReflectiveOperationException {
        installMinimalBukkitServer();
        ItemStack storedItem = new ItemStack(Material.STONE, 7);
        Inventory sourceInventory = mock(Inventory.class);
        when(sourceInventory.getSize()).thenReturn(3);
        when(sourceInventory.getItem(0)).thenReturn(storedItem);

        Block sourceBlock = mockBlockWithInventory(sourceInventory);
        LinCompoundTag captured = BukkitBlockEntitySupport.captureInventory(sourceBlock);

        assertNotNull(captured);
        assertTrue(BukkitBlockEntitySupport.hasInventoryData(captured));

        Inventory targetInventory = mock(Inventory.class);
        when(targetInventory.getSize()).thenReturn(3);
        Block targetBlock = mockBlockWithInventory(targetInventory);

        assertTrue(BukkitBlockEntitySupport.restoreInventory(targetBlock, captured));
        verify(targetInventory).clear();
        verify(targetInventory).setItem(0, storedItem);
    }

    @SuppressWarnings("deprecation")
    private static void installMinimalBukkitServer() throws ReflectiveOperationException {
        UnsafeValues unsafeValues = mock(UnsafeValues.class);
        when(unsafeValues.getDataVersion()).thenReturn(0);
        when(unsafeValues.getMaterial(Mockito.anyString(), Mockito.anyInt()))
                .thenAnswer(invocation -> Material.matchMaterial(invocation.getArgument(0)));

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(itemFactory.equals(Mockito.nullable(org.bukkit.inventory.meta.ItemMeta.class),
                Mockito.nullable(org.bukkit.inventory.meta.ItemMeta.class))).thenReturn(true);

        Server server = mock(Server.class);
        when(server.getUnsafe()).thenReturn(unsafeValues);
        when(server.getItemFactory()).thenReturn(itemFactory);

        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, server);
    }

    private static Block mockBlockWithInventory(Inventory inventory) {
        BlockState state = mock(
                BlockState.class,
                Mockito.withSettings().extraInterfaces(InventoryHolder.class)
        );
        when(((InventoryHolder) state).getInventory()).thenReturn(inventory);

        BlockData blockData = mock(BlockData.class);
        when(blockData.getMaterial()).thenReturn(Material.CHEST);

        Block block = mock(Block.class);
        when(block.getState()).thenReturn(state);
        when(block.getBlockData()).thenReturn(blockData);
        return block;
    }
}
