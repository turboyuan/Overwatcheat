package org.jire.overwatcheat.aimbot

import org.jire.overwatcheat.Screen
import java.awt.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Debug overlay that draws on top of the screen:
 * - Green rectangle when a target color is detected
 * - "未识别到" text when nothing is found
 * - Large colored debug info at top-left
 */
object DebugOverlay {

    @Volatile var detected = false
    @Volatile var boxScreenX = 0
    @Volatile var boxScreenY = 0
    @Volatile var boxScreenW = 0
    @Volatile var boxScreenH = 0
    @Volatile var debugInfo = ""
    @Volatile var sampleColors = ""  // sampled pixel colors from center of capture

    private var frame: JFrame? = null
    private var panel: JPanel? = null

    fun start(captureOffsetX: Int, captureOffsetY: Int) {
        SwingUtilities.invokeLater {
            val jf = JFrame("Debug Overlay")
            jf.isUndecorated = true
            jf.isAlwaysOnTop = true
            jf.background = Color(0, 0, 0, 0)
            jf.setSize(Screen.WIDTH, Screen.HEIGHT)
            jf.setLocation(0, 0)
            jf.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            jf.type = Window.Type.UTILITY

            val jp = object : JPanel() {
                init {
                    isOpaque = false
                }

                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

                    // Draw the scan region border (bright cyan dashed)
                    g2.color = Color(0, 255, 255, 180)
                    g2.stroke = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(8f, 4f), 0f)
                    g2.drawRect(captureOffsetX, captureOffsetY,
                        Screen.WIDTH - 2 * captureOffsetX,
                        Screen.HEIGHT - 2 * captureOffsetY)

                    if (detected) {
                        // Draw green rectangle around detected target
                        g2.color = Color(0, 255, 0, 220)
                        g2.stroke = BasicStroke(3f)
                        g2.drawRect(boxScreenX, boxScreenY, boxScreenW, boxScreenH)

                        // Label
                        g2.font = Font("Microsoft YaHei", Font.BOLD, 22)
                        g2.color = Color(0, 255, 0, 255)
                        drawTextWithShadow(g2, ">>> 已识别 <<<", boxScreenX, boxScreenY - 8)
                    } else {
                        // Show "未识别到" prominently above scan area
                        g2.font = Font("Microsoft YaHei", Font.BOLD, 36)
                        g2.color = Color(255, 60, 60, 255)
                        val text = ">>> 未识别到 <<<"
                        val fm = g2.fontMetrics
                        val tx = (width - fm.stringWidth(text)) / 2
                        val ty = captureOffsetY - 15
                        drawTextWithShadow(g2, text, tx, ty)
                    }

                    // Draw debug info — LARGE, bright yellow with black shadow
                    if (debugInfo.isNotEmpty()) {
                        g2.font = Font("Consolas", Font.BOLD, 20)
                        var yPos = 30
                        for (line in debugInfo.split("\n")) {
                            g2.color = Color(0, 0, 0, 200)
                            g2.drawString(line, 12, yPos + 1)
                            g2.color = Color(255, 255, 0, 255)
                            g2.drawString(line, 10, yPos)
                            yPos += 24
                        }

                        // AimBot status line
                        val keyStr = if (AimBotState.aimKeyPressed) "KEY:DOWN" else "KEY:UP"
                        val aimLine = "$keyStr | ${AimBotState.lastMoveAttempt} | moves:${AimBotState.moveCount}"
                        g2.color = Color(0, 0, 0, 200)
                        g2.drawString(aimLine, 12, yPos + 1)
                        g2.color = Color(255, 165, 0, 255)  // orange
                        g2.drawString(aimLine, 10, yPos)
                    }

                    // Draw sampled colors — bright cyan
                    if (sampleColors.isNotEmpty()) {
                        g2.font = Font("Consolas", Font.BOLD, 18)
                        var yPos = 30 + (debugInfo.split("\n").size) * 24 + 10
                        for (line in sampleColors.split("\n")) {
                            g2.color = Color(0, 0, 0, 200)
                            g2.drawString(line, 12, yPos + 1)
                            g2.color = Color(0, 255, 255, 255)
                            g2.drawString(line, 10, yPos)
                            yPos += 22
                        }
                    }
                }

                private fun drawTextWithShadow(g2: Graphics2D, text: String, x: Int, y: Int) {
                    val origColor = g2.color
                    g2.color = Color(0, 0, 0, 200)
                    g2.drawString(text, x + 2, y + 2)
                    g2.color = origColor
                    g2.drawString(text, x, y)
                }
            }
            jp.layout = null
            jf.contentPane = jp

            // Make fully transparent and click-through
            try {
                jf.opacity = 1.0f
                com.sun.jna.platform.win32.User32.INSTANCE.let { u32 ->
                    val hwnd = com.sun.jna.platform.win32.User32.INSTANCE.FindWindow(null, "Debug Overlay")
                    if (hwnd != null) {
                        val exStyle = u32.GetWindowLong(hwnd, com.sun.jna.platform.win32.WinUser.GWL_EXSTYLE)
                        u32.SetWindowLong(hwnd, com.sun.jna.platform.win32.WinUser.GWL_EXSTYLE,
                            exStyle or 0x80000 or 0x20)
                    }
                }
            } catch (_: Exception) {}

            jf.isVisible = true
            frame = jf
            panel = jp

            Thread({
                while (true) {
                    jp.repaint()
                    Thread.sleep(33)
                }
            }, "Overlay Repaint").apply { isDaemon = true }.start()
        }
    }
}
