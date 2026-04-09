# DivvyUp — Plan del Proyecto

## 1. Visión del Producto

DivvyUp es una app de **gestión de gastos en grupo** que combina la simplicidad de Tricount/Splitwise con funcionalidades avanzadas propias de apps de control de gastos individuales:

- **Reparto de gastos entre participantes** (equitativo, por porcentaje, por importes personalizados)
- **Categorización de gastos** con análisis por categoría
- **Filtrado avanzado** por fecha, categoría, pagador, participante
- **Estadísticas y gráficos** (gasto por mes, por categoría, por persona, evolución temporal)
- **Exportación de datos** (CSV, PDF)
- **Cálculo de deudas optimizado** (minimización de transferencias)
- **Historial de liquidaciones/pagos**

## 2. Funcionalidades — Priorización por Fases

### Fase 1 — MVP (Core)
| # | Feature | Descripción |
|---|---------|-------------|
| F1 | Gestión de grupos | Crear, editar, eliminar grupos |
| F2 | Participantes | Añadir/eliminar participantes a un grupo |
| F3 | Registro de gastos | Crear gasto con concepto, importe, fecha, pagador, categoría |
| F4 | Reparto equitativo | Dividir gasto entre todos o entre seleccionados |
| F5 | Categorías | Categorías predefinidas + personalizadas por grupo |
| F6 | Balance del grupo | Quién debe a quién (algoritmo de minimización de transferencias) |
| F7 | Detalle del grupo | Lista de gastos, participantes, balance |

### Fase 2 — Filtros y Análisis
| # | Feature | Descripción |
|---|---------|-------------|
| F8 | Filtros avanzados | Por rango de fechas, categoría, pagador, participante implicado |
| F9 | Resumen por categoría | Gráfico circular con desglose por categoría |
| F10 | Resumen por mes | Gráfico de barras con evolución mensual |
| F11 | Estadísticas por persona | Cuánto ha pagado, cuánto le corresponde, balance neto |
| F12 | Reparto personalizado | Por porcentaje o importe fijo por participante |

### Fase 3 — Exportación y Liquidación
| # | Feature | Descripción |
|---|---------|-------------|
| F13 | Exportar a CSV | Exportar gastos filtrados a CSV |
| F14 | Exportar a PDF | Resumen del grupo en PDF |
| F15 | Registro de pagos/liquidaciones | Marcar deuda como pagada (total o parcial) |
| F16 | Historial de liquidaciones | Ver historial de pagos entre participantes |

### Fase 4 — Social y Avanzado (Futuro)
| # | Feature | Descripción |
|---|---------|-------------|
| F17 | Autenticación | Login con Supabase Auth (email, Google) |
| F18 | Invitar por enlace | Compartir grupo via deep link |
| F19 | Notificaciones push | Avisar cuando se añade un gasto |
| F20 | Multi-divisa | Gastos en diferentes monedas con conversión |
| F21 | Gastos recurrentes | Programar gastos periódicos |
| F22 | Fotos de tickets | Adjuntar imagen al gasto |

## 3. Arquitectura de Pantallas

```
GroupListScreen (lista de grupos)
  └─ CreateGroupDialog
  
GroupDetailScreen (detalle de grupo: tabs)
  ├─ Tab: Gastos (lista con filtros)
  │   ├─ FilterSheet (bottom sheet con filtros)
  │   └─ AddSpendDialog / EditSpendDialog
  ├─ Tab: Balances (quién debe a quién)
  │   └─ SettleUpDialog (registrar pago)
  ├─ Tab: Estadísticas
  │   ├─ Resumen por categoría (pie chart)
  │   ├─ Evolución mensual (bar chart)
  │   └─ Detalle por persona
  └─ Tab: Participantes
      └─ AddParticipantDialog

SettingsScreen (futuro: perfil, divisa, tema)
```

## 4. Modelo de Dominio (Entidades)

```
Group
├── id: Long
├── name: String
├── description: String
├── createdAt: Instant
└── currency: String (default "EUR")

Participant
├── id: Long
├── groupId: Long
├── name: String
├── email: String?
└── createdAt: Instant

Category
├── id: Long
├── groupId: Long? (null = global/predefinida)
├── name: String
├── icon: String (emoji o nombre de icono Material)
└── color: String (hex color)

Spend
├── id: Long
├── groupId: Long
├── concept: String
├── amount: Double (representado como numeric en DB)
├── date: Instant
├── payerId: Long (FK → Participant)
├── categoryId: Long? (FK → Category)
├── splitType: SplitType (EQUAL, PERCENTAGE, CUSTOM)
├── notes: String?
└── createdAt: Instant

SpendShare (cómo se reparte cada gasto)
├── id: Long
├── spendId: Long (FK → Spend)
├── participantId: Long (FK → Participant)
├── amount: Double (lo que le corresponde pagar)
├── percentage: Double? (si splitType=PERCENTAGE)
└── isPaid: Boolean (si ha liquidado su parte)

Settlement (liquidación/pago entre personas)
├── id: Long
├── groupId: Long
├── fromParticipantId: Long (quien paga la deuda)
├── toParticipantId: Long (quien recibe el pago)
├── amount: Double
├── date: Instant
├── notes: String?
└── createdAt: Instant
```

## 5. Algoritmo de Balances

**Minimización de transferencias** (greedy):
1. Calcular balance neto de cada participante: `pagado - le_corresponde`
2. Separar en deudores (balance < 0) y acreedores (balance > 0)
3. Ordenar deudores por deuda desc, acreedores por crédito desc
4. Emparejar mayor deudor con mayor acreedor, transferir el mínimo de ambos
5. Repetir hasta que todos los balances sean 0

## 6. Stack Técnico

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.2 (KMP) |
| UI | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose |
| Estado | StateFlow + ViewModel (KMP) |
| Backend | Supabase (Postgres + Auth) |
| Serialización | kotlinx.serialization |
| Fechas | kotlinx.datetime |
| HTTP | Ktor (via Supabase SDK) |
| Gráficos | Vico (compose charts) o custom Canvas |
| Exportación CSV | Kotlin stdlib (manual) |
| Exportación PDF | Android Canvas/PDF API |
| DI | Manual (no framework) |

## 7. Estructura de Carpetas (Target)

```
app/src/
├── commonMain/kotlin/com/example/divvyup/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Group.kt
│   │   │   ├── Participant.kt
│   │   │   ├── Category.kt
│   │   │   ├── Spend.kt
│   │   │   ├── SpendShare.kt
│   │   │   ├── Settlement.kt
│   │   │   ├── SplitType.kt
│   │   │   ├── Balance.kt
│   │   │   └── DebtSimplification.kt
│   │   └── repository/
│   │       ├── GroupRepository.kt
│   │       ├── SpendRepository.kt
│   │       ├── CategoryRepository.kt
│   │       └── SettlementRepository.kt
│   └── application/
│       ├── GroupService.kt
│       ├── SpendService.kt
│       ├── BalanceCalculator.kt
│       └── CategoryService.kt
│
├── androidMain/kotlin/com/example/divvyup/
│   ├── integration/
│   │   ├── supabase/
│   │   │   ├── SupabaseGroupRepository.kt
│   │   │   ├── SupabaseSpendRepository.kt
│   │   │   ├── SupabaseCategoryRepository.kt
│   │   │   ├── SupabaseSettlementRepository.kt
│   │   │   └── dto/   (DTOs @Serializable + mappers)
│   │   └── ui/
│   │       ├── viewmodel/
│   │       │   ├── GroupListViewModel.kt
│   │       │   └── GroupDetailViewModel.kt
│   │       ├── screens/
│   │       │   ├── GroupListScreen.kt
│   │       │   ├── GroupDetailScreen.kt
│   │       │   └── components/  (composables reutilizables)
│   │       ├── navigation/
│   │       │   ├── Screen.kt
│   │       │   └── AppNavigation.kt
│   │       └── theme/
│   │           └── Theme.kt
│   └── MainActivity.kt
```

