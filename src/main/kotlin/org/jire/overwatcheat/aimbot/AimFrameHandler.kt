/*
 * Free, open-source undetected color cheat for Overwatch!
 * Copyright (C) 2017  Thomas G. Nappo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package org.jire.overwatcheat.aimbot

import org.jire.overwatcheat.framegrab.RobotFrameHandler
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class AimFrameHandler(
    val colorMatcher: AimColorMatcher,
    val captureOffsetX: Int = 0,
    val captureOffsetY: Int = 0,
) : RobotFrameHandler {

    @Volatile private var frameCount = 0L
    @Volatile private var matchCount = 0L
    private var lastSaveTime = 0L

    init {
        // Clear debug_frames on startup so screenshots are always from the current run
        val dir = File("debug_frames")
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
        dir.mkdirs()
        System.err.println("[DEBUG] Cleared debug_frames directory")
    }

    override fun handle(image: BufferedImage) {
        val frameWidth = image.width
        val frameHeight = image.height

        frameCount++

        // Save debug frame every 5 seconds
        val now = System.currentTimeMillis()
        if (now - lastSaveTime > 5000) {
            lastSaveTime = now
            try {
                val dir = File("debug_frames")
                val outFile = File(dir, "frame_${now}.png")
                ImageIO.write(image, "PNG", outFile)
                System.err.println("[DEBUG] Saved frame to ${outFile.absolutePath}")
            } catch (e: Exception) {
                System.err.println("[DEBUG] Failed to save frame: ${e.message}")
            }
        }

        var found = false
        var xHigh = Int.MIN_VALUE
        var xLow = Int.MAX_VALUE
        var yHigh = Int.MIN_VALUE
        var yLow = Int.MAX_VALUE
        var pixelMatchCount = 0

        // Also track what magenta-like pixels we actually see (same logic as colorMatcher)
        val colorSamples = linkedMapOf<String, Int>()

        for (x in 0 until frameWidth) {
            for (y in 0 until frameHeight) {
                val argb = image.getRGB(x, y)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                val packed = (r shl 16) or (g shl 8) or b

                // Track any pixel where R>200, G<50, B>200 for visibility
                if (r > 200 && g < 50 && b > 200) {
                    val hex = "#${"%02X%02X%02X".format(r, g, b)}"
                    colorSamples[hex] = (colorSamples[hex] ?: 0) + 1
                }

                if (!colorMatcher.colorMatches(packed)) continue

                found = true
                pixelMatchCount++
                matchCount++
                if (x > xHigh) xHigh = x
                if (x < xLow) xLow = x
                if (y > yHigh) yHigh = y
                if (y < yLow) yLow = y
            }
        }

        // Update debug overlay
        DebugOverlay.detected = found
        if (found) {
            DebugOverlay.boxScreenX = captureOffsetX + xLow
            DebugOverlay.boxScreenY = captureOffsetY + yLow
            DebugOverlay.boxScreenW = xHigh - xLow
            DebugOverlay.boxScreenH = yHigh - yLow
        }
        DebugOverlay.debugInfo = "frame #$frameCount | ${frameWidth}x$frameHeight (Robot)\nmatched pixels: $pixelMatchCount | total matches: $matchCount"

        val sb = StringBuilder()
        if (colorSamples.isEmpty()) {
            sb.append("No magenta pixels in frame (R>200,G<50,B>200)")
        } else {
            sb.append("Magenta-like pixels (top 8):")
            colorSamples.entries.sortedByDescending { it.value }.take(8).forEach { (hex, cnt) ->
                sb.append("\n  $hex x$cnt")
            }
        }
        DebugOverlay.sampleColors = sb.toString()

        AimBotState.aimData =
            if (found)
                (xLow.toLong() shl 48) or
                        (xHigh.toLong() shl 32) or
                        (yLow.toLong() shl 16) or
                        yHigh.toLong()
            else 0
    }
}
