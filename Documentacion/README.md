# Documentación — Marketplace v2.0

Documentación del proyecto según estándares IEEE / ISO. Cada documento está en Word (`.docx`, editable) y PDF.

```
Documentacion/
├── 01_Documentos_IEEE/
│   ├── SRS_Requisitos            IEEE 830 / ISO 29148 — requisitos
│   ├── Arquitectura_Diseno       IEEE 1016 / ISO 42010 — arquitectura y diseño
│   ├── Plan_Pruebas              IEEE 829 / ISO 29119-3 — plan de pruebas
│   └── Informe_Pruebas           IEEE 829 — informe resumen de pruebas
├── 02_Matrices/
│   ├── Matriz_Maestra.xlsx           vista general: requisitos, sprints, entregables, RACI
│   ├── Historias_Usuario.xlsx        19 historias con criterios de aceptación
│   ├── Trazabilidad_Requisitos.xlsx  RF → HU → componente → casos → pruebas unitarias
│   ├── Casos_Prueba.xlsx             30 casos funcionales + 46 pruebas unitarias
│   ├── Matriz_Riesgos.xlsx           12 riesgos con probabilidad × impacto y mapa de calor
│   ├── Cumplimiento_Guia.xlsx        lista de chequeo de la guía de la asignatura
│   └── Auditoria_Archivos.xlsx       inventario de archivos del repositorio (nuevo/modificado)
├── 03_Manuales/
│   ├── Manual_Instalacion
│   ├── Manual_Tecnico
│   └── Manual_Usuario
├── 04_Casos_Uso_y_Prueba/
│   ├── Casos_Uso                       14 casos de uso (UML 2.5) con flujos y diagramas
│   ├── Especificacion_Casos_Prueba     30 casos de prueba detallados por caso de uso (IEEE 829)
│   ├── Matriz_CasosUso_CasosPrueba.xlsx  trazabilidad caso de uso ↔ caso de prueba
│   └── Diagramas/                      diagramas de casos de uso Comprador y Administrador (PNG + SVG)
├── 05_Mockups/                       mockups de alta fidelidad de las pantallas, diseño y accesibilidad
├── 06_Seguridad/                     pruebas OWASP MASVS, análisis estático (mobsfscan antes/después), vulnerabilidades, re-pruebas, Kali/BadStore
├── 07_Agente_IA/                     agente de IA «Mari»: propósito, Gemini, controles, protección de la clave y escenarios de prueba
├── Modelos_Datos/                    diagramas ER, clases, arquitectura, navegación y esquema Firestore
└── Documentacion_Firebase            configuración, modelo de datos y reglas
```
