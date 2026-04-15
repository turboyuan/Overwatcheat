package org.jire.overwatcheat.framegrab

import java.awt.image.BufferedImage

interface RobotFrameHandler {
    fun handle(image: BufferedImage)
}
