# Arquitectura — Marketplace

Patrón: UI declarativa (Jetpack Compose) + ViewModel + lógica pura en Kotlin (fácil de probar).
La navegación cambia según el **rol** del usuario (Comprador o Administrador).

```mermaid
flowchart LR
    UI[Jetpack Compose UI] --> VM[MarketViewModel]
    VM --> AUTH[(Firebase Authentication)]
    VM --> FS[(Cloud Firestore: users, carts, orders)]
    VM --> STORE[(CatalogStore - SharedPreferences)]
    VM --> BOT[MarketBot]
    VM --> LOGIC[CartOps / Pricing / CatalogSearch / AuthErrors]
    VM --> ADMIN[CatalogOps / Inventory / ProductValidator]
    VM --> PROF[ProfileValidator / OrderParser / AdminConfig]
    STORE --> REPO[ProductRepository - semilla del catálogo]
```

Diagramas completos (arquitectura, navegación por rol, modelo de datos y clases) en `Documentacion/Modelos_Datos/`.
