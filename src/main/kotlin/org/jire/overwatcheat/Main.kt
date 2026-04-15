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

import net.openhft.chronicle.core.Jvm
import org.bytedeco.javacv.FFmpegLogCallback
import org.jire.overwatcheat.aimbot.*
import org.jire.overwatcheat.aimbot.DebugOverlay
import org.jire.overwatcheat.framegrab.RobotFrameGrabberThread
import org.jire.overwatcheat.nativelib.Kernel32
import org.jire.overwatcheat.settings.Settings
import org.jire.overwatcheat.util.PreciseSleeper
import java.util.concurrent.TimeUnit

object Main {

    init {
        Jvm.init()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Kernel32.SetPriorityClass(Kernel32.GetCurrentProcess(), Kernel32.HIGH_PRIORITY_CLASS)

        Settings.read()

        val captureWidth = Settings.boxWidth
        val captureHeight = Settings.boxHeight

        val captureOffsetX = (Screen.WIDTH - captureWidth) / 2
        val captureOffsetY = (Screen.HEIGHT - captureHeight) / 2

        val captureCenterX = captureWidth / 2
        val captureCenterY = captureHeight / 2

        val aimColorMatcher = AimColorMatcher()
        aimColorMatcher.initializeMatchSet()

        val frameHandler = AimFrameHandler(aimColorMatcher, captureOffsetX, captureOffsetY)

        // Start debug overlay
        DebugOverlay.start(captureOffsetX, captureOffsetY)

        val frameGrabberThread = RobotFrameGrabberThread(
            captureOffsetX, captureOffsetY,
            captureWidth, captureHeight,
            frameHandler,
            Settings.fps
        )

        val maxSnapX = (captureWidth / Settings.maxSnapDivisor).toInt()
        val maxSnapY = (captureHeight / Settings.maxSnapDivisor).toInt()

        val toggleUIThread = ToggleUIThread(Settings.keyboardId, *Settings.toggleKeyCodes)

        val preciseSleeper = PreciseSleeper[Settings.aimPreciseSleeperType] ?: PreciseSleeper.YIELD
        val aimMode = AimMode[Settings.aimMode] ?: AimMode.TRACKING
        val aimBotThread = AimBotThread(
            captureCenterX, captureCenterY,
            maxSnapX, maxSnapY,
            preciseSleeper,
            Settings.aimCpuThreadAffinityIndex,
            aimMode,
            TimeUnit.MILLISECONDS.toNanos(Settings.flickPause)
        )

        frameGrabberThread.start()
        toggleUIThread.start()
        aimBotThread.start()
    }

}
