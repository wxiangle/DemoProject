package com.le.downloadfilefromgithub

/**
 * Created by wangxl1 on 2024/7/8 11:15
 * E-Mail Address： wang_x_le@163.com
 */
class GradleFileParserUtils {
    private fun parseGradleContent(fileContent: String) {
        val versionCodeRegex = """versionCode\s+(\d+)""".toRegex()
        val versionNameRegex = """versionName\s+"([^"]+)"""".toRegex()

        val versionCode = versionCodeRegex.find(fileContent)?.groupValues?.get(1)
        val versionName = versionNameRegex.find(fileContent)?.groupValues?.get(1)

        println("Version Code: $versionCode")
        println("Version Name: $versionName")
    }
}
