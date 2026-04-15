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

package org.jire.overwatcheat.aimbot

object AimBotState {

    @Volatile var aimData = 0L
    @Volatile var flicking = false
    @Volatile var toggleUI = false

    // Debug info for overlay
    @Volatile var aimKeyPressed = false
    @Volatile var lastDx = 0
    @Volatile var lastDy = 0
    @Volatile var lastMoveAttempt = ""   // e.g. "SENT(3,2)" or "SKIPPED(maxSnap)" or "SKIPPED(minSize)"
    @Volatile var moveCount = 0L

}

