package com.optativa.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optativa.marketplace.data.Product
import com.optativa.marketplace.data.ProductRepository
import com.optativa.marketplace.logic.CatalogOps
import com.optativa.marketplace.logic.CatalogSearch
import com.optativa.marketplace.logic.Inventory
import com.optativa.marketplace.logic.Pricing
import com.optativa.marketplace.logic.ProductDraft
import com.optativa.marketplace.logic.ProductValidator

// ======================================================================== Panel (dashboard)

@Composable
fun AdminDashboardScreen(vm: MarketViewModel, modifier: Modifier = Modifier, onManageProducts: () -> Unit) {
    val stats = Inventory.stats(vm.catalog)
    val maxValue = stats.byCategory.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Panel de administración", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text("Resumen del inventario del marketplace", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("📦", "${stats.totalProducts}", "Productos", Modifier.weight(1f))
                MiniStat("✅", "${stats.activeProducts}", "Publicados", Modifier.weight(1f))
                MiniStat("🙈", "${stats.hiddenProducts}", "Ocultos", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("🔢", "${stats.totalUnits}", "Unidades", Modifier.weight(1f))
                MiniStat("⚠️", "${stats.lowStock.size}", "Stock bajo", Modifier.weight(1f))
                MiniStat("⛔", "${stats.outOfStock}", "Agotados", Modifier.weight(1f))
            }
        }
        item {
            SectionCard(title = "💰 Valor del inventario") {
                Text(Pricing.format(stats.inventoryValue), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                Text("Precio × unidades disponibles de todos los productos.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SectionCard(title = "📊 Valor por categoría") {
                stats.byCategory.forEach { c ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${c.category} (${c.products})", style = MaterialTheme.typography.bodyMedium)
                            Text(Pricing.format(c.value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth((c.value / maxValue).toFloat().coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
        item {
            SectionCard(title = "⚠️ Stock bajo (≤ ${Inventory.LOW_STOCK} unidades)", action = "Gestionar", onAction = onManageProducts) {
                if (stats.lowStock.isEmpty()) Text("Todo el inventario está por encima del mínimo. 👌")
                stats.lowStock.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(p.emoji, fontSize = 22.sp)
                        Column(Modifier.weight(1f)) {
                            Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (p.stock == 0) "Agotado" else "${p.stock} disponibles",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (p.stock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                        FilledTonalButton(
                            onClick = { vm.restock(p.id, 10) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("restock_${p.id}")
                        ) { Text("+10") }
                    }
                }
            }
        }
        item {
            SectionCard(title = "ℹ️ Acerca de los datos") {
                Text(
                    "El catálogo se administra en este dispositivo. Usuarios, carritos, favoritos y pedidos " +
                        "se siguen guardando en Firebase con las mismas reglas de seguridad del proyecto.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    AdminMessageDialog(vm)
}

// ======================================================================== Gestión de productos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(vm: MarketViewModel, modifier: Modifier = Modifier) {
    var search by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val results = CatalogSearch.filter(vm.catalog, search, null).sortedBy { it.id }

    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("🔍 Buscar en el inventario…") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_search")
                )
                TextButton(onClick = { confirmReset = true }) { Text("Restaurar") }
            }
            Text(
                "${results.size} de ${vm.catalog.size} productos · el interruptor publica u oculta",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.id }) { p ->
                    AdminProductRow(
                        p = p,
                        onEdit = { editingId = p.id },
                        onDelete = { deletingId = p.id },
                        onToggle = { vm.setProductActive(p.id, it) },
                        onRestock = { vm.restock(p.id, it) }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("admin_add")
        ) { Text("＋ Nuevo", modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onPrimary) }
    }

    if (creating) {
        ProductFormDialog(
            title = "Nuevo producto",
            initial = ProductDraft(),
            existing = vm.catalog,
            editingId = null,
            onDismiss = { creating = false },
            onSave = { draft ->
                vm.saveProduct(ProductValidator.toProduct(draft, CatalogOps.nextId(vm.catalog)))
                creating = false
            }
        )
    }

    editingId?.let { id ->
        vm.productById(id)?.let { p ->
            ProductFormDialog(
                title = "Editar producto #${p.id}",
                initial = ProductDraft.from(p),
                existing = vm.catalog,
                editingId = p.id,
                onDismiss = { editingId = null },
                onSave = { draft ->
                    vm.saveProduct(ProductValidator.toProduct(draft, p.id, p.rating, p.active))
                    editingId = null
                }
            )
        }
    }

    deletingId?.let { id ->
        val p = vm.productById(id)
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("¿Eliminar producto?") },
            text = { Text("«${p?.name}» se quitará del catálogo y de los carritos. Si solo quieres que no se vea, usa el interruptor para ocultarlo.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteProduct(id); deletingId = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Cancelar") } }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Restaurar catálogo?") },
            text = { Text("Se descartan los cambios y se vuelve a los ${ProductRepository.products.size} productos originales con su stock inicial.") },
            confirmButton = { TextButton(onClick = { vm.resetCatalog(); confirmReset = false }) { Text("Restaurar") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } }
        )
    }

    AdminMessageDialog(vm)
}

@Composable
private fun AdminProductRow(
    p: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRestock: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (p.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_product_${p.id}")
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(p.emoji, fontSize = 24.sp) }
                Column(Modifier.weight(1f)) {
                    Text("#${p.id} · ${p.name}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${p.category} · ${Pricing.format(p.price)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        when {
                            !p.active -> "Oculto · stock ${p.stock}"
                            p.stock == 0 -> "Agotado"
                            p.stock <= Inventory.LOW_STOCK -> "Stock bajo: ${p.stock}"
                            else -> "Stock: ${p.stock}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            p.stock == 0 -> MaterialTheme.colorScheme.error
                            p.stock <= Inventory.LOW_STOCK -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Switch(checked = p.active, onCheckedChange = onToggle, modifier = Modifier.testTag("toggle_${p.id}"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onRestock(-1) }, enabled = p.stock > 0, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("−1") }
                OutlinedButton(onClick = { onRestock(1) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+1") }
                OutlinedButton(onClick = { onRestock(10) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+10") }
                Box(Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("🗑️") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormDialog(
    title: String,
    initial: ProductDraft,
    existing: List<Product>,
    editingId: Int?,
    onDismiss: () -> Unit,
    onSave: (ProductDraft) -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    val emojis = listOf("📦", "🎧", "💻", "📱", "👕", "👟", "👜", "🏠", "☕", "🪑", "⚽", "🚴", "🧘", "💄", "🌸", "🎁")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    draft.name, { draft = draft.copy(name = it); error = null },
                    label = { Text("Nombre") }, singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_name")
                )
                Text("Categoría", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ProductRepository.categories) { c ->
                        FilterChip(selected = draft.category == c, onClick = { draft = draft.copy(category = c) }, label = { Text(c) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        draft.price, { draft = draft.copy(price = it); error = null },
                        label = { Text("Precio (COP)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("form_price")
                    )
                    OutlinedTextField(
                        draft.stock, { draft = draft.copy(stock = it); error = null },
                        label = { Text("Stock") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("form_stock")
                    )
                }
                Text("Ícono", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(emojis) { e ->
                        FilterChip(selected = draft.emoji == e, onClick = { draft = draft.copy(emoji = e) }, label = { Text(e) })
                    }
                }
                OutlinedTextField(
                    draft.description, { draft = draft.copy(description = it); error = null },
                    label = { Text("Descripción") }, minLines = 2, maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_description")
                )
                OutlinedTextField(
                    draft.seller, { draft = draft.copy(seller = it); error = null },
                    label = { Text("Vendedor / marca") }, singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_seller")
                )
                ProductValidator.parsePrice(draft.price)?.let {
                    Text("Vista previa: ${draft.emoji} ${Pricing.format(it)}", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("form_error")) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    error = ProductValidator.validate(draft, existing, editingId)
                    if (error == null) onSave(draft)
                },
                modifier = Modifier.testTag("form_save")
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AdminMessageDialog(vm: MarketViewModel) {
    vm.adminMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.dismissAdminMessage() },
            title = { Text("Administración") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.dismissAdminMessage() }) { Text("Entendido") } }
        )
    }
}
