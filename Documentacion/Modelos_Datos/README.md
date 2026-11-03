# Modelos de datos — Marketplace v2.0

| Archivo | Contenido |
|---|---|
| `modelo_entidad_relacion.png` | Modelo entidad-relación (Firestore + catálogo local) |
| `diagrama_clases.png` | Clases principales (modelos, ViewModel, almacenamiento) |
| `arquitectura.png` | Arquitectura por capas |
| `navegacion_roles.png` | Navegación según el rol (Comprador / Administrador) |
| `esquema_firestore.json` | Esquema de cada colección y del catálogo local |
| `ejemplos_documentos.json` | Documentos de ejemplo tal como quedan en Firestore |
| `fuentes/*.mmd` | Código Mermaid editable de los diagramas |

## Diccionario de datos

### users/{uid} — perfil (Firestore)
| Campo | Tipo | Oblig. | Descripción |
|---|---|---|---|
| name | string | Sí | Nombre visible |
| email | string | Sí | Correo de la cuenta |
| role | string | Sí | `comprador` o `admin` (informativo) |
| phone | string | No | Teléfono (7–15 dígitos) |
| address | string | No | Dirección de envío |
| city | string | No | Ciudad de envío |
| favorites | number[] | No | Ids de productos favoritos |
| createdAt / updatedAt | timestamp | — | Fechas del servidor |

### carts/{uid} — carrito (Firestore)
| Campo | Tipo | Descripción |
|---|---|---|
| items | map<string, number> | id de producto → cantidad |
| updatedAt | timestamp | Última modificación |

### orders/{id} — pedidos (Firestore)
| Campo | Tipo | Descripción |
|---|---|---|
| uid | string | Dueño del pedido |
| email | string | Correo del comprador |
| items | array | `{ productId, name, quantity, price }` |
| subtotal, shipping, total | number | Valores en COP |
| shippingAddress | string | Dirección y ciudad del perfil |
| status | string | Estado del pedido |
| createdAt | timestamp | Fecha del pedido |

### Product — catálogo (local, `CatalogStore`)
| Campo | Tipo | Regla |
|---|---|---|
| id | Int | Único, autoincremental |
| name | String | ≥ 3 caracteres, sin duplicados |
| category | String | Tecnología, Moda, Hogar, Deportes o Belleza |
| price | Double | 1 – 100.000.000 COP |
| stock | Int | 0 – 100.000 |
| description | String | ≥ 10 caracteres |
| seller | String | ≥ 2 caracteres |
| rating | Double | 0 = producto nuevo |
| active | Boolean | false = oculto para compradores |

Para regenerar un diagrama: `mmdc -i fuentes/arquitectura.mmd -o arquitectura.png -s 2 -b white`.
