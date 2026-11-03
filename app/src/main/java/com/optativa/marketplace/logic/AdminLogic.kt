package com.optativa.marketplace.logic

import com.optativa.marketplace.data.Product
import com.optativa.marketplace.data.ProductRepository
import kotlin.math.max

// ---------------------------------------------------------------- Formulario de producto (Administrador)

/** Lo que el administrador escribe en el formulario (todo como texto, tal cual se digita). */
data class ProductDraft(
    val name: String = "",
    val category: String = ProductRepository.TECH,
    val price: String = "",
    val stock: String = "",
    val emoji: String = "📦",
    val description: String = "",
    val seller: String = ""
) {
    companion object {
        fun from(p: Product) = ProductDraft(
            name = p.name,
            category = p.category,
            price = p.price.toLong().toString(),
            stock = p.stock.toString(),
            emoji = p.emoji,
            description = p.description,
            seller = p.seller
        )
    }
}

object ProductValidator {
    const val MAX_PRICE = 100_000_000.0
    const val MAX_STOCK = 100_000

    /** "189.900", "$189,900", "189900" -> 189900.0. Devuelve null si no es un número. */
    fun parsePrice(text: String): Double? {
        val digits = text.trim().removePrefix("$").replace(".", "").replace(",", "").replace(" ", "")
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
        return digits.toDoubleOrNull()
    }

    fun parseStock(text: String): Int? {
        val t = text.trim()
        if (t.isEmpty() || !t.all { it.isDigit() }) return null
        return t.toIntOrNull()
    }

    /** Devuelve el primer error encontrado o null si el borrador es válido. */
    fun validate(d: ProductDraft, existing: List<Product>, editingId: Int? = null): String? {
        val name = d.name.trim()
        val price = parsePrice(d.price)
        val stock = parseStock(d.stock)
        val duplicated = existing.any {
            it.id != editingId && TextUtil.normalize(it.name.trim()) == TextUtil.normalize(name)
        }
        return when {
            name.length < 3 -> "El nombre debe tener al menos 3 caracteres."
            duplicated -> "Ya existe un producto con ese nombre."
            d.category !in ProductRepository.categories -> "Selecciona una categoría válida."
            price == null || price <= 0.0 -> "Ingresa un precio válido mayor a 0."
            price > MAX_PRICE -> "El precio no puede superar ${Pricing.format(MAX_PRICE)}."
            stock == null -> "Ingresa el stock como número entero (0 o más)."
            stock > MAX_STOCK -> "El stock no puede superar $MAX_STOCK unidades."
            d.description.trim().length < 10 -> "La descripción debe tener al menos 10 caracteres."
            d.seller.trim().length < 2 -> "Ingresa el nombre del vendedor."
            else -> null
        }
    }

    /** Convierte un borrador ya validado en producto. */
    fun toProduct(d: ProductDraft, id: Int, rating: Double = 0.0, active: Boolean = true) = Product(
        id = id,
        name = d.name.trim(),
        category = d.category,
        price = parsePrice(d.price) ?: 0.0,
        emoji = d.emoji.trim().ifEmpty { "📦" },
        description = d.description.trim(),
        rating = rating,
        stock = parseStock(d.stock) ?: 0,
        seller = d.seller.trim(),
        active = active
    )
}

// ---------------------------------------------------------------- Operaciones sobre el catálogo

object CatalogOps {
    fun nextId(products: List<Product>): Int = (products.maxOfOrNull { it.id } ?: 0) + 1

    /** Inserta o reemplaza (por id). */
    fun upsert(products: List<Product>, p: Product): List<Product> =
        if (products.any { it.id == p.id }) products.map { if (it.id == p.id) p else it } else products + p

    fun delete(products: List<Product>, id: Int): List<Product> = products.filterNot { it.id == id }

    fun setActive(products: List<Product>, id: Int, active: Boolean): List<Product> =
        products.map { if (it.id == id) it.copy(active = active) else it }

    /** Suma (o resta) unidades sin bajar de 0. */
    fun adjustStock(products: List<Product>, id: Int, delta: Int): List<Product> =
        products.map { if (it.id == id) it.copy(stock = (it.stock + delta).coerceIn(0, ProductValidator.MAX_STOCK)) else it }

    /** Descuenta del inventario las unidades compradas (carrito idProducto -> cantidad). */
    fun applyOrder(products: List<Product>, cart: Map<Int, Int>): List<Product> =
        products.map { p -> cart[p.id]?.let { qty -> p.copy(stock = max(0, p.stock - qty)) } ?: p }

    /** Lo que ve el comprador: solo productos activos. */
    fun visible(products: List<Product>): List<Product> = products.filter { it.active }
}

// ---------------------------------------------------------------- Indicadores del panel

data class CategoryStat(val category: String, val products: Int, val units: Int, val value: Double)

data class InventoryStats(
    val totalProducts: Int,
    val activeProducts: Int,
    val hiddenProducts: Int,
    val totalUnits: Int,
    val inventoryValue: Double,
    val outOfStock: Int,
    val lowStock: List<Product>,
    val byCategory: List<CategoryStat>
)

object Inventory {
    const val LOW_STOCK = 10

    fun stats(products: List<Product>): InventoryStats {
        val categories = (ProductRepository.categories + products.map { it.category }).distinct()
        return InventoryStats(
            totalProducts = products.size,
            activeProducts = products.count { it.active },
            hiddenProducts = products.count { !it.active },
            totalUnits = products.sumOf { it.stock },
            inventoryValue = products.sumOf { it.price * it.stock },
            outOfStock = products.count { it.stock == 0 },
            lowStock = products.filter { it.stock in 0..LOW_STOCK }.sortedBy { it.stock },
            byCategory = categories.map { c ->
                val inCat = products.filter { it.category == c }
                CategoryStat(c, inCat.size, inCat.sumOf { it.stock }, inCat.sumOf { it.price * it.stock })
            }
        )
    }
}
