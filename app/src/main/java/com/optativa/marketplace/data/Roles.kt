package com.optativa.marketplace.data

/** Perfiles de la aplicación. [key] es el valor que se guarda en Firestore (`users/{uid}.role`). */
enum class Role(val key: String, val label: String, val emoji: String) {
    BUYER("comprador", "Comprador", "🛍️"),
    ADMIN("admin", "Administrador", "🛠️");

    companion object {
        fun fromKey(key: String?): Role = entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) } ?: BUYER
    }
}

/**
 * Quién es administrador.
 *
 * El rol se decide en la app a partir de esta lista de correos (no del documento de Firestore),
 * porque las reglas actuales permiten que cada usuario edite su propio `users/{uid}`; así nadie
 * puede volverse administrador escribiendo `role: "admin"` en su perfil.
 *
 * Para crear un administrador: registra una cuenta con uno de estos correos desde la app.
 * Para agregar otro, añade su correo aquí y vuelve a compilar.
 */
object AdminConfig {
    val ADMIN_EMAILS: Set<String> = setOf(
        "admin@marketplace.com"
    )

    fun roleFor(email: String?): Role {
        val clean = email?.trim()?.lowercase() ?: return Role.BUYER
        return if (ADMIN_EMAILS.any { it.lowercase() == clean }) Role.ADMIN else Role.BUYER
    }
}
