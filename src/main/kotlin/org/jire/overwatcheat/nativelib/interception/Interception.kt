/*
 * Free, open-source undetected color cheat for Overwatch!
 * Copyright (C) 2017  Thomas G. Nappo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jire.overwatcheat.nativelib.interception

import com.sun.jna.Pointer

/**
 * Facade over [InterceptionLibrary], providing a singleton context
 * and convenience [send] functions.
 *
 * All Panama FFI code has been removed; binding is done via JNA.
 */
object Interception {

    private val lib: InterceptionLibrary = InterceptionLibrary.INSTANCE

    /** Opaque interception context pointer. */
    val context: Pointer = lib.interception_create_context()

    /**
     * Sends a mouse [stroke] to [device] using the global [context].
     * @return number of strokes actually sent
     */
    fun send(device: Int, stroke: InterceptionStroke): Int =
        lib.interception_send(context, device, stroke, 1)

    /**
     * Sends a keyboard [stroke] to [device] using the global [context].
     * @return number of strokes actually sent
     */
    fun send(device: Int, stroke: InterceptionKeyStroke): Int =
        lib.interception_send(context, device, stroke, 1)

}
