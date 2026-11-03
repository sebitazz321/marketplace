package com.optativa.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(vm: MarketViewModel) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(32.dp))
        Text("🛍️", fontSize = 64.sp)
        Text("Marketplace", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(
            if (registering) "Crea tu cuenta" else "Inicia sesión para comprar",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(24.dp))

        if (registering) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; vm.clearMessages() },
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !vm.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_name")
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; vm.clearMessages() },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !vm.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email")
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; vm.clearMessages() },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !vm.loading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password")
        )

        vm.authError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("auth_error"))
        }
        vm.authInfo?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("auth_info"))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (registering) vm.register(name, email, password) else vm.login(email, password)
            },
            enabled = !vm.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auth_submit")
        ) { Text(if (vm.loading) "Cargando…" else if (registering) "Crear cuenta" else "Entrar") }

        if (!registering) {
            TextButton(onClick = { vm.resetPassword(email) }, enabled = !vm.loading) {
                Text("¿Olvidaste tu contraseña?")
            }
        }
        TextButton(onClick = { registering = !registering; vm.clearMessages() }, enabled = !vm.loading) {
            Text(if (registering) "¿Ya tienes cuenta? Inicia sesión" else "¿Eres nuevo? Regístrate")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "🛍️ Compradores: regístrate con cualquier correo.\n🛠️ Administradores: entra con tu correo autorizado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
