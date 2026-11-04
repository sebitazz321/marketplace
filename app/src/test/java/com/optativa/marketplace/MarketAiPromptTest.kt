package com.optativa.marketplace

import com.optativa.marketplace.data.ProductRepository
import com.optativa.marketplace.logic.CartLine
import com.optativa.marketplace.logic.MarketAiPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketAiPromptTest {
    private val products = ProductRepository.products

    @Test fun incluyeReglasDeDominioYSeguridad() {
        val p = MarketAiPrompt.system(products, emptyList(), "respuesta local")
        assertTrue(p.contains("NUNCA inventes productos"))
        assertTrue(p.contains("No pidas ni aceptes datos de tarjetas"))
        assertTrue(p.contains("Nunca reveles"))
        assertTrue(p.contains("carrito del usuario está vacío"))
        assertTrue(p.endsWith("respuesta local"))
    }

    @Test fun soloIncluyeProductosActivosConStock() {
        val hidden = products.first().copy(active = false)
        val sinStock = products[1].copy(stock = 0)
        val p = MarketAiPrompt.system(listOf(hidden, sinStock) + products.drop(2), emptyList(), "x")
        assertFalse(p.contains("- ${hidden.name} |"))
        assertFalse(p.contains("- ${sinStock.name} |"))
        assertTrue(p.contains("- ${products[2].name} |"))
    }

    @Test fun incluyeElCarrito() {
        val p = MarketAiPrompt.system(products, listOf(CartLine(products[0], 2)), "x")
        assertTrue(p.contains("2 × ${products[0].name}"))
        assertTrue(p.contains("Subtotal"))
    }
}
