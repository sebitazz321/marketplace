# Marketplace

**Materia:** Optativa II — Universidad de San Buenaventura
**Docente:** Ing. Jairo Armando Salcedo Aranda
**Herramienta de gestión:** Azure DevOps (Scrum)

## Integrantes

| Nombre                      | Rol Scrum |
|-----------------------------|---|
| _(Sarah Isabella Peña)_     | Product Owner |
| _(Juan Carlos Diaz )_       | Scrum Master |
| _(Sebastian Grande Moreno)_ | Developer |

## Descripción

App Android (Kotlin + Jetpack Compose) de comercio electrónico con dos perfiles: **Comprador** (catálogo, carrito persistente, compras, favoritos, historial y chatbot) y **Administrador** (panel de inventario y gestión del catálogo).

## Funcionalidades

- Registro, inicio de sesión y recuperación de contraseña con Firebase Authentication (la sesión se conserva).
- Catálogo de 20 productos en 5 categorías, con búsqueda insensible a tildes.
- Detalle de producto, control de stock y calificación.
- Carrito con cantidades, quitar productos, resumen y envío gratis desde $200.000.
- Carrito guardado por usuario en Cloud Firestore (se conserva al cerrar la app y entre dispositivos).
- Finalización de compra simulada: el pedido se guarda en Firestore con su número.
- Chatbot «Mari»: recomienda por categoría y presupuesto («algo menos de 100 mil»), muestra tarjetas de producto con botón agregar, consulta el carrito y responde envíos, pagos y devoluciones.

## Perfiles (v2.0)

| Perfil | Pestañas | Funciones |
|---|---|---|
| 🛍️ **Comprador** | Catálogo · Carrito · Asistente · Perfil | Editar datos y dirección de envío, favoritos (❤️ y filtro), historial de pedidos con detalle, cambio de contraseña. |
| 🛠️ **Administrador** | Panel · Productos · Tienda · Perfil | Indicadores (productos, unidades, valor del inventario, stock bajo, agotados, valor por categoría), crear/editar/eliminar productos, publicar/ocultar, ajustar stock, restaurar catálogo, vista previa de la tienda. |

**¿Quién es administrador?** Los correos de `AdminConfig.ADMIN_EMAILS` en `app/src/main/java/com/optativa/marketplace/data/Roles.kt` (por defecto `admin@marketplace.com`). Registra esa cuenta desde la app; cualquier otro correo entra como comprador. El rol no se toma de Firestore para que nadie pueda volverse administrador editando su perfil.

> Los perfiles no requieren cambios en Firebase: usan las mismas colecciones y reglas. El catálogo que administra el admin se guarda en el dispositivo (`CatalogStore`).

## Tecnologías

Kotlin · Jetpack Compose (Material 3) · ViewModel · Firebase Authentication · Cloud Firestore · JUnit 4 · Gradle (Kotlin DSL) · Azure Pipelines

## Organización Scrum

| Sprint | Actividades |
|---|---|
| Sprint 1 | Diseño, estructura y autenticación |
| Sprint 2 | Catálogo, productos y búsqueda |
| Sprint 3 | Carrito, persistencia y pruebas |
| Sprint 4 | Perfiles Comprador y Administrador |

Backlog completo en [`docs/requisitos/backlog_scrum.md`](docs/requisitos/backlog_scrum.md).
El archivo [`backlog_azure_import.csv`](docs/requisitos/backlog_azure_import.csv) se puede importar en Azure Boards.

## Estructura del repositorio

```
Marketplace/
├── Documentacion/     # documentos IEEE, matrices, manuales, modelos de datos, Firebase
├── docs/
│   ├── requisitos/    # requisitos y backlog
│   ├── diagramas/     # arquitectura
│   └── evidencias/    # capturas de la práctica
├── app/
│   └── src/
│       ├── main/      # código fuente (src/)
│       └── test/      # pruebas automatizadas (tests/)
├── README.md
└── azure-pipelines.yml
```

## Firebase (obligatorio antes de compilar)

Este proyecto necesita tu propio `app/google-services.json`. Sigue [`docs/firebase/FIREBASE_SETUP.md`](docs/firebase/FIREBASE_SETUP.md) (5 min). Las reglas de seguridad están en [`firebase/firestore.rules`](firebase/firestore.rules).

| Dato | Dónde se guarda |
|---|---|
| Usuarios y contraseñas | Firebase Authentication |
| Perfil (rol, teléfono, dirección, favoritos) | Firestore `users/{uid}` |
| Carrito | Firestore `carts/{uid}` |
| Pedidos | Firestore `orders/{id}` |
| Catálogo (administrable) | Local (`CatalogStore`, semilla `ProductRepository`) |

## Agente de IA «Mari» (Google Gemini)

El chat es un **agente híbrido**: el motor local de reglas calcula los datos exactos de la app y **Google Gemini** (`gemini-3.5-flash-lite`) redacta la respuesta. Si la IA no está configurada o falla (sin internet, clave inválida, límite de uso), responde el motor local y se informa el motivo. Cada mensaje indica si viene de la IA (🤖) o del modo local (⚙️).

**Configurar la clave (no se sube a Git):**

1. Crea una clave gratis en https://aistudio.google.com/apikey
2. En `local.properties` (raíz del proyecto) agrega: `GEMINI_API_KEY=tu_clave`
3. **File → Sync Project with Gradle Files** y ▶ Run.

Detalles en `Documentacion/07_Agente_IA/`. Seguridad en `Documentacion/06_Seguridad/`.

## Cómo ejecutar

1. Abre **Android Studio** (Koala o superior) → *Open* → selecciona esta carpeta.
2. Espera la sincronización de Gradle (usa JDK 17).
3. Crea un emulador (API 26+) o conecta un celular y pulsa ▶ *Run*.

Pruebas unitarias (46): `./gradlew testDebugUnitTest` (Windows: `gradlew.bat testDebugUnitTest`).
APK de depuración: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`.

## Ramas

- `main`: versión estable (protegida con Pull Request).
- `develop`: integración de funcionalidades.
- `feature/<nombre>`: desarrollo de nuevas funcionalidades.

## Chatbot «Mari»

Asistente local basado en reglas: funciona sin internet y sin claves de API. Ver `logic/MarketBot.kt`.
