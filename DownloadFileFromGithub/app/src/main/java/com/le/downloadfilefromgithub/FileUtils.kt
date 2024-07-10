package com.le.downloadfilefromgithub

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Created by wangxl1 on 2024/7/8 14:53
 * E-Mail Address： wang_x_le@163.com
 */
object FileUtils {

    fun getApkFile(fileName: String): File {
        Log
            .d(
                "File",
                "LeApplication.application?.filesDir = ${LeApplication.application?.filesDir}"
            )
        val path = LeApplication.application?.filesDir?.absolutePath + "/apk"
        if(!File(path).exists()){
           File(path).mkdir()
        }
        return File(path, fileName)
    }

    fun getFileMD5(file: File): String {
        val buffer = ByteArray(1024)
        val md5Digest = MessageDigest.getInstance("MD5")
        val fis = FileInputStream(file)

        var numRead: Int
        while (fis.read(buffer).also { numRead = it } != -1) {
            md5Digest.update(buffer, 0, numRead)
        }
        fis.close()

        val md5Bytes = md5Digest.digest()
        val sb = StringBuilder()
        for (b in md5Bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}