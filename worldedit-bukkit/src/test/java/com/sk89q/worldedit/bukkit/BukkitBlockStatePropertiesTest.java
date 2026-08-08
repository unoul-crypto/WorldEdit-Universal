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

import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BukkitBlockStatePropertiesTest {

    @Test
    void parsesVanillaAndModdedStateStrings() {
        assertEquals(
                Map.of("snowy", "false"),
                BukkitBlockStateProperties.parseProperties("minecraft:grass_block[snowy=false]")
        );
        assertEquals(
                Map.of("active", "true", "mode", "charged"),
                BukkitBlockStateProperties.parseProperties("examplemod:machine[active=true,mode=charged]")
        );
        assertEquals(Map.of(), BukkitBlockStateProperties.parseProperties("examplemod:plain_block"));
    }

    @Test
    void createsBooleanPropertyNeededByCopy() {
        Property<?> property = BukkitBlockStateProperties.createProperty(
                "snowy", "false", Set.of(), ignored -> true
        );

        BooleanProperty booleanProperty = assertInstanceOf(BooleanProperty.class, property);
        assertEquals("snowy", booleanProperty.name());
        assertEquals(java.util.List.of(false, true), booleanProperty.values());
    }

    @Test
    void discoversValidatedIntegerValues() {
        Property<?> property = BukkitBlockStateProperties.createProperty(
                "level",
                "0",
                Set.of(),
                value -> {
                    int number = Integer.parseInt(value);
                    return number >= 0 && number <= 7;
                }
        );

        IntegerProperty integerProperty = assertInstanceOf(IntegerProperty.class, property);
        assertEquals(java.util.List.of(0, 1, 2, 3, 4, 5, 6, 7), integerProperty.values());
    }

    @Test
    void discoversValidatedModdedEnumValues() {
        Property<?> property = BukkitBlockStateProperties.createProperty(
                "mode",
                "idle",
                Set.of("idle", "charged", "invalid"),
                value -> Set.of("idle", "charged").contains(value)
        );

        EnumProperty enumProperty = assertInstanceOf(EnumProperty.class, property);
        assertEquals(Set.of("idle", "charged"), Set.copyOf(enumProperty.values()));
    }

    @Test
    void keepsDirectionalPropertiesRotatable() {
        Property<?> property = BukkitBlockStateProperties.createProperty(
                "facing",
                "north",
                Set.of("north", "east", "south", "west", "invalid"),
                value -> Set.of("north", "east", "south", "west").contains(value)
        );

        DirectionalProperty directionalProperty = assertInstanceOf(DirectionalProperty.class, property);
        assertEquals(
                Set.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST),
                Set.copyOf(directionalProperty.values())
        );
    }
}
