package com.optativa.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.optativa.marketplace.data.Product
import com.optativa.marketplace.data.ProductRepository
import com.optativa.marketplace.logic.CatalogSearch
import com.optativa.marketplace.logic.Pricing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(vm: MarketViewModel, modifier: Modifier = Modifier, readOnly: Boolean = false) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    val base = if (onlyFavorites) vm.favoriteProducts else vm.products
    val results = CatalogSearch.filter(base, vm.query, vm.category)

    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            placeholder = { Text("🔍 Buscar productos…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                .testTag("search")
        )
        if (readOnly) {
            Text(
                "👁️ Vista previa de la tienda tal como la ve un comprador",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!readOnly) {
                item {
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = { onlyFavorites = !onlyFavorites },
                        label = { Text("❤️ Favoritos") },
                        modifier = Modifier.testTag("filter_favorites")
                    )
                }
            }
            item {
                FilterChip(selected = vm.category == null, onClick = { vm.category = null }, label = { Text("Todo") })
            }
            items(ProductRepository.categories) { c ->
                FilterChip(
                    selected = vm.category == c,
                    onClick = { vm.category = if (vm.category == c) null else c },
                    label = { Text(c) },
                    modifier = Modifier.testTag("cat_$c")
                )
            }
        }

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (onlyFavorites && vm.favorites.isEmpty()) "🤍 Aún no tienes favoritos. Toca el corazón de un producto para guardarlo."
                    else "😕 No encontramos productos con esa búsqueda",
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.id }) { p ->
                    ProductCard(
                        product = p,
                        inCart = vm.cart[p.id] ?: 0,
                        isFavorite = vm.isFavorite(p.id),
                        readOnly = readOnly,
                        onClick = { selectedId = p.id },
                        onAdd = { vm.addToCart(p) },
                        onToggleFavorite = { vm.toggleFavorite(p.id) }
                    )
                }
            }
        }
    }

    selectedId?.let { id ->
        vm.productById(id)?.let { p ->
            val inCart = vm.cart[p.id] ?: 0
            AlertDialog(
                onDismissRequest = { selectedId = null },
                title = { Text("${p.emoji} ${p.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.description)
                        Text("Categoría: ${p.category}")
                        Text("${ratingLabel(p.rating)} · Vendedor: ${p.seller}")
                        Text(if (p.stock == 0) "Agotado" else "Disponibles: ${p.stock}")
                        Text(Pricing.format(p.price), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                confirmButton = {
                    if (!readOnly) {
                        Button(onClick = { vm.addToCart(p) }, enabled = inCart < p.stock) {
                            Text(if (inCart > 0) "Agregar otro ($inCart)" else "Agregar al carrito")
                        }
                    }
                },
                dismissButton = {
                    Row {
                        if (!readOnly) {
                            TextButton(onClick = { vm.toggleFavorite(p.id) }) {
                                Text(if (vm.isFavorite(p.id)) "❤️ Guardado" else "🤍 Favorito")
                            }
                        }
                        TextButton(onClick = { selectedId = null }) { Text("Cerrar") }
                    }
                }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    inCart: Int,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    isFavorite: Boolean = false,
    readOnly: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_${product.id}")
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(product.emoji, fontSize = 44.sp)
                if (!readOnly) {
                    Text(
                        if (isFavorite) "❤️" else "🤍",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clickable(onClick = onToggleFavorite)
                            .padding(8.dp)
                            .testTag("fav_${product.id}")
                    )
                }
            }

            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Text(ratingLabel(product.rating), style = MaterialTheme.typography.bodySmall)
                Text(Pricing.format(product.price), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                if (readOnly) {
                    Text(
                        if (product.stock == 0) "Agotado" else "Stock: ${product.stock}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (product.stock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Button(
                        onClick = onAdd,
                        enabled = inCart < product.stock,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                product.stock == 0 -> "Agotado"
                                inCart > 0 -> "En carrito ($inCart) +"
                                else -> "Agregar"
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/** "⭐ 4.5" o "🆕 Nuevo" para productos recién creados por el administrador (sin calificaciones). */
fun ratingLabel(rating: Double): String = if (rating <= 0.0) "🆕 Nuevo" else "⭐ $rating"
