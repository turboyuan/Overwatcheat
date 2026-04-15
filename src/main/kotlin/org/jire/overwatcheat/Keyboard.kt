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

package org.jire.overwatcheat

import org.jire.overwatcheat.nativelib.User32
import org.jire.overwatcheat.nativelib.interception.Interception
import org.jire.overwatcheat.nativelib.interception.InterceptionKeyState
import org.jire.overwatcheat.nativelib.interception.InterceptionKeyStroke

object Keyboard {

    fun keyState(virtualKeyCode: Int): Short = User32.GetKeyState(virtualKeyCode)

    fun keyPressed(virtualKeyCode: Int): Boolean = keyState(virtualKeyCode) < 0

    fun keyReleased(virtualKeyCode: Int): Boolean = !keyPressed(virtualKeyCode)

    private val stroke = InterceptionKeyStroke()

    fun pressKey(scanCode: Int, deviceId: Int) {
        stroke.code = scanCode.toShort()
        stroke.state = InterceptionKeyState.INTERCEPTION_KEY_DOWN.toShort()
        Interception.send(deviceId, stroke)
    }

    fun releaseKey(scanCode: Int, deviceId: Int) {
        stroke.code = scanCode.toShort()
        stroke.state = InterceptionKeyState.INTERCEPTION_KEY_UP.toShort()
        Interception.send(deviceId, stroke)
    }

}
