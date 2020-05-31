package com.revolgenx.anilib.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.StatFs
import java.io.File
import java.io.FileNotFoundException
import java.util.*


val GB: Long = 1073741824 // 1024 * 1024 * 1024
val MB: Long = 1048576 // 1024 * 1024
val KB: Long = 1024


fun getDefualtStoragePath(): String {
    val path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        .absolutePath

    val dir = File(path)
    return if (dir.exists() && dir.isDirectory)
        path
    else
        if (dir.mkdirs()) path else ""
}

fun Int.formatSize(): String {
    return when {
        this >= GB -> {
            String.format("%d GB", this / GB)
        }
        this >= MB -> {
            String.format("%d MB", this  / MB)
        }
        else -> {
            String.format("%d KB", this  / KB)
        }
    }
}

fun Long.formatSize(): String {
    return when {
        this >= GB -> {
            String.format("%.1f GB", this * 1.0 / GB)
        }
        this >= MB -> {
            String.format("%.1f MB", this * 1.0 / MB)
        }
        else -> {
            String.format("%.1f KB", this * 1.0 / KB)
        }
    }
}


fun Long.formatSpeed(): String = this.formatSize() + "/s"

fun makeTempFile(context: Context, postfix: String): File {
    return File(getTempDir(context), UUID.randomUUID().toString() + postfix)
}

private fun getTempDir(context: Context): File? {
    val tmpDir = File(context.getExternalFilesDir(null), "temp")
    if (!tmpDir.exists()) {
        if (!tmpDir.mkdirs()) {
            return null
        }
    }

    return tmpDir
}

@Throws(FileNotFoundException::class)
fun uriContentToByteArray(context: Context, uri: Uri): ByteArray? {
    return context.contentResolver.openInputStream(uri)?.let {
        val bytes = it.readBytes()
        it.close()
        bytes
    }
}

fun getFree(file: File?): Long {
    var f = file
    while (!f!!.exists()) {
        f = f.parentFile
        if (f == null) return 0
    }
    val fsi = StatFs(f.path)
    return fsi.blockSizeLong * fsi.availableBlocksLong
}
