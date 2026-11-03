package com.optativa.marketplace

import com.optativa.marketplace.logic.Dates
import com.optativa.marketplace.logic.Initials
import com.optativa.marketplace.logic.OrderParser
import com.optativa.marketplace.logic.ProfileValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileLogicTest {

    @Test fun phone_optionalButValidated() {
        assertNull(ProfileValidator.phone(""))
        assertNull(ProfileValidator.phone("+57 300 123 4567"))
        assertNotNull(ProfileValidator.phone("12345"))
        assertNotNull(ProfileValidator.phone("300abc4567"))
    }

    @Test fun address_and_city() {
        assertNull(ProfileValidator.address(""))
        assertNull(ProfileValidator.address("Cra 7 # 45-10"))
        assertNotNull(ProfileValidator.address("Cr"))
        assertNotNull(ProfileValidator.city("Bo"))
    }

    @Test fun initials() {
        assertEquals("SG", Initials.of("Sebastián Grande Moreno"))
        assertEquals("A", Initials.of("ana"))
        assertEquals("?", Initials.of("   "))
    }

    @Test fun dates_nullIsDash() = assertEquals("—", Dates.day(null))

    @Test fun order_parsesFirestoreMap() {
        val data = mapOf<String, Any?>(
            "uid" to "u1",
            "total" to 131_800L,
            "status" to "Confirmado",
            "items" to listOf(
                mapOf("productId" to 3L, "name" to "Cargador rápido 65W", "quantity" to 2L, "price" to 59_900.0),
                "basura"
            )
        )
        val o = OrderParser.parse("abcdef123", data, 1_000L)!!
        assertEquals("PED-ABCDEF", o.code)
        assertEquals(131_800.0, o.total, 0.0)
        assertEquals(1, o.items.size)
        assertEquals(2, o.units)
        assertEquals(119_800.0, o.items[0].total, 0.0)
    }

    @Test fun order_missingTotalIsComputed() {
        val data = mapOf<String, Any?>("items" to listOf(mapOf("quantity" to 3, "price" to 10.0)))
        assertEquals(30.0, OrderParser.parse("x", data, null)!!.total, 0.0)
        assertNull(OrderParser.parse("x", null, null))
    }

    @Test fun order_sortNewestFirstPendingOnTop() {
        val a = OrderParser.parse("a", mapOf("total" to 1), 100L)!!
        val b = OrderParser.parse("b", mapOf("total" to 1), 200L)!!
        val c = OrderParser.parse("c", mapOf("total" to 1), null)!!
        assertEquals(listOf("c", "b", "a"), OrderParser.sort(listOf(a, b, c)).map { it.id })
    }
}
