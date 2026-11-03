# Product Backlog Scrum — Marketplace

Sprints de **2 semanas**. Estados del tablero: New → Approved → Committed → Done.

## Sprint 1: Diseño, estructura y autenticación

| ID | Product Backlog Item | Effort | Tasks (horas) |
|---|---|---|---|
| PBI-01 | Estructura del proyecto y tema visual | 3 | Crear proyecto Android y dependencias (2 h)<br>Definir tema Material 3 claro/oscuro (2 h) |
| PBI-02 | Configurar Firebase en el proyecto | 3 | Crear proyecto en la consola de Firebase y agregar la app Android (2 h)<br>Agregar google-services.json y dependencias (1 h)<br>Habilitar Authentication y crear Firestore con reglas (2 h) |
| PBI-03 | Registro de usuario | 5 | Pantalla de registro (3 h)<br>Validadores de nombre, correo y contraseña (3 h)<br>Registro con Firebase Auth y documento users/{uid} (3 h) |
| PBI-04 | Inicio y cierre de sesión | 5 | Pantalla de login (3 h)<br>Persistencia de sesión con Firebase (2 h)<br>Botón salir (1 h) |
| PBI-05 | Recuperar contraseña por correo | 2 | Botón «¿Olvidaste tu contraseña?» con sendPasswordResetEmail (2 h)<br>Traducir errores de Firebase a español (AuthErrors) (2 h) |
| PBI-06 | Navegación inferior y barra superior | 3 | Barra de navegación con 3 pestañas (3 h) |

## Sprint 2: Catálogo, productos y búsqueda

| ID | Product Backlog Item | Effort | Tasks (horas) |
|---|---|---|---|
| PBI-07 | Catálogo de productos en cuadrícula | 5 | Modelo Product y repositorio local (3 h)<br>Tarjeta de producto (3 h)<br>Cuadrícula de 2 columnas (2 h) |
| PBI-08 | Detalle del producto | 3 | Diálogo con descripción, vendedor y stock (3 h) |
| PBI-09 | Búsqueda por texto | 5 | Campo de búsqueda (2 h)<br>Algoritmo CatalogSearch sin tildes (3 h) |
| PBI-10 | Filtro por categoría | 3 | Chips de categorías (3 h) |
| PBI-11 | Chatbot Mari: recomendaciones | 8 | Implementar MarketBot (categoría y presupuesto) (6 h)<br>Pantalla de chat con tarjetas de producto (5 h) |

## Sprint 3: Carrito, persistencia y pruebas

| ID | Product Backlog Item | Effort | Tasks (horas) |
|---|---|---|---|
| PBI-12 | Carrito de compras | 8 | Operaciones CartOps (agregar, restar, quitar) (4 h)<br>Pantalla del carrito (5 h)<br>Insignia con cantidad en la barra inferior (1 h) |
| PBI-13 | Persistencia del carrito por usuario en la nube | 5 | Conversión del carrito a mapa de Firestore (CartSerializer) (2 h)<br>Guardar y cargar carts/{uid} (3 h) |
| PBI-14 | Resumen de compra y finalización | 5 | Cálculo de subtotal, envío y total (Pricing) (3 h)<br>Guardar pedido en orders/{id} y diálogo de confirmación (3 h) |
| PBI-15 | Chatbot: carrito y políticas de la tienda | 3 | Respuestas de carrito, envío, pagos y devoluciones (3 h) |
| PBI-16 | Pruebas unitarias | 5 | LogicTest (búsqueda, carrito, precios, validadores) (4 h)<br>MarketBotTest (3 h) |
| PBI-17 | Pipeline de integración continua y documentación | 3 | Crear azure-pipelines.yml y ejecutarlo (3 h)<br>Completar README y evidencias (2 h) |

## Sprint 4: Perfiles de usuario (Comprador y Administrador)

| ID | Product Backlog Item | Effort | Tasks (horas) |
|---|---|---|---|
| PBI-18 | Roles Comprador / Administrador | 3 | Enum Role y AdminConfig (2 h)<br>Navegación inferior según el rol (2 h) |
| PBI-19 | Perfil del comprador | 5 | Pantalla de perfil con datos y edición (4 h)<br>Validadores de teléfono, dirección y ciudad (2 h) |
| PBI-20 | Favoritos | 3 | Corazón en tarjeta y filtro de favoritos (3 h)<br>Guardar favoritos en users/{uid} (1 h) |
| PBI-21 | Historial de pedidos | 3 | Consulta de orders por uid y OrderParser (3 h)<br>Tarjetas de pedido con detalle (2 h) |
| PBI-22 | Panel de administración | 5 | Indicadores de inventario (Inventory) (3 h)<br>Gráfico de valor por categoría y stock bajo (3 h) |
| PBI-23 | Gestión de productos (CRUD) | 8 | Formulario con ProductValidator (4 h)<br>CatalogOps y CatalogStore local (4 h)<br>Publicar/ocultar, reabastecer y restaurar (3 h) |
| PBI-24 | Pruebas y documentación de perfiles | 5 | AdminLogicTest y ProfileLogicTest (4 h)<br>Carpeta Documentacion (IEEE, matrices y manuales) (6 h) |

## Bugs de ejemplo

- Palabras como «laptop» activaban la intención «top ventas» del chatbot (corregido)
