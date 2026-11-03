package com.optativa.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.optativa.marketplace.data.Role
import com.optativa.marketplace.ui.AdminDashboardScreen
import com.optativa.marketplace.ui.AdminProductsScreen
import com.optativa.marketplace.ui.AuthScreen
import com.optativa.marketplace.ui.CartScreen
import com.optativa.marketplace.ui.CatalogScreen
import com.optativa.marketplace.ui.ChatScreen
import com.optativa.marketplace.ui.MarketViewModel
import com.optativa.marketplace.ui.MarketplaceTheme
import com.optativa.marketplace.ui.ProfileScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MarketplaceTheme { MarketApp() } }
    }
}

@Composable
fun MarketApp(vm: MarketViewModel = viewModel()) {
    val user = vm.user
    if (user == null) {
        AuthScreen(vm)
        return
    }

    // Cada rol recuerda su propia pestaña; al cambiar de cuenta se reinicia.
    var tab by rememberSaveable(user.uid) { mutableIntStateOf(0) }

    vm.notice?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.dismissNotice() },
            title = { Text("Registro") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.dismissNotice() }) { Text("Entendido") } }
        )
    }

    val admin = user.role == Role.ADMIN

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (admin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (admin) "🛠️ Marketplace · Admin" else "🛍️ Marketplace",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "Hola, ${user.name} · ${user.role.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
                TextButton(onClick = { vm.logout() }) {
                    Text("Salir", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        bottomBar = {
            NavigationBar {
                if (admin) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("📊") }, label = { Text("Panel") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("📦") }, label = { Text("Productos") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("🏬") }, label = { Text("Tienda") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("👤") }, label = { Text("Perfil") })
                } else {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("🏬") }, label = { Text("Catálogo") })
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = {
                            BadgedBox(badge = { if (vm.cartCount > 0) Badge { Text("${vm.cartCount}") } }) { Text("🛒") }
                        },
                        label = { Text("Carrito") }
                    )
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("💬") }, label = { Text("Asistente") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("👤") }, label = { Text("Perfil") })
                }
            }
        }
    ) { padding ->
        val m = Modifier
            .fillMaxSize()
            .padding(padding)
        if (admin) {
            when (tab) {
                0 -> AdminDashboardScreen(vm, m, onManageProducts = { tab = 1 })
                1 -> AdminProductsScreen(vm, m)
                2 -> CatalogScreen(vm, m, readOnly = true)
                else -> ProfileScreen(vm, m)
            }
        } else {
            when (tab) {
                0 -> CatalogScreen(vm, m)
                1 -> CartScreen(vm, m, onGoShopping = { tab = 0 })
                2 -> ChatScreen(vm, m)
                else -> ProfileScreen(vm, m, onGoShopping = { tab = 0 })
            }
        }
    }
}
