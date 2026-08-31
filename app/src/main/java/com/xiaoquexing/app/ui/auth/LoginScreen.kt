package com.xiaoquexing.app.ui.auth

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
            .verticalScroll(rememberScrollState())
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
            Spacer(Modifier.height(20.dp))
            Text("修改密码", style = MaterialTheme.typography.titleMedium)
            Text("未退出登录的设备可以改密或直接重置，不发短信。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.oldPassword,
                onValueChange = viewModel::onOldPassword,
                label = { Text("原密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.newPassword,
                onValueChange = viewModel::onNewPassword,
                label = { Text("新密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.changePassword() },
                enabled = !ui.verifying,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("确认改密") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.resetOnDevice() },
                enabled = !ui.verifying,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("本机直接重置（不用原密码）") }
        } else {
            Text(
                "用账号和密码注册或登录。账号 3-32 位字母数字或下划线，密码至少 6 位。",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ui.account,
                onValueChange = viewModel::onAccount,
                label = { Text("账号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.password,
                onValueChange = viewModel::onPassword,
                label = { Text("密码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.login() },
                enabled = !ui.verifying,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.verifying && !ui.sending) "登录中…" else "登录并合并本地记录")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.register() },
                enabled = !ui.verifying,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (ui.sending) "注册中…" else "注册新账号")
            }
        }
        androidx.compose.animation.AnimatedVisibility(visible = ui.message != null) {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(ui.message.orEmpty(), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
