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

package org.jire.overwatcheat.nativelib

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinUser

object User32 : DirectNativeLib("user32") {

    external fun EnumWindows(lpEnumFunc: WinUser.WNDENUMPROC, userData: Pointer?): Boolean

    external fun GetWindowTextA(hWnd: Pointer, lpString: ByteArray, nMaxCount: Int): Int

    /**
     * Returns the state of the specified virtual key.
     * If the high-order bit is set (< 0 when treated as signed short), the key is pressed.
     */
    @JvmStatic
    external fun GetKeyState(nVirtKey: Int): Short

    /**
     * Translates a virtual-key code to a scan code or character value.
     * @param uCode       virtual-key code or scan code
     * @param uMapType    translation to perform (see MAPVK_* constants)
     */
    @JvmStatic
    external fun MapVirtualKeyA(uCode: Int, uMapType: Int): Int

    /** Convenience overload: translates virtual-key to scan code (MAPVK_VK_TO_VSC). */
    fun MapVirtualKeyA(uCode: Int) = MapVirtualKeyA(uCode, VirtualKeyMapType.MAPVK_VK_TO_VSC)

    object VirtualKeyMapType {
        const val MAPVK_VK_TO_VSC = 0
        const val MAPVK_VSC_TO_VK = 1
        const val MAPVK_VK_TO_CHAR = 2
        const val MAPVK_VSC_TO_VK_EX = 3
        const val MAPVK_VK_TO_VSC_EX = 4
    }

}
