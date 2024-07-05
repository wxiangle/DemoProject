package com.le.downloadfilefromgithub

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.le.downloadfilefromgithub.ui.theme.DownloadFileFromGithubTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import android.util.Base64 as AndroidBase64

class MainActivity : ComponentActivity() {

    val token = "y9U13XcYTuqtFNCvGy4B"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownloadFileFromGithubTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("检查更新", modifier = Modifier.size(300.dp, 50.dp))
                }
            }
        }
    }
}
val token = "y9U13XcYTuqtFNCvGy4B"
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    var text by remember { mutableStateOf("Hello, $name!") }
    var showDialog by remember {
        mutableStateOf(true)
    }
    var mData: VersionData? by remember { mutableStateOf(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }


    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        // 创建一个按钮并添加点击事件
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                val fileContent = fetchFileContentFromGitLab()
                withContext(Dispatchers.Main) {
//                parseGradleContent(fileContent)
                    Log.d(
                        "downloadfilefromgithub",
                        "\nversionName -> ${fileContent} "
                    )
                    parseUpdateFile(fileContent) { mVersionData ->
                        Log.d(
                            "downloadfilefromgithub",
                            "\nversionName -> ${mVersionData.versionName} "
                        )
                        Log.d(
                            "downloadfilefromgithub",
                            "\nversionCode -> ${mVersionData.versionCode} "
                        )
                        Log.d(
                            "downloadfilefromgithub",
                            "\nupdateMessage -> \n${mVersionData.updateMessage} "
                        )
                        Log.d(
                            "downloadfilefromgithub",
                            "\nupdateTime -> ${mVersionData.updateTime} "
                        )
                        mData = mVersionData
                    }

                    text = fileContent
                    showDialog = !showDialog
                }
            }

        }) {
            Text(
                text = "$name!",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "更新提示") },
                text = {
                    Text(
                        text = "检测到新版本，是否下载并安装？\n更新版本：v${mData?.versionName}\n更新时间：${mData?.updateTime}\n更新内容：\n" +
                                "${mData?.updateMessage}"
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val apkUrl = mData?.apkUrl
                        //启动携程下载 apk
                        downloading = true


                        CoroutineScope(Dispatchers.IO).launch {
                            val downloadUrl = if (apkUrl != null) {
                                if (apkUrl?.contains("http") == true) {
                                    //其他地址 apk 地址 直接下载
                                    apkUrl
                                } else {
                                    get52ToysGitlabFileUrl(apkUrl)
                                }
                            } else {
                                null
                            }
                            downloadApk(downloadUrl, "gacha_release.apk") {
                                progress = it
                                if (it == 1f) {
                                    downloading = false
                                    installApk(apkFilePath = "gacha_release.apk")
                                }
                            }

                        }

                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("取消")
                    }
                }
            )

        }
        if (downloading) {
            LinearProgressIndicator(progress = progress)
        }
    }


}


fun installApk(apkFilePath: String) {
    try {
        // 假设您已经配置了adb路径并具有相应权限
        val process = Runtime.getRuntime().exec("adb install $apkFilePath")
        process.waitFor()
        if (process.exitValue() == 0) {
            println("APK安装成功")
        } else {
            println("APK安装失败")
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

suspend fun downloadApk(url: String?, fileName: String, onProgress: (Float) -> Unit) {
    if (url == null) {
        onProgress.invoke(0f)
        //TODO 下载失败
        return
    }
    withContext(Dispatchers.IO) {
        try {
            val url = URL(url)
            val connection = url.openConnection()
            connection.setRequestProperty("PRIVATE-TOKEN", token)
            connection.connect()
            val fileLength = connection.contentLength

            val input = connection.getInputStream()
            val output = FileOutputStream(File(fileName))

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                // publishing the progress....
                onProgress(total.toFloat() / fileLength)
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            onProgress(1f) // 下载完成
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1f) // 下载失败
        }
    }
}

val TAG = "downloadfilefromgithub"
private fun parseUpdateFile(
    fileContent: String,
    handleContent: ((mVersionData: VersionData) -> Unit)
) {
    runCatching {
        val jsonObject = JSONObject(fileContent)
        val versionCode = jsonObject.getInt("versionCode")
        val versionName = jsonObject.getString("versionName")
        val versionMessage = jsonObject.getString("updateMessage")
        val versionTime = jsonObject.getString("updateTime")
        val apkUrl = jsonObject.getString("apkUrl")
        handleContent.invoke(
            VersionData(
                versionCode,
                versionName,
                versionMessage,
                versionTime,
                apkUrl
            )
        )
    }.getOrElse {
        Log.d(TAG, "error-> ${it.message}")
    }
}

private fun parseGradleContent(fileContent: String) {
    val versionCodeRegex = """versionCode\s+(\d+)""".toRegex()
    val versionNameRegex = """versionName\s+"([^"]+)"""".toRegex()

    val versionCode = versionCodeRegex.find(fileContent)?.groupValues?.get(1)
    val versionName = versionNameRegex.find(fileContent)?.groupValues?.get(1)

    println("Version Code: $versionCode")
    println("Version Name: $versionName")
}

private fun get52ToysGitlabFileUrl(filePath: String): String {
    val gitLabUrl = "http://gitlab.52toys.com"
    val repoPath = "gacha/gacha-android"
    val branch = "dizhi"

    val url = "$gitLabUrl/api/v4/projects/${
        URLEncoder.encode(
            repoPath,
            "UTF-8"
        )
    }/repository/files/${URLEncoder.encode(filePath, "UTF-8")}/raw?ref=$branch"
    return url
}


private fun fetchFileContentFromGitLab(): String {
    // GitLab 的 URL，例如 "https://gitlab.com"
    val gitLabUrl = "http://gitlab.52toys.com"
    // 仓库 ID 或路径，例如 "namespace/repo"
    val repoPath = "gacha/gacha-android"
    // 文件路径，相对于仓库根目录，例如 "README.md"
    val filePath = "app/update.json"
    // 分支名称，例如 "main"
    val branch = "dizhi"
    // 你的 GitLab Personal Access Token
    val token = "y9U13XcYTuqtFNCvGy4B"

    // GitLab API URL
    val url = "$gitLabUrl/api/v4/projects/${
        URLEncoder.encode(
            repoPath,
            "UTF-8"
        )
    }/repository/files/${URLEncoder.encode(filePath, "UTF-8")}/raw?ref=$branch"

    Log.d("downloadfilefromgithub", "url -> $url ")
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .header("PRIVATE-TOKEN", token)
        .build()

    return try {
        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            "Failed to retrieve file: ${response.code}"
        } else {
            // 获取文件内容
            val fileContent = response.body?.string()
            fileContent ?: "No content"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: ${e.message}"
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DownloadFileFromGithubTheme {
        Greeting("Android")
    }
}

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
                AndroidBase64.decode(fileContent, AndroidBase64.DEFAULT)
            }
            String(decodedBytes)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Error: ${e.message}"
    }
}

data class VersionData(
    val versionCode: Int,
    val versionName: String,
    val updateMessage: String,
    val updateTime: String,
    val apkUrl: String
)

