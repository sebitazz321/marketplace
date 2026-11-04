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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optativa.marketplace.logic.Pricing

@Composable
fun CartScreen(vm: MarketViewModel, modifier: Modifier = Modifier, onGoShopping: () -> Unit) {
    val lines = vm.cartLines
    val subtotal = Pricing.subtotal(lines)
    val shipping = Pricing.shipping(subtotal)

    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        if (lines.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🛒", fontSize = 64.sp)
                Text("Tu carrito está vacío", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onGoShopping, modifier = Modifier.padding(top = 16.dp)) { Text("Ver productos") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lines, key = { it.product.id }) { line ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text(line.product.emoji, fontSize = 28.sp) }

                            Column(Modifier.weight(1f)) {
                                Text(line.product.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text(Pricing.format(line.product.price), style = MaterialTheme.typography.bodySmall)
                                Text(Pricing.format(line.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalIconButton(
                                        onClick = { vm.decrement(line.product.id) },
                                        modifier = Modifier.size(32.dp).semantics { contentDescription = "Quitar una unidad de ${line.product.name}" }
                                    ) { Text("−") }
                                    Text("${line.quantity}", fontWeight = FontWeight.Bold)
                                    FilledTonalIconButton(
                                        onClick = { vm.addToCart(line.product) },
                                        enabled = line.quantity < line.product.stock,
                                        modifier = Modifier.size(32.dp).semantics { contentDescription = "Agregar una unidad de ${line.product.name}" }
                                    ) { Text("+") }
                                }
                                TextButton(onClick = { vm.remove(line.product.id) }) { Text("Quitar", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }

            // Resumen
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryRow("Subtotal", Pricing.format(subtotal))
                    SummaryRow(
                        "Envío",
                        if (shipping == 0.0) "Gratis 🎉" else Pricing.format(shipping)
                    )
                    if (shipping > 0.0) {
                        Text(
                            "Agrega ${Pricing.format(Pricing.FREE_SHIPPING_FROM - subtotal)} más para envío gratis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    SummaryRow("Total", Pricing.format(subtotal + shipping), bold = true)
                    Button(
                        onClick = { vm.checkout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("checkout")
                    ) { Text("Finalizar compra") }
                }
            }
        }
    }

    vm.lastOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { vm.dismissOrder() },
            title = { Text("✅ ¡Compra realizada!") },
            text = { Text("Tu pedido $order fue registrado (simulación). Te avisaremos cuando salga a envío.") },
            confirmButton = { TextButton(onClick = { vm.dismissOrder(); onGoShopping() }) { Text("Seguir comprando") } }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = if (bold) 18.sp else 14.sp)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = if (bold) 18.sp else 14.sp)
    }
}
