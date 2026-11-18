package ru.dev.gitnews.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ru.dev.gitnews.domain.model.AuthProvider
import java.security.MessageDigest
import java.util.UUID

/**
 * Secure Hardware-Backed Token & Credential Storage
 * Enforces AES-256 GCM encryption via Android KeyStore with robust fallback to MODE_PRIVATE.
 */
class SecureTokenStorage(private val context: Context) {

    private val prefs: SharedPreferences = createSafeSharedPreferences(context)

    private fun createSafeSharedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences init failed, clearing corrupted prefs and using fallback", e)
            try {
                context.deleteSharedPreferences(PREFS_FILENAME)
            } catch (_: Exception) {}

            try {
                context.getSharedPreferences(FALLBACK_PREFS_FILENAME, Context.MODE_PRIVATE)
            } catch (_: Exception) {
                context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
            }
        }
    }

    fun saveToken(token: String) {
        try {
            prefs.edit()
                .putString(KEY_AUTH_TOKEN, token.trim())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "saveToken failed", e)
            handleCorruption()
        }
    }

    fun getToken(): String? {
        return try {
            val token = prefs.getString(KEY_AUTH_TOKEN, null)
            if (!token.isNullOrBlank()) token else null
        } catch (e: Exception) {
            Log.e(TAG, "getToken failed", e)
            handleCorruption()
            null
        }
    }

    fun saveSession(
        provider: AuthProvider,
        login: String,
        name: String?,
        email: String?,
        avatarUrl: String?,
        token: String?,
        githubToken: String? = null
    ) {
        try {
            prefs.edit()
                .putString(KEY_AUTH_PROVIDER, provider.name)
                .putString(KEY_USER_LOGIN, login)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_AVATAR, avatarUrl)
                .putString(KEY_AUTH_TOKEN, token?.trim())
                .putString(KEY_GITHUB_TOKEN, githubToken?.trim())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "saveSession failed", e)
            handleCorruption()
        }
    }

    fun getAuthProvider(): AuthProvider {
        return try {
            val name = prefs.getString(KEY_AUTH_PROVIDER, null)
            if (name != null) {
                AuthProvider.valueOf(name)
            } else {
                val token = getToken()
                when {
                    token?.startsWith("google:") == true -> AuthProvider.GOOGLE
                    token?.startsWith("email:") == true -> AuthProvider.EMAIL
                    else -> AuthProvider.GITHUB
                }
            }
        } catch (_: Exception) {
            AuthProvider.GITHUB
        }
    }

    fun getUserEmail(): String? {
        return try {
            prefs.getString(KEY_USER_EMAIL, null)
        } catch (_: Exception) {
            null
        }
    }

    fun saveGitHubToken(token: String?) {
        try {
            prefs.edit()
                .putString(KEY_GITHUB_TOKEN, token?.trim())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "saveGitHubToken failed", e)
        }
    }

    fun getGitHubToken(): String? {
        return try {
            val token = prefs.getString(KEY_GITHUB_TOKEN, null)
            if (!token.isNullOrBlank()) token else null
        } catch (_: Exception) {
            null
        }
    }

    fun registerEmailUser(email: String, password: String, displayName: String?): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val existingHash = prefs.getString(KEY_PREFIX_USER_PWD + cleanEmail, null)
            if (!existingHash.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Пользователь с таким email уже зарегистрирован"))
            }

            val salt = UUID.randomUUID().toString()
            val hash = hashPassword(password, salt)

            prefs.edit()
                .putString(KEY_PREFIX_USER_PWD + cleanEmail, hash)
                .putString(KEY_PREFIX_USER_SALT + cleanEmail, salt)
                .putString(KEY_PREFIX_USER_NAME + cleanEmail, displayName)
                .apply()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loginEmailUser(email: String, password: String): Result<String?> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val storedHash = prefs.getString(KEY_PREFIX_USER_PWD + cleanEmail, null)
            val storedSalt = prefs.getString(KEY_PREFIX_USER_SALT + cleanEmail, null)

            if (storedHash.isNullOrBlank() || storedSalt.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Аккаунт не найден. Проверьте email или зарегистрируйтесь."))
            }

            val calculatedHash = hashPassword(password, storedSalt)
            if (calculatedHash != storedHash) {
                return Result.failure(IllegalArgumentException("Неверный пароль"))
            }

            val storedName = prefs.getString(KEY_PREFIX_USER_NAME + cleanEmail, null)
            Result.success(storedName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + password).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun clearToken() {
        try {
            prefs.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_AUTH_PROVIDER)
                .remove(KEY_USER_LOGIN)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_AVATAR)
                .remove(KEY_GITHUB_TOKEN)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "clearToken failed", e)
            handleCorruption()
        }
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun saveUser(login: String, name: String?, avatarUrl: String?) {
        try {
            prefs.edit()
                .putString(KEY_USER_LOGIN, login)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_AVATAR, avatarUrl)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "saveUser failed", e)
            handleCorruption()
        }
    }

    fun getUserLogin(): String? {
        return try {
            prefs.getString(KEY_USER_LOGIN, null)
        } catch (e: Exception) {
            Log.e(TAG, "getUserLogin failed", e)
            null
        }
    }

    fun getUserName(): String? {
        return try {
            prefs.getString(KEY_USER_NAME, null)
        } catch (e: Exception) {
            Log.e(TAG, "getUserName failed", e)
            null
        }
    }

    fun getUserAvatar(): String? {
        return try {
            prefs.getString(KEY_USER_AVATAR, null)
        } catch (e: Exception) {
            Log.e(TAG, "getUserAvatar failed", e)
            null
        }
    }

    fun isReleaseNotificationsEnabled(): Boolean {
        return try {
            prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        } catch (e: Exception) {
            true
        }
    }

    fun setReleaseNotificationsEnabled(enabled: Boolean) {
        try {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        } catch (e: Exception) {
            Log.e(TAG, "setReleaseNotificationsEnabled failed", e)
        }
    }

    private fun handleCorruption() {
        try {
            context.deleteSharedPreferences(PREFS_FILENAME)
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "SecureTokenStorage"
        private const val PREFS_FILENAME = "gitnews_secure_auth"
        private const val FALLBACK_PREFS_FILENAME = "gitnews_auth_prefs"
        private const val KEY_AUTH_TOKEN = "github_access_token"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_USER_LOGIN = "github_user_login"
        private const val KEY_USER_NAME = "github_user_name"
        private const val KEY_USER_EMAIL = "github_user_email"
        private const val KEY_USER_AVATAR = "github_user_avatar"
        private const val KEY_GITHUB_TOKEN = "auth_github_token"
        private const val KEY_NOTIFICATIONS_ENABLED = "release_notifications_enabled"

        private const val KEY_PREFIX_USER_PWD = "usr_pwd_"
        private const val KEY_PREFIX_USER_SALT = "usr_slt_"
        private const val KEY_PREFIX_USER_NAME = "usr_nam_"
    }
}
