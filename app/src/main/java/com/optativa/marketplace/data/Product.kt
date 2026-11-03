package com.optativa.marketplace.data

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,          // COP
    val emoji: String,
    val description: String,
    val rating: Double,
    val stock: Int,
    val seller: String,
    val active: Boolean = true  // el administrador puede ocultar un producto sin borrarlo
)

/** Usuario en sesión. El rol decide qué navegación y pantallas ve. */
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val role: Role = Role.BUYER
)

/** Datos de perfil guardados en Firestore `users/{uid}`. */
data class UserProfile(
    val name: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val createdAt: Long? = null
)

/** Catálogo local de ejemplo (sirve sin internet). Es la semilla inicial del [CatalogStore]. */
object ProductRepository {

    const val TECH = "Tecnología"
    const val FASHION = "Moda"
    const val HOME = "Hogar"
    const val SPORTS = "Deportes"
    const val BEAUTY = "Belleza"

    val categories = listOf(TECH, FASHION, HOME, SPORTS, BEAUTY)

    val products: List<Product> = listOf(
        Product(1, "Audífonos inalámbricos ANC", TECH, 189_900.0, "🎧", "Cancelación de ruido activa, 30 h de batería y Bluetooth 5.3.", 4.7, 25, "SonidoPlus"),
        Product(2, "Smartwatch Fit Pro", TECH, 249_900.0, "⌚", "Monitor de sueño y ritmo cardíaco, GPS integrado, resistente al agua.", 4.5, 18, "TechLine"),
        Product(3, "Cargador rápido 65W", TECH, 59_900.0, "🔌", "Carga celulares y portátiles. Doble puerto USB-C.", 4.6, 80, "TechLine"),
        Product(4, "Teclado mecánico RGB", TECH, 159_900.0, "⌨️", "Switches rojos, iluminación RGB y construcción en aluminio.", 4.4, 30, "GamerZone"),
        Product(5, "Mouse inalámbrico ergonómico", TECH, 49_900.0, "🖱️", "Sensor de 1600 DPI, silencioso, batería de 12 meses.", 4.3, 60, "GamerZone"),
        Product(6, "Laptop 14\" 16GB RAM", TECH, 1_899_900.0, "💻", "Procesador de última generación, SSD de 512 GB y pantalla Full HD.", 4.6, 7, "CompuMundo"),
        Product(7, "Camiseta algodón orgánico", FASHION, 45_900.0, "👕", "Suave, transpirable y de corte regular. Varias tallas.", 4.2, 120, "ModaViva"),
        Product(8, "Chaqueta de jean clásica", FASHION, 139_900.0, "🧥", "Denim resistente con forro interior suave.", 4.5, 35, "ModaViva"),
        Product(9, "Tenis urbanos blancos", FASHION, 219_900.0, "👟", "Cómodos, livianos y combinables con todo.", 4.6, 40, "Paso Firme"),
        Product(10, "Bolso tote de cuero vegano", FASHION, 89_900.0, "👜", "Espacioso, con bolsillo interno y cierre magnético.", 4.1, 22, "Paso Firme"),
        Product(11, "Botella térmica 750 ml", HOME, 39_900.0, "🧴", "Mantiene el frío 24 h y el calor 12 h. Acero inoxidable.", 4.8, 150, "CasaFresca"),
        Product(12, "Cafetera espresso", HOME, 329_900.0, "☕", "Presión de 15 bares y vaporizador de leche.", 4.5, 12, "CasaFresca"),
        Product(13, "Lámpara LED de escritorio", HOME, 69_900.0, "💡", "3 tonos de luz, 5 intensidades y carga USB.", 4.4, 45, "LuzHogar"),
        Product(14, "Silla ergonómica de oficina", HOME, 599_900.0, "🪑", "Soporte lumbar, brazos ajustables y malla transpirable.", 4.7, 9, "LuzHogar"),
        Product(15, "Balón de fútbol profesional", SPORTS, 79_900.0, "⚽", "Tamaño 5, costuras termoselladas.", 4.5, 55, "DeporMax"),
        Product(16, "Bicicleta estática", SPORTS, 899_900.0, "🚴", "8 niveles de resistencia, pantalla LCD y asiento ajustable.", 4.3, 6, "DeporMax"),
        Product(17, "Tapete de yoga antideslizante", SPORTS, 64_900.0, "🧘", "6 mm de grosor, ecológico y fácil de limpiar.", 4.6, 70, "Zen Fit"),
        Product(18, "Sérum de vitamina C", BEAUTY, 54_900.0, "🧪", "Ilumina y unifica el tono. 30 ml, apto para todo tipo de piel.", 4.4, 90, "PielNova"),
        Product(19, "Perfume floral 100 ml", BEAUTY, 169_900.0, "🌸", "Notas de jazmín y vainilla. Duración prolongada.", 4.5, 28, "PielNova"),
        Product(20, "Kit de maquillaje básico", BEAUTY, 119_900.0, "💄", "Base, labial, sombras y brochas incluidas.", 4.2, 33, "Glam Co")
    )

    fun byId(id: Int): Product? = products.firstOrNull { it.id == id }
}
