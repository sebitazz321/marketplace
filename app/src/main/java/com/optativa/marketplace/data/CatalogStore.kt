package com.optativa.marketplace.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistencia local del catálogo que administra el perfil Administrador.
 *
 * - La primera vez usa [ProductRepository.products] como semilla.
 * - Los cambios del administrador (crear, editar, ocultar, borrar, reabastecer) y el descuento de
 *   stock de cada compra se guardan en SharedPreferences como JSON.
 * - No usa Firebase: la configuración y las reglas de Firestore del proyecto no se modifican.
 */
class CatalogStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<Product> {
        val raw = prefs.getString(KEY, null) ?: return ProductRepository.products
        return try {
            decode(raw)
        } catch (e: Exception) {
            ProductRepository.products
        }
    }

    fun save(products: List<Product>) {
        prefs.edit().putString(KEY, encode(products)).apply()
    }

    fun reset(): List<Product> {
        prefs.edit().remove(KEY).apply()
        return ProductRepository.products
    }

    companion object {
        private const val PREFS = "marketplace_catalog"
        private const val KEY = "products_v1"

        fun encode(products: List<Product>): String {
            val array = JSONArray()
            products.forEach { p ->
                array.put(
                    JSONObject()
                        .put("id", p.id)
                        .put("name", p.name)
                        .put("category", p.category)
                        .put("price", p.price)
                        .put("emoji", p.emoji)
                        .put("description", p.description)
                        .put("rating", p.rating)
                        .put("stock", p.stock)
                        .put("seller", p.seller)
                        .put("active", p.active)
                )
            }
            return array.toString()
        }

        fun decode(raw: String): List<Product> {
            val array = JSONArray(raw)
            return (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Product(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    category = o.getString("category"),
                    price = o.getDouble("price"),
                    emoji = o.optString("emoji", "📦"),
                    description = o.optString("description", ""),
                    rating = o.optDouble("rating", 0.0),
                    stock = o.optInt("stock", 0),
                    seller = o.optString("seller", "Marketplace"),
                    active = o.optBoolean("active", true)
                )
            }
        }
    }
}
