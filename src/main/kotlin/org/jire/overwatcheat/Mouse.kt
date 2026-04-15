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

import org.jire.overwatcheat.nativelib.interception.Interception
import org.jire.overwatcheat.nativelib.interception.InterceptionFilter
import org.jire.overwatcheat.nativelib.interception.InterceptionMouseFlag
import org.jire.overwatcheat.nativelib.interception.InterceptionStroke
import java.lang.Thread.sleep

object Mouse {

    private val moveStroke = InterceptionStroke().apply {
        state = 0
        flags = (InterceptionMouseFlag.INTERCEPTION_MOUSE_MOVE_RELATIVE or
                InterceptionMouseFlag.INTERCEPTION_MOUSE_CUSTOM).toShort()
        rolling = 0
        x = 0
        y = 0
        information = 0
    }

    private val clickStroke = InterceptionStroke()

    fun move(x: Int, y: Int, deviceID: Int) {
        moveStroke.x = x
        moveStroke.y = y
        Interception.send(deviceID, moveStroke)
    }

    fun click(deviceID: Int) {
        clickStroke.state = InterceptionFilter.INTERCEPTION_MOUSE_LEFT_BUTTON_DOWN.toShort()
        clickStroke.x = 0
        clickStroke.y = 0
        Interception.send(deviceID, clickStroke)

        sleep(300)

        clickStroke.state = InterceptionFilter.INTERCEPTION_MOUSE_LEFT_BUTTON_UP.toShort()
        Interception.send(deviceID, clickStroke)
    }

}
