package com.github.rahmnathan.localmovies.app.media.provider.control

import java.io.File
import java.util.*
import java.util.stream.Collectors

internal object MediaPathUtils {
    @JvmStatic
    fun getParentPath(path: String): String {
        val dirs = path.split(File.separator).toTypedArray()
        return Arrays.stream(dirs)
                .limit(dirs.size - 1.toLong())
                .collect(Collectors.joining(File.separator))
    }

    @JvmStatic
    fun getFilename(path: String): String {
        val directoryList = path.split(File.separator).toTypedArray()
        return directoryList[directoryList.size - 1]
    }
}