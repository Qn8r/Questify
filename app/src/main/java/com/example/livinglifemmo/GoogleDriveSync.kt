@file:Suppress("DEPRECATION")

package com.example.livinglifemmo

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GoogleDriveSync {
    private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val BACKUP_FILE_NAME = "questify_backup.enc"
    private val gson = Gson()

    fun signInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun signOut(context: Context) {
        signInClient(context).signOut()
    }

    fun resolveSignInResult(data: Intent?): Result<GoogleSignInAccount?> {
        return runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        }
    }

    fun resolveSignInStatusCode(error: Throwable): Int? {
        return (error as? ApiException)?.statusCode
    }

    private suspend fun accessToken(context: Context, account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        val androidAccount = account.account ?: return@withContext null
        val scope = "oauth2:$DRIVE_SCOPE"
        runCatching {
            GoogleAuthUtil.getToken(context, androidAccount, scope)
        }.getOrNull()
    }

    private fun openConnection(url: String, method: String, token: String, contentType: String = "application/json"): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", contentType)
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    private suspend fun findBackupFileId(token: String): String? = withContext(Dispatchers.IO) {
        val q = "name='$BACKUP_FILE_NAME' and 'appDataFolder' in parents and trashed=false"
        val encodedQ = URLEncoder.encode(q, StandardCharsets.UTF_8.toString())
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$encodedQ&fields=files(id,name)"
        runCatching {
            val conn = openConnection(url, "GET", token)
            val code = conn.responseCode
            val body = conn.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299 || body.isBlank()) return@runCatching null
            val parsed = gson.fromJson(body, DriveFilesResponse::class.java)
            parsed.files.firstOrNull()?.id
        }.getOrNull()
    }

    suspend fun uploadBackup(context: Context, payload: String): Boolean = withContext(Dispatchers.IO) {
        if (payload.isBlank()) return@withContext false
        val account = getLastSignedInAccount(context) ?: return@withContext false
        val token = accessToken(context, account) ?: return@withContext false
        val existingId = findBackupFileId(token)

        if (existingId != null) {
            val url = "https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media"
            runCatching {
                val conn = openConnection(url, "PATCH", token, "text/plain; charset=utf-8")
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            }.getOrDefault(false)
        } else {
            val boundary = "questify_${System.currentTimeMillis()}"
            val metadata = """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"]}"""
            val body = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata)
                append("\r\n--$boundary\r\n")
                append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
                append(payload)
                append("\r\n--$boundary--")
            }
            val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            runCatching {
                val conn = openConnection(url, "POST", token, "multipart/related; boundary=$boundary")
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            }.getOrDefault(false)
        }
    }

    suspend fun downloadBackup(context: Context): String? = withContext(Dispatchers.IO) {
        val account = getLastSignedInAccount(context) ?: return@withContext null
        val token = accessToken(context, account) ?: return@withContext null
        val fileId = findBackupFileId(token) ?: return@withContext null
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        runCatching {
            val conn = openConnection(url, "GET", token, "text/plain; charset=utf-8")
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty() else ""
            conn.disconnect()
            if (code in 200..299) body else null
        }.getOrNull()
    }

    private data class DriveFilesResponse(val files: List<DriveFileItem> = emptyList())
    private data class DriveFileItem(val id: String)
}
