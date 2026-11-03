package com.optativa.marketplace

import com.optativa.marketplace.data.AdminConfig
import com.optativa.marketplace.data.ProductRepository
import com.optativa.marketplace.data.Role
import com.optativa.marketplace.logic.CatalogOps
import com.optativa.marketplace.logic.Inventory
import com.optativa.marketplace.logic.ProductDraft
import com.optativa.marketplace.logic.ProductValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminLogicTest {
    private val all = ProductRepository.products

    private val valid = ProductDraft(
        name = "Parlante Bluetooth",
        category = ProductRepository.TECH,
        price = "129.900",
        stock = "15",
        emoji = "🔊",
        description = "Sonido 360° y 12 h de batería.",
        seller = "SonidoPlus"
    )

    // ---- roles
    @Test fun role_adminEmailIsCaseAndSpaceInsensitive() {
        val admin = AdminConfig.ADMIN_EMAILS.first()
        assertEquals(Role.ADMIN, AdminConfig.roleFor("  ${admin.uppercase()} "))
    }

    @Test fun role_anyOtherEmailIsBuyer() {
        assertEquals(Role.BUYER, AdminConfig.roleFor("cliente@gmail.com"))
        assertEquals(Role.BUYER, AdminConfig.roleFor(null))
    }

    @Test fun role_fromKey() {
        assertEquals(Role.ADMIN, Role.fromKey("admin"))
        assertEquals(Role.BUYER, Role.fromKey("comprador"))
        assertEquals(Role.BUYER, Role.fromKey("desconocido"))
    }

    // ---- validación del formulario
    @Test fun validator_acceptsValidDraft() = assertNull(ProductValidator.validate(valid, all))

    @Test fun validator_parsesColombianPrice() {
        assertEquals(189_900.0, ProductValidator.parsePrice("$189.900")!!, 0.0)
        assertEquals(189_900.0, ProductValidator.parsePrice("189,900")!!, 0.0)
        assertNull(ProductValidator.parsePrice("abc"))
        assertNull(ProductValidator.parsePrice(""))
    }

    @Test fun validator_rejectsBadFields() {
        assertNotNull(ProductValidator.validate(valid.copy(name = "ab"), all))
        assertNotNull(ProductValidator.validate(valid.copy(price = "0"), all))
        assertNotNull(ProductValidator.validate(valid.copy(price = "gratis"), all))
        assertNotNull(ProductValidator.validate(valid.copy(stock = "-3"), all))
        assertNotNull(ProductValidator.validate(valid.copy(stock = "2.5"), all))
        assertNotNull(ProductValidator.validate(valid.copy(description = "corta"), all))
        assertNotNull(ProductValidator.validate(valid.copy(seller = ""), all))
        assertNotNull(ProductValidator.validate(valid.copy(category = "Juguetes"), all))
    }

    @Test fun validator_rejectsDuplicateNameIgnoringAccents() {
        val dup = valid.copy(name = "audifonos inalambricos anc")
        assertNotNull(ProductValidator.validate(dup, all))
        // al editar el mismo producto el nombre no cuenta como duplicado
        assertNull(ProductValidator.validate(dup, all, editingId = 1))
    }

    @Test fun validator_toProductTrimsAndParses() {
        val p = ProductValidator.toProduct(valid.copy(name = "  Parlante  "), id = 99)
        assertEquals("Parlante", p.name)
        assertEquals(129_900.0, p.price, 0.0)
        assertEquals(15, p.stock)
        assertTrue(p.active)
    }

    // ---- operaciones del catálogo
    @Test fun ops_nextIdAndUpsert() {
        val id = CatalogOps.nextId(all)
        assertEquals(21, id)
        val created = CatalogOps.upsert(all, ProductValidator.toProduct(valid, id))
        assertEquals(all.size + 1, created.size)
        val edited = CatalogOps.upsert(created, created.last().copy(price = 1.0))
        assertEquals(created.size, edited.size)
        assertEquals(1.0, edited.last().price, 0.0)
    }

    @Test fun ops_deleteAndHide() {
        assertEquals(all.size - 1, CatalogOps.delete(all, 1).size)
        val hidden = CatalogOps.setActive(all, 2, false)
        assertFalse(hidden.first { it.id == 2 }.active)
        assertEquals(all.size - 1, CatalogOps.visible(hidden).size)
    }

    @Test fun ops_stockNeverNegative() {
        val p = CatalogOps.adjustStock(all, 6, -100).first { it.id == 6 }
        assertEquals(0, p.stock)
        val q = CatalogOps.adjustStock(all, 6, 10).first { it.id == 6 }
        assertEquals(17, q.stock)
    }

    @Test fun ops_applyOrderDiscountsStock() {
        val after = CatalogOps.applyOrder(all, mapOf(1 to 5, 6 to 50))
        assertEquals(20, after.first { it.id == 1 }.stock)
        assertEquals(0, after.first { it.id == 6 }.stock)
        assertEquals(all.first { it.id == 2 }.stock, after.first { it.id == 2 }.stock)
    }

    // ---- indicadores
    @Test fun inventory_stats() {
        val s = Inventory.stats(all)
        assertEquals(20, s.totalProducts)
        assertEquals(20, s.activeProducts)
        assertEquals(all.sumOf { it.stock }, s.totalUnits)
        assertEquals(all.sumOf { it.price * it.stock }, s.inventoryValue, 0.01)
        assertTrue(s.lowStock.all { it.stock <= Inventory.LOW_STOCK })
        assertEquals(listOf(6, 14, 16), s.lowStock.map { it.id }.sorted()) // stock 7, 9, 6
        assertEquals(ProductRepository.categories.size, s.byCategory.size)
    }
}
