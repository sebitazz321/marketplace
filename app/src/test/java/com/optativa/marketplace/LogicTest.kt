package com.optativa.marketplace

import com.optativa.marketplace.data.ProductRepository
import com.optativa.marketplace.logic.CartLine
import com.optativa.marketplace.logic.CartOps
import com.optativa.marketplace.logic.CartSerializer
import com.optativa.marketplace.logic.CatalogSearch
import com.optativa.marketplace.logic.AuthErrors
import com.optativa.marketplace.logic.Pricing
import com.optativa.marketplace.logic.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private val all = ProductRepository.products

    // ---- búsqueda
    @Test fun search_ignoresAccentsAndCase() {
        val r = CatalogSearch.filter(all, "AUDIFONOS", null)
        assertTrue(r.any { it.name.contains("Audífonos") })
    }

    @Test fun search_byCategory() {
        val r = CatalogSearch.filter(all, "", ProductRepository.HOME)
        assertTrue(r.isNotEmpty() && r.all { it.category == ProductRepository.HOME })
    }

    @Test fun search_noResults() = assertTrue(CatalogSearch.filter(all, "xyzabc", null).isEmpty())

    @Test fun search_allTokensMustMatch() {
        assertEquals(1, CatalogSearch.filter(all, "silla ergonomica", null).size)
    }

    // ---- carrito
    @Test fun cart_addIncrementsAndRespectsStock() {
        val p = all.first { it.id == 6 } // stock 7
        var cart = emptyMap<Int, Int>()
        repeat(10) { cart = CartOps.add(cart, p) }
        assertEquals(p.stock, cart[p.id])
    }

    @Test fun cart_decrementRemovesAtZero() {
        var cart = mapOf(1 to 2)
        cart = CartOps.decrement(cart, 1)
        assertEquals(1, cart[1])
        cart = CartOps.decrement(cart, 1)
        assertTrue(cart.isEmpty())
    }

    @Test fun cart_countAndLines() {
        val cart = mapOf(1 to 2, 3 to 1, 999 to 5) // 999 no existe
        assertEquals(8, CartOps.count(cart))
        assertEquals(2, CartOps.lines(cart, all).size)
    }

    @Test fun serializer_roundTrip() {
        val cart = linkedMapOf(1 to 2, 5 to 1)
        assertEquals(cart, CartSerializer.decode(CartSerializer.encode(cart)))
    }

    @Test fun serializer_toleratesGarbage() {
        assertEquals(mapOf(2 to 3), CartSerializer.decode("2:3,abc,4:x,5:0,:"))
        assertTrue(CartSerializer.decode(null).isEmpty())
    }

    // ---- precios
    @Test fun pricing_shippingRules() {
        assertEquals(0.0, Pricing.shipping(0.0), 0.0)
        assertEquals(Pricing.SHIPPING_COST, Pricing.shipping(50_000.0), 0.0)
        assertEquals(0.0, Pricing.shipping(200_000.0), 0.0)
    }

    @Test fun pricing_totals() {
        val lines = listOf(CartLine(all.first { it.id == 3 }, 2)) // 2 x 59.900
        assertEquals(119_800.0, Pricing.subtotal(lines), 0.0)
        assertEquals(119_800.0 + Pricing.SHIPPING_COST, Pricing.total(lines), 0.0)
    }

    @Test fun pricing_format() {
        assertEquals("$189.900", Pricing.format(189_900.0))
        assertEquals("$1.899.900", Pricing.format(1_899_900.0))
        assertEquals("$0", Pricing.format(0.0))
    }

    // ---- autenticación
    @Test fun validators() {
        assertNull(Validators.email("ana@correo.com"))
        assertNotNull(Validators.email("ana@correo"))
        assertNull(Validators.password("abc123"))
        assertNotNull(Validators.password("abc"))
        assertNotNull(Validators.password("abcdefgh"))
        assertNotNull(Validators.name("A"))
    }

    @Test fun authErrors_translateFirebaseCodes() {
        assertEquals("Ya existe una cuenta con ese correo.", AuthErrors.message("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals("Ya existe una cuenta con ese correo.", AuthErrors.message("email-already-in-use"))
        assertEquals("Correo o contraseña incorrectos.", AuthErrors.message("ERROR_INVALID_CREDENTIAL"))
        assertEquals("Correo o contraseña incorrectos.", AuthErrors.message("invalid-credential"))
        assertEquals("Correo o contraseña incorrectos.", AuthErrors.message("ERROR_WRONG_PASSWORD"))
        assertEquals("Correo o contraseña incorrectos.", AuthErrors.message("wrong-password"))
        assertEquals("El inicio de sesión con correo y contraseña no está habilitado en Firebase Console.", AuthErrors.message("operation-not-allowed"))
        assertTrue(AuthErrors.message(AuthErrors.NETWORK).contains("Sin conexión"))
        assertTrue(AuthErrors.message(null).contains("No se pudo"))
    }

    @Test fun firestore_cartRoundTrip() {
        val cart = linkedMapOf(1 to 2, 5 to 1)
        val remote: Map<String, Any> = CartSerializer.toFirestore(cart).mapValues { it.value.toLong() } // Firestore devuelve Long
        assertEquals(cart, CartSerializer.fromFirestore(remote))
    }

    @Test fun firestore_ignoresInvalidEntries() {
        val raw = mapOf("2" to 3L, "x" to 1L, "4" to 0L, "6" to "texto")
        assertEquals(mapOf(2 to 3), CartSerializer.fromFirestore(raw))
        assertTrue(CartSerializer.fromFirestore(null).isEmpty())
    }
}
