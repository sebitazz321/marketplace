package com.optativa.marketplace.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Perfil

object ProfileValidator {
    fun phone(value: String): String? {
        val t = value.trim()
        if (t.isEmpty()) return null // opcional
        val digits = t.count { it.isDigit() }
        val allowed = t.all { it.isDigit() || it in " +-()" }
        return if (!allowed || digits !in 7..15) "Teléfono no válido (7 a 15 dígitos)." else null
    }

    fun address(value: String): String? {
        val t = value.trim()
        return if (t.isNotEmpty() && t.length < 5) "La dirección es muy corta." else null
    }

    fun city(value: String): String? {
        val t = value.trim()
        return if (t.isNotEmpty() && t.length < 3) "Ciudad no válida." else null
    }
}

object Initials {
    /** "Sebastián Grande Moreno" -> "SG". */
    fun of(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        return parts.take(2).joinToString("") { it.first().uppercase() }
    }
}

object Dates {
    private val LOCALE = Locale("es", "CO")

    fun day(millis: Long?): String =
        millis?.let { SimpleDateFormat("dd/MM/yyyy", LOCALE).format(Date(it)) } ?: "—"

    fun dayTime(millis: Long?): String =
        millis?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", LOCALE).format(Date(it)) } ?: "Procesando…"
}

// ---------------------------------------------------------------- Historial de pedidos

data class OrderItem(val productId: Int, val name: String, val quantity: Int, val price: Double) {
    val total: Double get() = price * quantity
}

data class OrderSummary(
    val id: String,
    val total: Double,
    val items: List<OrderItem>,
    val createdAt: Long?,
    val status: String
) {
    val code: String get() = OrderParser.code(id)
    val units: Int get() = items.sumOf { it.quantity }
}

/** Convierte un documento `orders/{id}` (mapa de Firestore) en [OrderSummary]. Lógica pura y testeable. */
object OrderParser {
    fun code(id: String): String = "PED-" + id.take(6).uppercase()

    fun parse(id: String, data: Map<String, Any?>?, createdAt: Long?): OrderSummary? {
        if (data == null) return null
        val items = (data["items"] as? List<*>).orEmpty().mapNotNull { raw ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            val qty = (m["quantity"] as? Number)?.toInt() ?: return@mapNotNull null
            OrderItem(
                productId = (m["productId"] as? Number)?.toInt() ?: 0,
                name = m["name"] as? String ?: "Producto",
                quantity = qty,
                price = (m["price"] as? Number)?.toDouble() ?: 0.0
            )
        }
        val total = (data["total"] as? Number)?.toDouble() ?: items.sumOf { it.total }
        val status = data["status"] as? String ?: "Confirmado"
        return OrderSummary(id, total, items, createdAt, status)
    }

    /** Más recientes primero; los que aún no tienen fecha del servidor van arriba. */
    fun sort(orders: List<OrderSummary>): List<OrderSummary> =
        orders.sortedByDescending { it.createdAt ?: Long.MAX_VALUE }
}
