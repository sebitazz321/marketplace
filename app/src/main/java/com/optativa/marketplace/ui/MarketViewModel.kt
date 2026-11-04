package com.optativa.marketplace.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.optativa.marketplace.BuildConfig
import com.optativa.marketplace.data.AdminConfig
import com.optativa.marketplace.data.GeminiClient
import com.optativa.marketplace.data.CatalogStore
import com.optativa.marketplace.data.Product
import com.optativa.marketplace.data.Role
import com.optativa.marketplace.data.User
import com.optativa.marketplace.data.UserProfile
import com.optativa.marketplace.logic.AiErrorMapper
import com.optativa.marketplace.logic.AiHistory
import com.optativa.marketplace.logic.AiInputGuard
import com.optativa.marketplace.logic.AiRateLimiter
import com.optativa.marketplace.logic.AiResponseGuard
import com.optativa.marketplace.logic.AuthErrors
import com.optativa.marketplace.logic.InputCheck
import com.optativa.marketplace.logic.MarketAiPrompt
import com.optativa.marketplace.logic.MsgSource
import com.optativa.marketplace.logic.Turn
import com.optativa.marketplace.logic.CartLine
import com.optativa.marketplace.logic.CartOps
import com.optativa.marketplace.logic.CartSerializer
import com.optativa.marketplace.logic.CatalogOps
import com.optativa.marketplace.logic.MarketBot
import com.optativa.marketplace.logic.OrderParser
import com.optativa.marketplace.logic.OrderSummary
import com.optativa.marketplace.logic.Pricing
import com.optativa.marketplace.logic.ProfileValidator
import com.optativa.marketplace.logic.Validators
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val source: MsgSource, val products: List<Product> = emptyList()) {
    val fromUser: Boolean get() = source == MsgSource.USER
}

/**
 * Estado de la app.
 *  - Firebase Authentication: registro, login, recuperación de contraseña y sesión.
 *  - Cloud Firestore (mismas colecciones y reglas de siempre):
 *      users/{uid}  perfil (nombre, correo, rol, teléfono, dirección, ciudad, favoritos)
 *      carts/{uid}  carrito
 *      orders/{id}  pedidos (el comprador consulta solo los suyos)
 *  - Catálogo: local ([CatalogStore]); lo gestiona el perfil Administrador.
 */
class MarketViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val store = CatalogStore(app)

    // ---- sesión (Firebase conserva la sesión entre aperturas de la app)
    var user by mutableStateOf(auth.currentUser?.toUser())
        private set
    var authError by mutableStateOf<String?>(null)
        private set
    var authInfo by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    val isAdmin: Boolean get() = user?.role == Role.ADMIN

    /** Mensaje de confirmación/error tras registrarse (se muestra en un diálogo). */
    var notice by mutableStateOf<String?>(null)
        private set

    fun dismissNotice() { notice = null }

    // ---- catálogo (todos los productos, incluidos los ocultos; el comprador solo ve los activos)
    var catalog by mutableStateOf(store.load())
        private set
    val products: List<Product> get() = CatalogOps.visible(catalog)
    fun productById(id: Int): Product? = catalog.firstOrNull { it.id == id }

    var query by mutableStateOf("")
    var category by mutableStateOf<String?>(null)

    // ---- carrito (idProducto -> cantidad), sincronizado con Firestore
    var cart by mutableStateOf<Map<Int, Int>>(emptyMap())
        private set
    var lastOrder by mutableStateOf<String?>(null)
        private set

    // ---- perfil
    var profile by mutableStateOf<UserProfile?>(null)
        private set
    var favorites by mutableStateOf<Set<Int>>(emptySet())
        private set
    var orders by mutableStateOf<List<OrderSummary>>(emptyList())
        private set
    var ordersLoading by mutableStateOf(false)
        private set
    var ordersError by mutableStateOf<String?>(null)
        private set
    var profileMessage by mutableStateOf<String?>(null)
        private set
    var profileSaving by mutableStateOf(false)
        private set

    fun dismissProfileMessage() { profileMessage = null }

    // ---- avisos del panel de administración
    var adminMessage by mutableStateOf<String?>(null)
        private set

    fun dismissAdminMessage() { adminMessage = null }

    // ---- chatbot
    val chat = mutableStateListOf(ChatMessage(MarketBot(emptyList()).greeting(), MsgSource.LOCAL))
    var typing by mutableStateOf(false)
        private set
    private val ai = GeminiClient(BuildConfig.GEMINI_API_KEY)
    private val aiLimiter = AiRateLimiter()
    val aiEnabled: Boolean get() = ai.isConfigured

    val cartLines: List<CartLine> get() = CartOps.lines(cart, products)
    val cartCount: Int get() = cartLines.sumOf { it.quantity }
    val favoriteProducts: List<Product> get() = products.filter { it.id in favorites }

    init {
        user?.let { afterSignIn(it) }
    }

    private fun FirebaseUser.toUser(nameOverride: String? = null) = User(
        uid = uid,
        name = nameOverride ?: displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore("@") ?: "Usuario",
        email = email ?: "",
        role = AdminConfig.roleFor(email)
    )

    private fun errorMessage(e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode
            ?: if (e is FirebaseNetworkException || e.message?.contains("network", ignoreCase = true) == true) AuthErrors.NETWORK
            else null
        return if (code != null) {
            AuthErrors.message(code)
        } else {
            val msg = e.localizedMessage ?: e.message
            if (!msg.isNullOrBlank()) "Error: $msg" else AuthErrors.message(null)
        }
    }

    // ------------------------------------------------------------ autenticación

    fun clearMessages() {
        authError = null
        authInfo = null
    }

    fun register(name: String, email: String, password: String) {
        clearMessages()
        authError = Validators.name(name) ?: Validators.email(email) ?: Validators.password(password)
        if (authError != null) return
        loading = true
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val fu = result.user ?: run { loading = false; return@addOnSuccessListener }
                val cleanName = name.trim()
                val role = AdminConfig.roleFor(fu.email ?: email)
                // 1) guarda el nombre en el perfil de Authentication, 2) crea el documento users/{uid}
                fu.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(cleanName).build())
                    .addOnCompleteListener {
                        db.collection("users").document(fu.uid).set(
                            mapOf(
                                "name" to cleanName,
                                "email" to (fu.email ?: email.trim()),
                                "role" to role.key,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                            .addOnSuccessListener {
                                notice = "✅ Cuenta creada y guardada en Firebase.\n\n" +
                                    "Perfil: ${role.emoji} ${role.label}\n" +
                                    "Authentication → Users: ${fu.email}\n" +
                                    "Firestore → users/${fu.uid}"
                                loadProfile(fu.uid)
                            }
                            .addOnFailureListener { e ->
                                notice = "⚠️ La cuenta se creó en Authentication, pero no se pudo guardar el perfil " +
                                    "en Firestore.\n\n${e.message}\n\n" +
                                    "Revisa que hayas creado Firestore Database y publicado las reglas."
                            }
                        onSignedIn(fu, cleanName)
                    }
            }
            .addOnFailureListener { e ->
                loading = false
                authError = errorMessage(e)
            }
    }

    fun login(email: String, password: String) {
        clearMessages()
        authError = Validators.email(email)
        if (authError != null) return
        if (password.isEmpty()) {
            authError = "Ingresa tu contraseña."
            return
        }
        loading = true
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val fu = result.user ?: run { loading = false; return@addOnSuccessListener }
                onSignedIn(fu)
            }
            .addOnFailureListener { e ->
                loading = false
                authError = errorMessage(e)
            }
    }

    fun resetPassword(email: String) {
        clearMessages()
        authError = Validators.email(email)
        if (authError != null) return
        loading = true
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                loading = false
                authInfo = "Te enviamos un correo para restablecer tu contraseña. Revisa también spam."
            }
            .addOnFailureListener { e ->
                loading = false
                authError = errorMessage(e)
            }
    }

    private fun onSignedIn(fu: FirebaseUser, nameOverride: String? = null) {
        loading = false
        clearMessages()
        val u = fu.toUser(nameOverride)
        user = u
        afterSignIn(u)
    }

    private fun afterSignIn(u: User) {
        loadCart(u.uid)
        loadProfile(u.uid)
    }

    fun logout() {
        auth.signOut()
        user = null
        cart = emptyMap()
        profile = null
        favorites = emptySet()
        orders = emptyList()
        ordersError = null
        query = ""
        category = null
        clearMessages()
        chat.clear()
        chat.add(ChatMessage(MarketBot(emptyList()).greeting(), MsgSource.LOCAL))
    }

    // ------------------------------------------------------------ perfil (Firestore users/{uid})

    private fun loadProfile(uid: String) {
        val u = user ?: return
        profile = UserProfile(name = u.name, email = u.email)
        val ref = db.collection("users").document(uid)
        ref.get().addOnSuccessListener { doc ->
            if (user?.uid != uid) return@addOnSuccessListener
            profile = UserProfile(
                name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: u.name,
                email = doc.getString("email") ?: u.email,
                phone = doc.getString("phone") ?: "",
                address = doc.getString("address") ?: "",
                city = doc.getString("city") ?: "",
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
            )
            favorites = (doc.get("favorites") as? List<*>).orEmpty()
                .mapNotNull { (it as? Number)?.toInt() }
                .toSet()
            // Cuentas creadas antes de los perfiles: se completa el documento con el rol.
            if (doc.getString("role") != u.role.key) {
                val missing = mutableMapOf<String, Any>("role" to u.role.key)
                if (!doc.exists()) {
                    missing["name"] = u.name
                    missing["email"] = u.email
                    missing["createdAt"] = FieldValue.serverTimestamp()
                }
                ref.set(missing, SetOptions.merge())
            }
        }
    }

    fun updateProfile(name: String, phone: String, address: String, city: String) {
        val u = user ?: return
        val error = Validators.name(name) ?: ProfileValidator.phone(phone) ?: ProfileValidator.address(address) ?: ProfileValidator.city(city)
        if (error != null) {
            profileMessage = "⚠️ $error"
            return
        }
        val clean = UserProfile(
            name = name.trim(),
            email = u.email,
            phone = phone.trim(),
            address = address.trim(),
            city = city.trim(),
            createdAt = profile?.createdAt
        )
        profileSaving = true
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(clean.name).build())
        db.collection("users").document(u.uid).set(
            mapOf(
                "name" to clean.name,
                "phone" to clean.phone,
                "address" to clean.address,
                "city" to clean.city,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
            .addOnSuccessListener {
                profileSaving = false
                profile = clean
                user = u.copy(name = clean.name)
                profileMessage = "✅ Perfil actualizado."
            }
            .addOnFailureListener { e ->
                profileSaving = false
                profileMessage = "⚠️ No se pudo guardar: ${e.message}"
            }
    }

    /** Envía al correo del usuario el enlace de Firebase para cambiar la contraseña. */
    fun sendPasswordChange() {
        val email = user?.email ?: return
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { profileMessage = "📧 Te enviamos un enlace a $email para cambiar tu contraseña." }
            .addOnFailureListener { e -> profileMessage = "⚠️ ${errorMessage(e)}" }
    }

    fun isFavorite(id: Int) = id in favorites

    fun toggleFavorite(id: Int) {
        val u = user ?: return
        favorites = if (id in favorites) favorites - id else favorites + id
        db.collection("users").document(u.uid)
            .set(mapOf("favorites" to favorites.sorted()), SetOptions.merge())
    }

    /** Historial del comprador: consulta solo sus pedidos (compatible con las reglas actuales). */
    fun loadOrders() {
        val u = user ?: return
        ordersLoading = true
        ordersError = null
        db.collection("orders").whereEqualTo("uid", u.uid).get()
            .addOnSuccessListener { snap ->
                ordersLoading = false
                orders = OrderParser.sort(
                    snap.documents.mapNotNull { d ->
                        OrderParser.parse(d.id, d.data, d.getTimestamp("createdAt")?.toDate()?.time)
                    }
                )
            }
            .addOnFailureListener { e ->
                ordersLoading = false
                ordersError = "No se pudo cargar el historial: ${e.message}"
            }
    }

    // ------------------------------------------------------------ carrito (Firestore)

    private fun loadCart(uid: String) {
        db.collection("carts").document(uid).get()
            .addOnSuccessListener { doc -> cart = CartSerializer.fromFirestore(doc.get("items") as? Map<*, *>) }
        // si falla (sin red) se conserva el carrito actual; Firestore reintenta con su caché local
    }

    private fun updateCart(newCart: Map<Int, Int>) {
        cart = newCart
        val uid = user?.uid ?: return
        db.collection("carts").document(uid).set(
            mapOf(
                "items" to CartSerializer.toFirestore(newCart),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    fun addToCart(p: Product) {
        val current = productById(p.id) ?: p // usa el stock más reciente del catálogo
        if (!current.active) return
        updateCart(CartOps.add(cart, current))
    }

    fun decrement(id: Int) = updateCart(CartOps.decrement(cart, id))
    fun remove(id: Int) = updateCart(CartOps.remove(cart, id))

    /** Simula el pago: guarda el pedido en orders/{id}, descuenta el stock y vacía el carrito. */
    fun checkout() {
        val u = user ?: return
        val lines = cartLines
        if (lines.isEmpty()) return
        val total = Pricing.total(lines)
        val p = profile
        val shippingTo = listOfNotNull(
            p?.address?.takeIf { it.isNotBlank() },
            p?.city?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
        val ref = db.collection("orders").document()
        ref.set(
            mapOf(
                "uid" to u.uid,
                "email" to u.email,
                "items" to lines.map {
                    mapOf(
                        "productId" to it.product.id,
                        "name" to it.product.name,
                        "quantity" to it.quantity,
                        "price" to it.product.price
                    )
                },
                "subtotal" to Pricing.subtotal(lines),
                "shipping" to Pricing.shipping(Pricing.subtotal(lines)),
                "total" to total,
                "shippingAddress" to shippingTo,
                "status" to "Confirmado",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).addOnSuccessListener { loadOrders() }

        updateCatalog(CatalogOps.applyOrder(catalog, lines.associate { it.product.id to it.quantity }))
        lastOrder = "${OrderParser.code(ref.id)} · total ${Pricing.format(total)}" +
            if (shippingTo.isNotBlank()) "\nEnvío a: $shippingTo" else ""
        updateCart(emptyMap())
    }

    fun dismissOrder() { lastOrder = null }

    // ------------------------------------------------------------ administración del catálogo

    private fun updateCatalog(newCatalog: List<Product>) {
        catalog = newCatalog
        store.save(newCatalog)
    }

    private fun requireAdmin(): Boolean {
        if (!isAdmin) adminMessage = "⛔ Solo un administrador puede modificar el catálogo."
        return isAdmin
    }

    fun saveProduct(p: Product) {
        if (!requireAdmin()) return
        val isNew = catalog.none { it.id == p.id }
        updateCatalog(CatalogOps.upsert(catalog, p))
        adminMessage = if (isNew) "✅ Producto «${p.name}» creado." else "✅ Producto «${p.name}» actualizado."
    }

    fun deleteProduct(id: Int) {
        if (!requireAdmin()) return
        val name = productById(id)?.name ?: "Producto"
        updateCatalog(CatalogOps.delete(catalog, id))
        if (cart.containsKey(id)) updateCart(CartOps.remove(cart, id))
        adminMessage = "🗑️ «$name» eliminado del catálogo."
    }

    fun setProductActive(id: Int, active: Boolean) {
        if (!requireAdmin()) return
        updateCatalog(CatalogOps.setActive(catalog, id, active))
    }

    fun restock(id: Int, delta: Int) {
        if (!requireAdmin()) return
        updateCatalog(CatalogOps.adjustStock(catalog, id, delta))
    }

    fun resetCatalog() {
        if (!requireAdmin()) return
        catalog = store.reset()
        adminMessage = "↩️ Catálogo restaurado a los ${catalog.size} productos originales."
    }

    // ------------------------------------------------------------ chatbot

    /**
     * Agente "Mari" (híbrido):
     *  1. Valida la entrada (longitud, caracteres, intentos de manipular las instrucciones).
     *  2. El motor local [MarketBot] busca productos reales del catálogo y calcula el carrito.
     *  3. Gemini redacta la respuesta con ese contexto; las tarjetas de productos salen del motor local.
     *  4. Si la IA no está configurada o falla → respuesta local con aviso.
     */
    fun send(text: String) {
        if (typing) return
        val clean = when (val check = AiInputGuard.check(text)) {
            is InputCheck.Ok -> check.text
            is InputCheck.Rejected -> {
                if (text.isNotBlank()) chat.add(ChatMessage(text.trim().take(80), MsgSource.USER))
                chat.add(ChatMessage(check.reason, MsgSource.LOCAL))
                return
            }
        }
        val history = AiHistory.trim(chat.map { Turn(if (it.fromUser) Turn.USER else Turn.MODEL, it.text) })
        chat.add(ChatMessage(clean, MsgSource.USER))
        typing = true
        val snapshot = cartLines
        val catalogNow = products
        val bot = MarketBot(catalogNow)
        viewModelScope.launch {
            val local = bot.reply(clean, snapshot)
            val reply = when {
                !ai.isConfigured -> ChatMessage(local.text, MsgSource.LOCAL, local.products)
                !aiLimiter.tryAcquire() -> ChatMessage("${local.text}\n\n${AiErrorMapper.fallbackNote(AiErrorMapper.RATE_LIMITED_LOCAL)}", MsgSource.LOCAL, local.products)
                else -> when (val r = ai.generate(MarketAiPrompt.system(catalogNow, snapshot, local.text), history, clean)) {
                    is GeminiClient.Result.Ok -> AiResponseGuard.clean(r.text)?.let { ChatMessage(it, MsgSource.AI, local.products) }
                        ?: ChatMessage("${local.text}\n\n${AiErrorMapper.fallbackNote(AiErrorMapper.BAD_RESPONSE)}", MsgSource.LOCAL, local.products)
                    is GeminiClient.Result.Fail -> ChatMessage("${local.text}\n\n${AiErrorMapper.fallbackNote(r.reason)}", MsgSource.LOCAL, local.products)
                }
            }
            chat.add(reply)
            typing = false
        }
    }
}
