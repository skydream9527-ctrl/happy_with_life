package com.xiaoquexing.app.data.remote

import com.xiaoquexing.app.BuildConfig

class SessionRepository(
    private val api: ApiService,
    private val tokens: TokenStore,
    private val holder: TokenHolder,
) {
    suspend fun sendCode(phone: String) {
        val body = api.smsSend(SmsSendReq(phone = phone.trim(), deviceId = tokens.deviceId()))
        body.error?.let { throw ApiException(it) }
    }

    suspend fun register(account: String, password: String): Session =
        savePair(api.register(PasswordAuthReq(account.trim(), password, tokens.deviceId(), "android", BuildConfig.VERSION_NAME)))

    suspend fun login(account: String, password: String): Session =
        savePair(api.login(PasswordAuthReq(account.trim(), password, tokens.deviceId(), "android", BuildConfig.VERSION_NAME)))

    suspend fun verify(phone: String, code: String): Session {
        val env = api.smsVerify(
            SmsVerifyReq(
                phone = phone.trim(),
                code = code.trim(),
                deviceId = tokens.deviceId(),
                platform = "android",
                appVersion = BuildConfig.VERSION_NAME,
            )
        )
        val pair = env.data ?: throw ApiException(env.error ?: ApiError("AUTH", "登录失败"))
        return persist(pair)
    }

    private suspend fun savePair(env: Envelope<TokenPair>): Session {
        val pair = env.data ?: throw ApiException(env.error ?: ApiError("AUTH", "登录失败"))
        return persist(pair)
    }

    private suspend fun persist(pair: TokenPair): Session {
        tokens.save(pair, null)
        applyHolder(pair.accessToken, pair.deviceId)
        val me = runCatching { api.me().data }.getOrNull()
        tokens.save(pair, me)
        if (me != null) tokens.bindPersonalSpace(me.personalSpaceId)
        return tokens.current() ?: error("session missing after auth")
    }

    suspend fun refresh(): Session? {
        val cur = tokens.current() ?: return null
        val env = api.refresh(RefreshReq(cur.refreshToken, cur.deviceId.ifBlank { tokens.deviceId() }))
        val pair = env.data ?: run {
            tokens.clear()
            throw ApiException(env.error ?: ApiError("AUTH", "登录已过期"))
        }
        tokens.saveTokens(pair)
        applyHolder(pair.accessToken, pair.deviceId)
        return tokens.current()
    }

    suspend fun restore() {
        val cur = tokens.current() ?: return
        applyHolder(cur.accessToken, cur.deviceId)
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        val env = api.changePassword(ChangePasswordReq(oldPassword, newPassword))
        env.error?.let { throw ApiException(it) }
    }

    suspend fun resetPasswordOnDevice(newPassword: String) {
        val env = api.resetPasswordOnDevice(ResetPasswordReq(newPassword))
        env.error?.let { throw ApiException(it) }
    }

    suspend fun logout() {
        val cur = tokens.current()
        if (cur != null) {
            runCatching { api.logout(RefreshReq(cur.refreshToken, cur.deviceId)) }
        }
        holder.accessToken = null
        tokens.clear()
    }

    private fun applyHolder(access: String, deviceId: String) {
        holder.accessToken = access
        if (deviceId.isNotBlank()) holder.deviceId = deviceId
    }
}

class ApiException(val err: ApiError) : Exception(err.message)
