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

import com.google.common.base.Splitter;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.data.BlockData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Discovers block-state properties using only the stable Bukkit API.
 *
 * <p>This is slower than a native adapter, but it works across Minecraft versions and also
 * allows hybrid servers to expose modded materials through Bukkit's material registry.</p>
 */
final class BukkitBlockStateProperties {

    private static final int MIN_PROBED_INTEGER = -16;
    private static final int MAX_PROBED_INTEGER = 127;
    private static final Set<String> DIRECTIONS = Set.of("north", "east", "south", "west", "up", "down");

    private final Map<Material, Map<String, ? extends Property<?>>> properties = new ConcurrentHashMap<>();
    private volatile Set<String> enumCandidates;

    Map<String, ? extends Property<?>> get(Material material) {
        if (material == null || !material.isBlock()) {
            return Collections.emptyMap();
        }
        return properties.computeIfAbsent(material, this::discover);
    }

    private Map<String, ? extends Property<?>> discover(Material material) {
        final BlockData defaultData;
        try {
            defaultData = material.createBlockData();
        } catch (RuntimeException | LinkageError ignored) {
            return Collections.emptyMap();
        }

        String serialized = defaultData.getAsString();
        Map<String, String> serializedProperties = parseProperties(serialized);
        if (serializedProperties.isEmpty()) {
            return Collections.emptyMap();
        }

        int bracket = serialized.indexOf('[');
        String blockId = bracket < 0 ? serialized : serialized.substring(0, bracket);
        Set<String> candidates = getEnumCandidates();
        Map<String, Property<?>> result = new LinkedHashMap<>();
        serializedProperties.forEach((name, currentValue) -> {
            Predicate<String> accepted = value -> isAccepted(blockId, name, value);
            result.put(name, createProperty(name, currentValue, candidates, accepted));
        });
        return Collections.unmodifiableMap(result);
    }

    private boolean isAccepted(String blockId, String property, String value) {
        try {
            Bukkit.createBlockData(blockId + "[" + property + "=" + value + "]");
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private Set<String> getEnumCandidates() {
        Set<String> candidates = enumCandidates;
        if (candidates != null) {
            return candidates;
        }
        synchronized (this) {
            if (enumCandidates == null) {
                enumCandidates = Collections.unmodifiableSet(discoverEnumCandidates());
            }
            return enumCandidates;
        }
    }

    private static Set<String> discoverEnumCandidates() {
        Set<String> result = new LinkedHashSet<>();
        for (Material material : Registry.MATERIAL) {
            if (!material.isBlock()) {
                continue;
            }
            try {
                BlockData data = material.createBlockData();
                result.addAll(parseProperties(data.getAsString()).values());
                addEnumValues(data, result);
            } catch (RuntimeException | LinkageError ignored) {
                // A hybrid server can expose a material before its backing mod is fully ready.
            }
        }
        return result;
    }

    private static void addEnumValues(BlockData data, Set<String> result) {
        for (Method method : data.getClass().getMethods()) {
            addEnumType(method.getReturnType(), result);
            for (Class<?> parameterType : method.getParameterTypes()) {
                addEnumType(parameterType, result);
            }
            if (method.getParameterCount() != 0
                    || !(method.getName().startsWith("get") || method.getName().startsWith("is"))) {
                continue;
            }
            try {
                Object value = method.invoke(data);
                if (value instanceof Collection<?> collection) {
                    for (Object element : collection) {
                        if (element instanceof Enum<?> enumValue) {
                            result.add(toStateValue(enumValue));
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
                // Enum types from the public method signature are still useful when invocation is blocked.
            }
        }
    }

    private static void addEnumType(Class<?> type, Set<String> result) {
        if (!type.isEnum() || type == Material.class) {
            return;
        }
        for (Object constant : type.getEnumConstants()) {
            result.add(toStateValue((Enum<?>) constant));
        }
    }

    private static String toStateValue(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    static Map<String, String> parseProperties(String serialized) {
        int opening = serialized.indexOf('[');
        int closing = serialized.lastIndexOf(']');
        if (opening < 0 || closing <= opening + 1) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        String body = serialized.substring(opening + 1, closing);
        for (String entry : Splitter.on(',').split(body)) {
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                continue;
            }
            result.put(entry.substring(0, separator), entry.substring(separator + 1));
        }
        return Collections.unmodifiableMap(result);
    }

    static BlockState applyProperties(BlockState state, String serialized) {
        for (Map.Entry<String, String> entry : parseProperties(serialized).entrySet()) {
            @SuppressWarnings("unchecked")
            Property<Object> property = (Property<Object>) state.getBlockType().getPropertyMap().get(entry.getKey());
            if (property == null) {
                continue;
            }
            try {
                state = state.with(property, property.getValueFor(entry.getValue()));
            } catch (IllegalArgumentException ignored) {
                // Keep the first valid state value if a hybrid server reports an inconsistent default.
            }
        }
        return state;
    }

    static Property<?> createProperty(
            String name,
            String currentValue,
            Set<String> enumCandidates,
            Predicate<String> accepted
    ) {
        if ("true".equals(currentValue) || "false".equals(currentValue)) {
            List<Boolean> values = List.of(false, true).stream()
                    .filter(value -> accepted.test(value.toString()) || value.toString().equals(currentValue))
                    .toList();
            return new BooleanProperty(name, values);
        }

        try {
            int current = Integer.parseInt(currentValue);
            int minimum = Math.min(MIN_PROBED_INTEGER, current - 16);
            int maximum = Math.max(MAX_PROBED_INTEGER, current + 16);
            maximum = Math.min(maximum, 1024);
            minimum = Math.max(minimum, -1024);
            Set<Integer> values = new LinkedHashSet<>();
            for (int value = minimum; value <= maximum; value++) {
                if (value == current || accepted.test(Integer.toString(value))) {
                    values.add(value);
                }
            }
            return new IntegerProperty(name, List.copyOf(values));
        } catch (NumberFormatException ignored) {
            // Enum-like state property.
        }

        Set<String> values = new LinkedHashSet<>();
        values.add(currentValue);
        for (String candidate : enumCandidates) {
            if (candidate.equals(currentValue) || accepted.test(candidate)) {
                values.add(candidate);
            }
        }
        if (DIRECTIONS.contains(currentValue) && DIRECTIONS.containsAll(values)) {
            List<Direction> directions = values.stream()
                    .map(value -> Direction.valueOf(value.toUpperCase(Locale.ROOT)))
                    .toList();
            return new DirectionalProperty(name, directions);
        }
        return new EnumProperty(name, List.copyOf(values));
    }
}
