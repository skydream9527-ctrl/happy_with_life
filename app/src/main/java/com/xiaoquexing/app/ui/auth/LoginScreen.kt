package com.xiaoquexing.app.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        TopAppBar(
            title = { Text("登录") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
        )
        val session = ui.session
        if (session != null) {
            Text("${session.displayName.ifBlank { "已登录" }} · ${session.maskedPhone}")
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { viewModel.syncNow() },
                enabled = !ui.syncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.syncing) "同步中…" else "立即同步本地记录")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("退出登录")
            }
        } else {
            Text(
                "开发环境验证码固定为 123456。正式短信尚未接入。",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ui.phone,
                onValueChange = viewModel::onPhone,
                label = { Text("手机号") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.code,
                onValueChange = viewModel::onCode,
                label = { Text("验证码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { viewModel.sendCode() },
                enabled = !ui.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.sending) "发送中…" else "获取验证码")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.verify() },
                enabled = !ui.verifying,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.verifying) "登录中…" else "登录并合并本地记录")
            }
        }
        ui.message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}
