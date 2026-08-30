package com.xiaoquexing.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.sessionStore by preferencesDataStore("xqx_session")

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val deviceId: String,
    val personalSpaceId: String,
    val displayName: String,
    val maskedPhone: String,
)

class TokenStore(private val context: Context) {
    private val access = stringPreferencesKey("access")
    private val refresh = stringPreferencesKey("refresh")
    private val userId = stringPreferencesKey("user_id")
    private val deviceId = stringPreferencesKey("device_id")
    private val spaceId = stringPreferencesKey("personal_space_id")
    private val name = stringPreferencesKey("display_name")
    private val phone = stringPreferencesKey("masked_phone")

    val session: Flow<Session?> = context.sessionStore.data.map { p ->
        val a = p[access] ?: return@map null
        val r = p[refresh] ?: return@map null
        Session(
            accessToken = a,
            refreshToken = r,
            userId = p[userId].orEmpty(),
            deviceId = p[deviceId].orEmpty(),
            personalSpaceId = p[spaceId].orEmpty(),
            displayName = p[name].orEmpty(),
            maskedPhone = p[phone].orEmpty(),
        )
    }

    suspend fun current(): Session? = session.first()

    suspend fun deviceId(): String {
        val existing = context.sessionStore.data.first()[deviceId]
        if (!existing.isNullOrBlank()) return existing
        val id = UUID.randomUUID().toString()
        context.sessionStore.edit { it[deviceId] = id }
        return id
    }

    suspend fun save(tokens: TokenPair, profile: Profile?) {
        context.sessionStore.edit {
            it[access] = tokens.accessToken
            it[refresh] = tokens.refreshToken
            it[userId] = tokens.userId
            if (tokens.deviceId.isNotBlank()) it[deviceId] = tokens.deviceId
            if (profile != null) {
                it[spaceId] = profile.personalSpaceId
                it[name] = profile.displayName
                it[phone] = profile.maskedPhone
            }
        }
    }

    suspend fun saveTokens(tokens: TokenPair) {
        context.sessionStore.edit {
            it[access] = tokens.accessToken
            it[refresh] = tokens.refreshToken
            if (tokens.userId.isNotBlank()) it[userId] = tokens.userId
            if (tokens.deviceId.isNotBlank()) it[deviceId] = tokens.deviceId
        }
    }

    suspend fun bindPersonalSpace(id: String) {
        context.sessionStore.edit { it[spaceId] = id }
    }

    suspend fun clear() {
        val keep = deviceId()
        context.sessionStore.edit {
            it.clear()
            it[deviceId] = keep
        }
    }
}
