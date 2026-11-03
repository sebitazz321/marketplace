# Requisitos — Marketplace

## Requisitos funcionales

| ID | Descripción |
|---|---|
| RF-01 | El usuario se registra con nombre, correo y contraseña válidos (Firebase Authentication). |
| RF-02 | El usuario inicia y cierra sesión; la sesión se conserva. |
| RF-02b | El usuario puede recuperar su contraseña por correo. |
| RF-03 | El usuario explora el catálogo de productos por categorías. |
| RF-04 | El usuario busca productos por texto. |
| RF-05 | El usuario consulta el detalle de un producto. |
| RF-06 | El usuario agrega, quita y cambia cantidades en el carrito respetando el stock. |
| RF-07 | El carrito se guarda por usuario en Firestore y se recupera al volver a abrir la app. |
| RF-08 | El sistema calcula subtotal, envío y total; el usuario finaliza la compra. |
| RF-09 | El usuario consulta al chatbot por recomendaciones, carrito y políticas de la tienda. |
| RF-10 | El sistema asigna el perfil Comprador o Administrador según el correo con que se inicia sesión. |
| RF-11 | El comprador consulta y edita su perfil (nombre, teléfono, dirección y ciudad de envío). |
| RF-12 | El comprador marca productos como favoritos y los filtra en el catálogo. |
| RF-13 | El comprador consulta el historial de sus pedidos con su detalle. |
| RF-14 | El usuario solicita el cambio de contraseña desde su perfil. |
| RF-15 | El administrador ve un panel con indicadores de inventario (productos, unidades, valor, stock bajo). |
| RF-16 | El administrador crea, edita y elimina productos con validación de datos. |
| RF-17 | El administrador publica/oculta productos y ajusta el stock; cada compra descuenta el stock. |
| RF-18 | El administrador previsualiza la tienda tal como la ve un comprador. |

## Requisitos no funcionales

| ID | Descripción |
|---|---|
| RNF-01 | Requiere internet para registrarse/iniciar sesión; el catálogo es local. |
| RNF-02 | Las contraseñas las gestiona Firebase Authentication; nunca se guardan en la base de datos. |
| RNF-05 | Reglas de Firestore: cada usuario solo accede a su perfil, carrito y pedidos. |
| RNF-03 | Compatible con Android 8.0 (API 26) o superior. |
| RNF-04 | Soporta modo claro y oscuro. |
| RNF-06 | El rol administrador no depende de datos que el usuario pueda editar (lista de correos en `AdminConfig`). |
| RNF-07 | Los perfiles se implementan sin modificar la configuración ni las reglas de Firebase. |
