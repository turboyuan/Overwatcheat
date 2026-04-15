/*
 * Free, open-source undetected color cheat for Overwatch!
 * Copyright (C) 2017  Thomas G. Nappo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package org.jire.overwatcheat.framegrab

import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage

/**
 * Replaces the FFmpeg/gdigrab frame grabber with a simple java.awt.Robot
 * screen capture. This avoids all window-title matching problems and
 * transparent overlay interference.
 */
class RobotFrameGrabberThread(
    private val captureX: Int,
    private val captureY: Int,
    private val captureWidth: Int,
    private val captureHeight: Int,
    private val handler: RobotFrameHandler,
    private val targetFps: Double = 60.0,
) : Thread("Frame Grabber") {

    private val robot = Robot()
    private val rect = Rectangle(captureX, captureY, captureWidth, captureHeight)
    private val frameIntervalNs = (1_000_000_000.0 / targetFps).toLong()

    override fun run() {
        priority = MAX_PRIORITY
        var lastFrameTime = System.nanoTime()
        while (true) {
            val img: BufferedImage = robot.createScreenCapture(rect)
            handler.handle(img)

            val elapsed = System.nanoTime() - lastFrameTime
            val sleepNs = frameIntervalNs - elapsed
            if (sleepNs > 100_000) {
                val sleepMs = sleepNs / 1_000_000
                val sleepNsRem = (sleepNs % 1_000_000).toInt()
                sleep(sleepMs, sleepNsRem)
            }
            lastFrameTime = System.nanoTime()
        }
    }
}
