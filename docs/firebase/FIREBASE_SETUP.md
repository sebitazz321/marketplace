# Configuración de Firebase (Marketplace)

La app usa **Firebase Authentication** (correo + contraseña) y **Cloud Firestore**. Sin el archivo `google-services.json` la app **no compila**.

## 1. Crear el proyecto
1. Entra a <https://console.firebase.google.com> con tu cuenta Google → **Agregar proyecto** → nombre `marketplace-optativa` (puedes desactivar Google Analytics).
2. Plan **Spark** (gratis): alcanza de sobra para la práctica.

## 2. Registrar la app Android
1. En el proyecto: **ícono Android** (Agregar app).
2. **Nombre del paquete:** `com.optativa.marketplace` (debe ser exacto).
3. Descarga **`google-services.json`** y cópialo en `Marketplace/app/google-services.json` (junto a `app/build.gradle.kts`). _(En este zip ya viene el del proyecto `marketplace-bdf94`.)_
4. Los pasos de "agregar SDK" que muestra la consola ya están hechos en el proyecto: salta hasta el final.

## 3. Activar Authentication
**Compilación → Authentication → Comenzar → Método de acceso → Correo electrónico/contraseña → Habilitar → Guardar.**

## 4. Crear Firestore
**Compilación → Firestore Database → Crear base de datos →** elige ubicación (ej. `nam5` o `southamerica-east1`) → modo **producción** → Crear. Luego pestaña **Reglas** → pega el contenido de `firebase/firestore.rules` → **Publicar**.

## 5. Dónde ver los datos (para la exposición)
| Qué | Dónde en la consola |
|---|---|
| Usuarios registrados (correo, UID, fecha) | **Authentication → Users** |
| Perfiles `users/{uid}` | **Firestore Database → Datos → users** |
| Carrito de cada usuario `carts/{uid}` | **Firestore Database → Datos → carts** (campo `items`: `{ "3": 2, "5": 1 }` = id de producto → cantidad) |
| Pedidos `orders/{id}` | **Firestore Database → Datos → orders** |

Las contraseñas **nunca** se ven ni se guardan en Firestore: las gestiona Firebase Authentication.

## 6. Pipeline de Azure
El pipeline necesita el `google-services.json` para compilar. Opción simple: **súbelo al repositorio** (contiene identificadores públicos del proyecto, no contraseñas; la seguridad real está en las reglas de Firestore). Opción más estricta: súbelo en *Pipelines → Library → Secure files* y descárgalo en el YAML con `DownloadSecureFile@1`.

## Notas
- Las pruebas unitarias (`./gradlew testDebugUnitTest`) no usan Firebase y corren sin internet.
- El catálogo de productos sigue siendo local (`ProductRepository`); usuarios, carritos y pedidos sí están en la nube.
- Si recibes `ERROR_INVALID_CREDENTIAL` al entrar: correo o contraseña incorrectos (o la cuenta no existe).

## Cómo comprobar que un registro quedó guardado
1. Corre la app, ve a **Regístrate**, llena nombre, correo y contraseña (letras + números, mínimo 6) y pulsa **Crear cuenta**.
2. Al entrar aparece un diálogo: **«✅ Cuenta creada y guardada en Firebase»** (o un ⚠️ con el error exacto).
3. En la consola de Firebase:
   - **Authentication → Users:** debe aparecer el correo con su UID.
   - **Firestore Database → Datos → users → (UID):** campos `name`, `email`, `createdAt`.
4. Si el diálogo dice ⚠️ o Firestore queda vacío: casi siempre es que no se creó **Firestore Database** o no se publicaron las **reglas** (paso 4).
