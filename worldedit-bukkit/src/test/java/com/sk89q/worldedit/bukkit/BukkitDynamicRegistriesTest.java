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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BukkitDynamicRegistriesTest {

    @Test
    void bridgesDynamicallyDiscoveredMaterialBackToItsBlockType() {
        Material moddedMaterial = mock(Material.class);
        BlockType moddedBlockType = mock(BlockType.class);

        when(moddedMaterial.getKey()).thenReturn(new NamespacedKey("worldedit_test", "machine"));
        when(moddedBlockType.id()).thenReturn("worldedit_test:machine");

        BukkitDynamicRegistries.remember(moddedMaterial);

        assertSame(moddedMaterial, BukkitAdapter.adapt(moddedBlockType));
    }
}
