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

import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder

/**
 * Maps to InterceptionKeyStroke in interception.h:
 *   unsigned short code;        // offset 0  (scan code)
 *   unsigned short state;       // offset 2  (key state flags)
 *   unsigned int information;   // offset 4
 * Total: 8 bytes
 */
@FieldOrder("code", "state", "information")
class InterceptionKeyStroke(
    @JvmField var code: Short = 0,
    @JvmField var state: Short = 0,
    @JvmField var information: Int = 0,
) : Structure()
