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

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File

/**
 * JNA binding for interception.dll.
 * Replaces the old Panama FFI approach to avoid JDK version compatibility issues.
 */
interface InterceptionLibrary : Library {

    companion object {

        init {
            // Build a search path covering all likely DLL locations so that
            // JNA can find interception.dll no matter how the application is
            // launched (gradlew run, run.bat from build/overwatcheat, etc.).
            val extraPaths = mutableListOf<String>()

            // 1. Current working directory
            extraPaths += System.getProperty("user.dir", ".")

            // 2. Directory where the running jar lives (build/overwatcheat/)
            val jarDir = File(
                InterceptionLibrary::class.java.protectionDomain
                    .codeSource.location.toURI()
            ).parentFile
            if (jarDir != null) extraPaths += jarDir.absolutePath

            // 3. Interception/library/x64 relative to working directory
            val relX64 = File("Interception/library/x64")
            if (relX64.isDirectory) extraPaths += relX64.absolutePath

            // 4. Interception/library/x64 relative to the jar directory
            //    (handles cases where workingDir != projectRoot)
            val jarX64 = File(jarDir, "../../Interception/library/x64").canonicalFile
            if (jarX64.isDirectory) extraPaths += jarX64.absolutePath

            // Merge with any existing jna.library.path / java.library.path
            val existing = listOfNotNull(
                System.getProperty("jna.library.path"),
                System.getProperty("java.library.path")
            ).flatMap { it.split(File.pathSeparator) }

            val merged = (extraPaths + existing)
                .distinct()
                .joinToString(File.pathSeparator)

            System.setProperty("jna.library.path", merged)
        }

        val INSTANCE: InterceptionLibrary =
            Native.load("interception", InterceptionLibrary::class.java)
    }

    /** Creates and returns an interception context (opaque pointer). */
    fun interception_create_context(): Pointer

    /** Destroys an interception context. */
    fun interception_destroy_context(context: Pointer)

    /**
     * Sends a stroke to the given device.
     * @param context  the interception context
     * @param device   device ID (mouse: 11..20, keyboard: 1..10)
     * @param stroke   the stroke structure to send (mouse or keyboard)
     * @param nStroke  number of strokes (usually 1)
     * @return number of strokes sent
     */
    fun interception_send(context: Pointer, device: Int, stroke: Structure, nStroke: Int): Int

}
