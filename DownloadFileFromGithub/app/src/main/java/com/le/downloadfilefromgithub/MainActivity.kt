package com.le.downloadfilefromgithub

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.le.downloadfilefromgithub.ui.theme.DownloadFileFromGithubTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.net.URLEncoder

class MainActivity : ComponentActivity() {


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
val token = "qWApQdzxfFvZNRBCWezP"
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
        val context = LocalContext.current
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
                        val apkUrl = mData?.apkPath
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
                                    installApk2(context = context, apkFileName = "gacha_release.apk")
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
        val path = FileUtils.getApkFile(apkFilePath).absolutePath
        val process = Runtime.getRuntime().exec("adb install ${path}")
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
fun installApk2(context: Context, apkFileName: String) {
    val file =FileUtils.getApkFile(apkFileName)
    Log.d(TAG, "file md5 = ${FileUtils.getFileMD5(file)}")
    Log.d(TAG, "file path = ${file.path}")
    Log.d(TAG, "context.packageName = ${context.packageName}")
    //a60d7170e25d7a2c0411362fd5a0072f
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
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
            val output = FileOutputStream(FileUtils.getApkFile(fileName))

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
        val apkUrl = jsonObject.getString("apkPath")
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


data class VersionData(
    val versionCode: Int,
    val versionName: String,
    val updateMessage: String,
    val updateTime: String,
    val apkPath: String
)

