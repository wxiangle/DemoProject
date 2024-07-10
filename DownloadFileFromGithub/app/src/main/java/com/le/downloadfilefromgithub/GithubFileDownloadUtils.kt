package com.le.downloadfilefromgithub

import android.os.Build
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.Base64

/**
 * Created by wangxl1 on 2024/7/8 11:14
 * E-Mail Address： wang_x_le@163.com
 */
object GithubFileDownloadUtils {

    private fun fetchFileContent(): String {
        // 你的 GitHub 用户名
        val username = "wangxiangle"
        // 你的 GitHub Personal Access Token

        // 仓库名称，格式为 'owner/repo'
        val repo = "owner/repo"
        // 文件路径，相对于仓库根目录
        val path = "path/to/your/file"

        // GitHub API URL
        val url = "https://api.github.com/repos/$repo/contents/$path"

        val client = OkHttpClient()
        val credential = Credentials.basic(username, token)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", credential)
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                "Failed to retrieve file: ${response.code}"
            } else {
                // 解析 JSON 响应
                val jsonResponse = response.body?.string()
                val jsonObject = JSONObject(jsonResponse)
                // 获取文件内容并解码
                val fileContent = jsonObject.getString("content")
                val decodedBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Base64.getDecoder().decode(fileContent)
                } else {
                    android.util.Base64.decode(fileContent, android.util.Base64.DEFAULT)
                }
                String(decodedBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

}