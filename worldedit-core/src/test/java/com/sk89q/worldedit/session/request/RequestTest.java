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

package com.sk89q.worldedit.session.request;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTest {

    @Test
    void requestScopeSupportsNestingAndRestoresTheOuterRequest() {
        AtomicReference<Request> outerReference = new AtomicReference<>();
        AtomicReference<Request> innerReference = new AtomicReference<>();

        Request.runWithRequest(() -> {
            Request outer = Request.request();
            outerReference.set(outer);
            assertTrue(outer.isValid());

            Request.runWithRequest(() -> {
                Request inner = Request.request();
                innerReference.set(inner);
                assertNotSame(outer, inner);
                assertTrue(inner.isValid());
            });

            assertSame(outer, Request.request());
            assertFalse(innerReference.get().isValid());
        });

        assertFalse(outerReference.get().isValid());
        assertThrows(NoSuchElementException.class, Request::request);
    }

    @Test
    void requestScopeIsClearedAfterAnException() {
        assertThrows(IllegalStateException.class, () -> Request.runWithRequest(() -> {
            throw new IllegalStateException("expected");
        }));

        AtomicBoolean called = new AtomicBoolean();
        Request.applyIfPresent(request -> called.set(true));
        assertFalse(called.get());
        assertThrows(NoSuchElementException.class, Request::request);
    }
}
