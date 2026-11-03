package com.optativa.marketplace.logic

import com.optativa.marketplace.data.Product
import com.optativa.marketplace.data.ProductRepository

data class BotReply(val text: String, val products: List<Product> = emptyList())

/**
 * Asistente de compras "Mari": chatbot local basado en reglas (sin internet ni claves de API).
 * Recomienda productos por categoría / presupuesto, consulta el carrito y responde políticas de la tienda.
 */
class MarketBot(private val products: List<Product>) {

    companion object {
        val SUGGESTIONS = listOf(
            "Recomiéndame tecnología",
            "Algo menos de 100 mil",
            "¿Qué hay en mi carrito?",
            "¿Cómo es el envío?",
            "Los mejor valorados"
        )

        private val CATEGORY_WORDS = mapOf(
            ProductRepository.TECH to listOf("tecnologia", "tecnologico", "celular", "laptop", "portatil", "computador", "audifono", "cargador", "teclado", "mouse", "reloj", "smartwatch", "gadget"),
            ProductRepository.FASHION to listOf("moda", "ropa", "camiseta", "chaqueta", "tenis", "zapato", "bolso", "vestir"),
            ProductRepository.HOME to listOf("hogar", "casa", "cafetera", "silla", "lampara", "botella", "cocina", "oficina"),
            ProductRepository.SPORTS to listOf("deporte", "deportes", "futbol", "yoga", "bicicleta", "ejercicio", "gimnasio", "fitness"),
            ProductRepository.BEAUTY to listOf("belleza", "perfume", "maquillaje", "serum", "piel", "cosmetico")
        )
    }

    fun greeting(): String =
        "¡Hola! 🛍️ Soy Mari, tu asistente de compras. Puedo recomendarte productos por categoría o " +
            "presupuesto, revisar tu carrito y resolver dudas de envíos, pagos y devoluciones."

    /** Extrae un presupuesto en COP: "100 mil", "150000", "$200.000", "menos de 80k". */
    fun parseBudget(text: String): Double? {
        val t = TextUtil.normalize(text)
        val m = Regex("(\\d[\\d.,]*)\\s*(mil|k)?\\b").find(t) ?: return null
        val raw = m.groupValues[1].replace(".", "").replace(",", "")
        val n = raw.toDoubleOrNull() ?: return null
        val thousands = m.groupValues[2].isNotEmpty()
        return when {
            thousands -> n * 1000
            n < 1000 -> n * 1000   // "100" se interpreta como 100 mil
            else -> n
        }
    }

    fun reply(input: String, cart: List<CartLine>): BotReply {
        val t = TextUtil.normalize(input)

        return when {
            has(t, "carrito", "cuanto llevo", "mi compra", "mi pedido") -> cartSummary(cart)

            has(t, "envio", "domicilio", "entrega", "despacho") ->
                BotReply(
                    "🚚 Enviamos a todo el país en 2–5 días hábiles. El envío cuesta " +
                        "${Pricing.format(Pricing.SHIPPING_COST)} y es GRATIS desde ${Pricing.format(Pricing.FREE_SHIPPING_FROM)}."
                )

            has(t, "pago", "pagar", "tarjeta", "pse", "nequi", "contraentrega") ->
                BotReply("💳 Aceptamos tarjeta débito/crédito, PSE, Nequi y pago contraentrega (según ciudad). Pagas al finalizar la compra desde el carrito.")

            has(t, "devolucion", "devolver", "garantia", "cambio", "reembolso") ->
                BotReply("↩️ Tienes 10 días para devolver un producto sin usar y en su empaque. La garantía depende del vendedor (mínimo 3 meses en tecnología).")

            has(t, "vender", "vendedor", "publicar") ->
                BotReply("🏪 Para vender en el marketplace regístrate como vendedor, publica tu producto con fotos y precio, y gestiona los pedidos desde tu panel. (Próximo sprint del proyecto.)")

            has(t, "mejor valorado", "mejores", "mas vendido", "populares", "los mejores") ->
                BotReply("⭐ Estos son los mejor calificados:", products.sortedByDescending { it.rating }.take(3))

            has(t, "hola", "buenas", "buenos dias", "buenas tardes", "hey", "saludos") ->
                BotReply("¡Hola! 😊 ¿Buscas algo en particular? Dime una categoría (tecnología, moda, hogar, deportes, belleza) o tu presupuesto.")

            has(t, "gracias", "genial", "perfecto", "listo") ->
                BotReply("¡Con gusto! 🙌 Si necesitas algo más, aquí estaré.")

            has(t, "adios", "chao", "hasta luego") -> BotReply("¡Hasta pronto! Que disfrutes tus compras 🛒")

            else -> recommend(t)
        }
    }

    private fun recommend(t: String): BotReply {
        val category = CATEGORY_WORDS.entries.firstOrNull { (_, words) -> words.any { t.contains(it) } }?.key
        val budget = parseBudget(t)
        val cheap = has(t, "barato", "economico", "oferta", "descuento")

        // coincidencia directa por nombre de producto
        val byName = products.filter { p ->
            TextUtil.normalize(p.name).split(" ").any { w -> w.length > 4 && t.contains(w) }
        }

        var candidates = when {
            byName.isNotEmpty() && category == null -> byName
            category != null -> products.filter { it.category == category }
            else -> products
        }
        if (budget != null) candidates = candidates.filter { it.price <= budget }
        candidates = if (cheap) candidates.sortedBy { it.price } else candidates.sortedByDescending { it.rating }

        val hasCriteria = category != null || budget != null || cheap || byName.isNotEmpty()
        if (!hasCriteria) {
            return BotReply("No estoy seguro de entender 🤔. Prueba con \"recomiéndame tecnología\", \"algo menos de 100 mil\" o \"¿cómo es el envío?\".")
        }
        if (candidates.isEmpty()) {
            val where = category?.let { " en $it" } ?: ""
            val limit = budget?.let { " por menos de ${Pricing.format(it)}" } ?: ""
            return BotReply("No encontré productos$where$limit 😕. Prueba con otro presupuesto u otra categoría.")
        }

        val top = candidates.take(3)
        val intro = buildString {
            append("✨ Te recomiendo")
            category?.let { append(" en $it") }
            budget?.let { append(" por menos de ${Pricing.format(it)}") }
            append(":")
        }
        return BotReply(intro, top)
    }

    private fun cartSummary(cart: List<CartLine>): BotReply {
        if (cart.isEmpty()) return BotReply("🛒 Tu carrito está vacío. ¿Quieres que te recomiende algo?")
        val subtotal = Pricing.subtotal(cart)
        val shipping = Pricing.shipping(subtotal)
        val lines = cart.joinToString("\n") { "• ${it.quantity} × ${it.product.name} — ${Pricing.format(it.total)}" }
        val shipText = if (shipping == 0.0) "envío gratis 🎉" else {
            val missing = Pricing.FREE_SHIPPING_FROM - subtotal
            "envío ${Pricing.format(shipping)} (te faltan ${Pricing.format(missing)} para envío gratis)"
        }
        return BotReply("🛒 Tu carrito:\n$lines\n\nSubtotal: ${Pricing.format(subtotal)} · $shipText\nTotal: ${Pricing.format(subtotal + shipping)}")
    }

    private fun has(text: String, vararg keys: String) = keys.any { text.contains(it) }
}
