package com.optativa.marketplace.logic

import com.optativa.marketplace.data.Product
import java.text.Normalizer
import java.util.Locale

// ---------------------------------------------------------------- Texto

object TextUtil {
    /** Minúsculas y sin tildes: "Audífonos" -> "audifonos". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

// ---------------------------------------------------------------- Búsqueda

object CatalogSearch {
    /** Filtra por categoría (opcional) y por texto: todas las palabras deben aparecer. */
    fun filter(products: List<Product>, query: String, category: String?): List<Product> {
        val tokens = TextUtil.normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }
        return products.filter { p ->
            val inCategory = category == null || p.category == category
            val haystack = TextUtil.normalize("${p.name} ${p.category} ${p.description}")
            inCategory && tokens.all { haystack.contains(it) }
        }
    }
}

// ---------------------------------------------------------------- Carrito y precios

data class CartLine(val product: Product, val quantity: Int) {
    val total: Double get() = product.price * quantity
}

object Pricing {
    const val FREE_SHIPPING_FROM = 200_000.0
    const val SHIPPING_COST = 12_000.0

    fun subtotal(lines: List<CartLine>): Double = lines.sumOf { it.total }

    fun shipping(subtotal: Double): Double =
        if (subtotal <= 0.0 || subtotal >= FREE_SHIPPING_FROM) 0.0 else SHIPPING_COST

    fun total(lines: List<CartLine>): Double {
        val s = subtotal(lines)
        return s + shipping(s)
    }

    /** 189900.0 -> "$189.900" (formato colombiano, independiente del idioma del dispositivo). */
    fun format(value: Double): String =
        "$" + String.format(Locale.US, "%,d", value.toLong()).replace(',', '.')
}

/** Operaciones puras sobre el carrito (mapa idProducto -> cantidad). */
object CartOps {
    fun add(cart: Map<Int, Int>, p: Product): Map<Int, Int> {
        val current = cart[p.id] ?: 0
        return if (current >= p.stock) cart else cart + (p.id to current + 1)
    }

    fun decrement(cart: Map<Int, Int>, id: Int): Map<Int, Int> {
        val current = cart[id] ?: return cart
        return if (current <= 1) cart - id else cart + (id to current - 1)
    }

    fun remove(cart: Map<Int, Int>, id: Int): Map<Int, Int> = cart - id

    fun count(cart: Map<Int, Int>): Int = cart.values.sum()

    fun lines(cart: Map<Int, Int>, products: List<Product>): List<CartLine> =
        cart.mapNotNull { (id, qty) -> products.firstOrNull { it.id == id }?.let { CartLine(it, qty) } }
}

/** Formatos de persistencia del carrito: texto "1:2,5:1" y mapa para Firestore {"1": 2, "5": 1}. */
object CartSerializer {
    fun toFirestore(cart: Map<Int, Int>): Map<String, Int> = cart.mapKeys { it.key.toString() }

    /** Firestore devuelve los números como Long; se ignoran entradas inválidas. */
    fun fromFirestore(raw: Map<*, *>?): Map<Int, Int> {
        if (raw == null) return emptyMap()
        val result = linkedMapOf<Int, Int>()
        raw.forEach { (k, v) ->
            val id = k?.toString()?.toIntOrNull()
            val qty = (v as? Number)?.toInt()
            if (id != null && qty != null && qty > 0) result[id] = qty
        }
        return result
    }

    fun encode(cart: Map<Int, Int>): String = cart.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decode(text: String?): Map<Int, Int> {
        if (text.isNullOrBlank()) return emptyMap()
        val result = linkedMapOf<Int, Int>()
        text.split(",").forEach { part ->
            val pieces = part.split(":")
            val id = pieces.getOrNull(0)?.toIntOrNull()
            val qty = pieces.getOrNull(1)?.toIntOrNull()
            if (id != null && qty != null && qty > 0) result[id] = qty
        }
        return result
    }
}

// ---------------------------------------------------------------- Autenticación

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun name(value: String): String? =
        if (value.trim().length < 2) "Ingresa tu nombre (mínimo 2 letras)." else null

    fun email(value: String): String? =
        if (!EMAIL.matches(value.trim())) "Correo electrónico no válido." else null

    fun password(value: String): String? = when {
        value.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
        !value.any { it.isLetter() } || !value.any { it.isDigit() } -> "Usa letras y números en la contraseña."
        else -> null
    }
}

/** Traduce los códigos de error de Firebase Authentication a mensajes en español. */
object AuthErrors {
    const val NETWORK = "NETWORK"

    fun message(code: String?): String {
        if (code.isNullOrBlank()) return "No se pudo completar la operación. Inténtalo de nuevo."
        val normalized = code.uppercase()
            .removePrefix("ERROR_")
            .replace("-", "_")
        return when (normalized) {
            "EMAIL_ALREADY_IN_USE", "EMAIL_EXISTS" -> "Ya existe una cuenta con ese correo."
            "INVALID_EMAIL" -> "Correo electrónico no válido."
            "WEAK_PASSWORD" -> "La contraseña es muy débil (mínimo 6 caracteres)."
            "WRONG_PASSWORD", "USER_NOT_FOUND", "INVALID_CREDENTIAL", "INVALID_LOGIN_CREDENTIALS" -> "Correo o contraseña incorrectos."
            "USER_DISABLED" -> "Esta cuenta fue deshabilitada."
            "TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera unos minutos e inténtalo de nuevo."
            "OPERATION_NOT_ALLOWED" -> "El inicio de sesión con correo y contraseña no está habilitado en Firebase Console."
            NETWORK, "NETWORK_REQUEST_FAILED" -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
            else -> "No se pudo completar la operación ($code). Inténtalo de nuevo."
        }
    }
}
