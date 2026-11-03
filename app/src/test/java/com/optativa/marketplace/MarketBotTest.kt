package com.optativa.marketplace

import com.optativa.marketplace.data.ProductRepository
import com.optativa.marketplace.logic.CartLine
import com.optativa.marketplace.logic.MarketBot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketBotTest {
    private val bot = MarketBot(ProductRepository.products)

    @Test fun parseBudget_variants() {
        assertEquals(100_000.0, bot.parseBudget("menos de 100 mil")!!, 0.0)
        assertEquals(150_000.0, bot.parseBudget("hasta 150000")!!, 0.0)
        assertEquals(200_000.0, bot.parseBudget("\$200.000")!!, 0.0)
        assertEquals(80_000.0, bot.parseBudget("80k")!!, 0.0)
        assertEquals(null, bot.parseBudget("sin números"))
    }

    @Test fun recommendsByCategory() {
        val r = bot.reply("Recomiéndame tecnología", emptyList())
        assertTrue(r.products.isNotEmpty())
        assertTrue(r.products.all { it.category == ProductRepository.TECH })
    }

    @Test fun recommendsByBudget() {
        val r = bot.reply("Algo menos de 100 mil", emptyList())
        assertTrue(r.products.isNotEmpty())
        assertTrue(r.products.all { it.price <= 100_000.0 })
    }

    @Test fun categoryAndBudgetCombined() {
        val r = bot.reply("ropa por menos de 100 mil", emptyList())
        assertTrue(r.products.all { it.category == ProductRepository.FASHION && it.price <= 100_000.0 })
    }

    @Test fun impossibleBudgetGivesFriendlyMessage() {
        val r = bot.reply("laptop menos de 10 mil", emptyList())
        assertTrue(r.text, r.products.isEmpty() && r.text.contains("No encontré"))
    }

    @Test fun laptopIsNotMistakenForTop() {
        val r = bot.reply("quiero una laptop", emptyList())
        assertTrue(r.text, r.products.any { it.name.contains("Laptop") })
    }

    @Test fun cartSummary_emptyAndFilled() {
        assertTrue(bot.reply("¿Qué hay en mi carrito?", emptyList()).text.contains("vacío"))
        val p = ProductRepository.byId(3)!!
        val r = bot.reply("¿Qué hay en mi carrito?", listOf(CartLine(p, 2)))
        assertTrue(r.text, r.text.contains("2 × ${p.name}"))
        assertTrue(r.text, r.text.contains("faltan"))
    }

    @Test fun storePolicies() {
        assertTrue(bot.reply("¿Cómo es el envío?", emptyList()).text.contains("GRATIS"))
        assertTrue(bot.reply("¿Cómo puedo pagar?", emptyList()).text.contains("PSE"))
        assertTrue(bot.reply("quiero una devolución", emptyList()).text.contains("10 días"))
    }

    @Test fun topRated() {
        val r = bot.reply("Los mejor valorados", emptyList())
        assertEquals(3, r.products.size)
        assertEquals(r.products.maxOf { it.rating }, r.products.first().rating, 0.0)
    }

    @Test fun fallback() {
        assertTrue(bot.reply("asdfgh", emptyList()).text.contains("No estoy seguro"))
    }
}
