package com.optativa.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optativa.marketplace.data.AdminConfig
import com.optativa.marketplace.data.Role
import com.optativa.marketplace.logic.Dates
import com.optativa.marketplace.logic.Initials
import com.optativa.marketplace.logic.Inventory
import com.optativa.marketplace.logic.OrderSummary
import com.optativa.marketplace.logic.Pricing

/** Perfil compartido por ambos roles. El contenido cambia según [MarketViewModel.isAdmin]. */
@Composable
fun ProfileScreen(vm: MarketViewModel, modifier: Modifier = Modifier, onGoShopping: () -> Unit = {}) {
    val user = vm.user ?: return
    val profile = vm.profile
    val admin = vm.isAdmin
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(user.uid) { if (!admin) vm.loadOrders() }

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------- encabezado
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (admin) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(Initials.of(user.name), color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.testTag("profile_name"))
                        Text(user.email, style = MaterialTheme.typography.bodySmall)
                        RoleBadge(user.role)
                        Text(
                            "Miembro desde ${Dates.day(profile?.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // ---------- indicadores
        item {
            if (admin) {
                val stats = Inventory.stats(vm.catalog)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("📦", "${stats.activeProducts}", "Publicados", Modifier.weight(1f))
                    MiniStat("⚠️", "${stats.lowStock.size}", "Stock bajo", Modifier.weight(1f))
                    MiniStat("🙈", "${stats.hiddenProducts}", "Ocultos", Modifier.weight(1f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniStat("🧾", "${vm.orders.size}", "Pedidos", Modifier.weight(1f))
                    MiniStat("❤️", "${vm.favorites.size}", "Favoritos", Modifier.weight(1f))
                    MiniStat("🛒", "${vm.cartCount}", "En carrito", Modifier.weight(1f))
                }
            }
        }

        // ---------- datos personales
        item {
            if (editing) {
                EditProfileCard(vm, onDone = { editing = false })
            } else {
                SectionCard(title = "👤 Mis datos", action = "Editar", onAction = { editing = true }) {
                    InfoRow("Nombre", profile?.name ?: user.name)
                    InfoRow("Correo", user.email)
                    InfoRow("Teléfono", profile?.phone?.ifBlank { null } ?: "Sin registrar")
                    if (!admin) {
                        InfoRow("Dirección", profile?.address?.ifBlank { null } ?: "Sin registrar")
                        InfoRow("Ciudad", profile?.city?.ifBlank { null } ?: "Sin registrar")
                    }
                }
            }
        }

        if (admin) {
            // ---------- permisos del administrador
            item {
                SectionCard(title = "🛠️ Permisos de administrador") {
                    listOf(
                        "Ver el panel con indicadores de inventario",
                        "Crear, editar y eliminar productos",
                        "Ocultar o publicar productos en la tienda",
                        "Reabastecer stock y restaurar el catálogo",
                        "Ver la tienda tal como la ve un comprador"
                    ).forEach { Text("✔️ $it", style = MaterialTheme.typography.bodyMedium) }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        "Correos con rol administrador: ${AdminConfig.ADMIN_EMAILS.joinToString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // ---------- favoritos
            item {
                SectionCard(title = "❤️ Mis favoritos") {
                    if (vm.favoriteProducts.isEmpty()) {
                        Text("Aún no guardas productos. Toca 🤍 en el catálogo.", style = MaterialTheme.typography.bodyMedium)
                    }
                    vm.favoriteProducts.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(p.emoji, fontSize = 24.sp)
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(Pricing.format(p.price), style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = { vm.addToCart(p) },
                                enabled = (vm.cart[p.id] ?: 0) < p.stock,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) { Text("🛒 +") }
                            TextButton(onClick = { vm.toggleFavorite(p.id) }) { Text("✕") }
                        }
                    }
                }
            }

            // ---------- historial de pedidos
            item {
                SectionCard(title = "🧾 Mis pedidos", action = "Actualizar", onAction = { vm.loadOrders() }) {
                    when {
                        vm.ordersLoading -> Text("Cargando pedidos…")
                        vm.ordersError != null -> Text(vm.ordersError ?: "", color = MaterialTheme.colorScheme.error)
                        vm.orders.isEmpty() -> {
                            Text("Todavía no has comprado nada.")
                            Button(onClick = onGoShopping) { Text("Ir al catálogo") }
                        }
                    }
                }
            }
            if (!vm.ordersLoading) {
                items(vm.orders, key = { it.id }) { order -> OrderCard(order) }
            }
        }

        // ---------- seguridad y sesión
        item {
            SectionCard(title = "🔐 Seguridad") {
                OutlinedButton(onClick = { vm.sendPasswordChange() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cambiar contraseña (enlace al correo)")
                }
                Button(
                    onClick = { confirmLogout = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_logout")
                ) { Text("Cerrar sesión") }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tu carrito y tus favoritos quedan guardados en tu cuenta.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; vm.logout() }) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } }
        )
    }

    vm.profileMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.dismissProfileMessage() },
            title = { Text("Perfil") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.dismissProfileMessage() }) { Text("Entendido") } }
        )
    }
}

@Composable
private fun EditProfileCard(vm: MarketViewModel, onDone: () -> Unit) {
    val p = vm.profile
    val admin = vm.isAdmin
    var name by rememberSaveable { mutableStateOf(p?.name ?: vm.user?.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(p?.phone.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(p?.address.orEmpty()) }
    var city by rememberSaveable { mutableStateOf(p?.city.orEmpty()) }

    SectionCard(title = "✏️ Editar perfil") {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("edit_name"))
        OutlinedTextField(
            phone, { phone = it }, label = { Text("Teléfono (opcional)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
        )
        if (!admin) {
            OutlinedTextField(address, { address = it }, label = { Text("Dirección de envío") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(city, { city = it }, label = { Text("Ciudad") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(
                onClick = {
                    vm.updateProfile(name, phone, address, city)
                    onDone()
                },
                enabled = !vm.profileSaving,
                modifier = Modifier
                    .weight(1f)
                    .testTag("edit_save")
            ) { Text("Guardar") }
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummary) {
    var open by rememberSaveable(order.id) { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = !open }
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.code, fontWeight = FontWeight.Bold)
                Text(Pricing.format(order.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "${Dates.dayTime(order.createdAt)} · ${order.units} unidad(es) · ${order.status}",
                style = MaterialTheme.typography.bodySmall
            )
            if (open) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                order.items.forEach {
                    Text("• ${it.quantity} × ${it.name} — ${Pricing.format(it.total)}", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("Toca para ver el detalle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// ---------------------------------------------------------------- piezas reutilizables

@Composable
fun RoleBadge(role: Role) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (role == Role.ADMIN) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (role == Role.ADMIN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            "${role.emoji} ${role.label}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .testTag("role_badge")
        )
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (action != null) TextButton(onClick = onAction) { Text(action) }
                }
            }
            content()
        }
    }
}

@Composable
fun MiniStat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp))
    }
}
