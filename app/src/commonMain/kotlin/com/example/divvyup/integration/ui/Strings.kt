package com.example.divvyup.integration.ui

import kotlin.math.abs
import kotlin.math.round

/**
 * Strings centralizados para el rediseño visual (Phases 6–7). Antes vivían
 * inline en cada Composable como literales; ahora son una sola fuente para
 * editar la copia.
 *
 * Mantener esto en Kotlin (no `strings.xml`) es una decisión deliberada:
 *
 *  - El proyecto es KMP (Android + iOS). Un bundle Android-only no llega a
 *    iOS limpiamente, y aún no tenemos segundo locale.
 *  - Cuando llegue i18n real, este objeto es trivialmente extraíble a
 *    `Strings.Es` / `Strings.En` o a Compose Resources sin tocar call-sites,
 *    que aquí pasan por constantes tipadas o helpers Kotlin (nunca por
 *    `String.format` directo).
 */
object Strings {

    // ── Common (compartidos por ≥2 pantallas) ───────────────────────────────

    object Common {
        const val CANCEL = "Cancelar"
        const val OK = "OK"
        const val DELETE = "Eliminar"
        const val SAVE = "Guardar"
        const val ACCEPT = "Aceptar"
        const val LOGOUT = "Cerrar sesión"
        const val ALL_FEMININE = "Todas"   // "Todas las categorías"
        const val ALL_MASCULINE = "Todos"  // "Todos los participantes"

        // Filtros de borrado avanzado (compartidos por GroupList y SpendTab)
        const val DELETE_SPENDS_TITLE = "Borrar gastos"
        const val DELETE_SPENDS_CONFIRM = "Borrar gastos"
        const val PERIOD_LABEL = "Período"
        const val CATEGORY_OPTIONAL_LABEL = "Categoría (opcional)"
        const val PERSON_OPTIONAL_LABEL = "Persona (opcional)"

        // ── Helpers ──────────────────────────────────────────────────────

        /** "$n seleccionados" — cabecera de selección. */
        fun selectedCount(n: Int): String = "$n seleccionados"

        /** "$amount $currency" — importe + código de moneda. */
        fun amountCurrency(amount: String, currency: String): String =
            "$amount $currency"

        /** "$sign$amount $currency" — importe firmado (badge personal). */
        fun amountCurrencySigned(sign: String, amount: String, currency: String): String =
            "$sign$amount $currency"

        /** Format Double as "12,34" — KMP-friendly sin Locale. */
        fun fmt2(value: Double): String {
            val r = round(value * 100) / 100.0
            return "${r.toLong()}.${(abs(r % 1) * 100).toLong().toString().padStart(2, '0')}"
        }
    }

    // ── GroupListScreen ─────────────────────────────────────────────────────

    object GroupList {
        // AppBar / TopBar
        const val A11Y_USER_SETTINGS = "Ajustes de usuario"
        const val APP_TITLE_FALLBACK = "DivvyUp"
        const val SUBTITLE_DEFAULT = "Tus grupos de gastos"
        const val SUBTITLE_SELECTION = "Mantén pulsado para seleccionar más"

        // FAB
        const val FAB_NEW_GROUP = "Nuevo grupo"

        // Search & empty state
        const val SEARCH_PLACEHOLDER = "Buscar grupo"
        const val SEARCH_EMPTY = "No hay grupos que coincidan con la búsqueda"

        // EmptyGroupsPlaceholder
        const val EMPTY_HEADLINE = "Sin grupos todavía"
        const val EMPTY_SUBTITLE = "Crea tu primer grupo para empezar\na compartir gastos con tus amigos"

        // Dialog "Borrar grupos seleccionados"
        const val DELETE_SELECTED_TITLE = "Borrar grupos seleccionados"
        fun deleteConfirmGroupsSelected(count: Int): String =
            "¿Eliminar $count grupo(s) seleccionado(s)? Se borrarán todos sus gastos y participantes."

        // GroupSelectorDialog
        const val SELECT_GROUP_TITLE = "Selecciona un grupo"

        // Delete group dialog
        const val DELETE_GROUP_TITLE = "Eliminar grupo"
        fun deleteGroupConfirm(name: String): String =
            "¿Eliminar \"$name\"? Se borrarán todos sus gastos y participantes."

        // GroupCard a11y
        const val A11Y_OPEN_GROUP = "Abrir grupo"
        const val A11Y_SELECT_GROUP = "Seleccionar grupo"
        const val A11Y_DESELECT_GROUP = "Deseleccionar grupo"
        const val A11Y_REMOVE_GROUP = "Eliminar grupo"

        // AdvancedDeleteDialog — fila de grupo actual
        fun groupNameLabel(groupName: String): String = "Grupo: $groupName"

        // DeleteTimeOption labels (valores que viven en el enum privado).
        // El enum los referencia directamente: `TODO(Strings.GroupList.TIME_ALL)`.
        const val TIME_ALL = "Todos los gastos"
        const val TIME_LAST_WEEK = "Anteriores a 1 semana"
        const val TIME_LAST_MONTH = "Anteriores a 1 mes"
        const val TIME_LAST_3_MONTHS = "Anteriores a 3 meses"
        const val TIME_LAST_YEAR = "Anteriores a 1 año"

        // Aviso resumen — todas las frases del bloque "Se borrarán…"
        const val SUMMARY_PREFIX = "Se borrarán los gastos"
        const val SUMMARY_ALL_SUFFIX = " (todos)"
        const val SUMMARY_LAST_WEEK = " anteriores a la última semana"
        const val SUMMARY_LAST_MONTH = " anteriores al último mes"
        const val SUMMARY_LAST_3_MONTHS = " anteriores a los últimos 3 meses"
        const val SUMMARY_LAST_YEAR = " anteriores al último año"
        const val SUMMARY_BY_CATEGORY = " de la categoría seleccionada"
        const val SUMMARY_BY_PERSON = " pagados por la persona seleccionada"
        const val SUMMARY_FOOTER = ". Esta acción no se puede deshacer."

        /** "$count participante(s)" — plural manual para mantener el comportamiento actual. */
        fun participantsCount(n: Int): String =
            "$n participante${if (n == 1) "" else "s"}"

        /** Ensambla el resumen de borrado (GroupListScreen.AdvancedDeleteDialog). */
        fun deleteSummary(
            timeSuffix: String,
            hasCategory: Boolean,
            hasPerson: Boolean
        ): String = buildString {
            append(SUMMARY_PREFIX)
            append(timeSuffix)
            if (hasCategory) append(SUMMARY_BY_CATEGORY)
            if (hasPerson) append(SUMMARY_BY_PERSON)
            append(SUMMARY_FOOTER)
        }
    }

    // ── GroupDetailScreen ───────────────────────────────────────────────────

    object GroupDetail {
        const val LOADING_FALLBACK = "Cargando..."
        const val FAB_NEW_SPEND = "Nuevo gasto"
        const val A11Y_GROUP_SETTINGS = "Ajustes del grupo"

        // Tabs
        const val TAB_GASTOS = "Gastos"
        const val TAB_BALANCES = "Balances"
        const val TAB_ANALYTICS = "Analíticas"
        const val TAB_ACTIVITY = "Actividad"

        /** "$count participantes - $currency" — header del AppTopBar. */
        fun participantsHeader(count: Int, currency: String): String =
            "$count participantes - $currency"
    }

    // ── SpendTabScreen ──────────────────────────────────────────────────────

    object SpendTab {
        // Search & empty state
        const val SEARCH_PLACEHOLDER = "Buscar gasto"
        const val FILTERED_EMPTY = "No hay gastos que coincidan"

        // Paginación
        const val LOAD_MORE = "Mostrar más"

        // Empty state
        const val EMOJI_EMPTY = "💸"
        const val EMPTY_HEADLINE = "Sin gastos todavía"
        const val EMPTY_SUBTITLE = "Pulsa el botón para añadir el primer gasto"

        // Payer fallback
        const val PAYER_UNKNOWN = "Desconocido"

        // A11y iconos
        const val A11Y_FILTERS = "Filtros"
        const val A11Y_MULTI_SELECT = "Selección múltiple"
        const val A11Y_DELETE_SELECTED = "Borrar seleccionados"
        const val A11Y_ADVANCED_DELETE = "Borrado avanzado"

        // Recurrence badges
        const val RECURRENCE_WEEKLY = "🔁 Semanal"
        const val RECURRENCE_MONTHLY = "🔁 Mensual"
        const val RECURRENCE_DAILY = "🔁 Diario"

        // Auto-generated badge
        const val AUTO_BADGE = "⚡ Auto"

        // Delete selected dialog
        const val DELETE_SELECTED_TITLE = "Borrar gastos seleccionados"
        fun deleteConfirmSpendsSelected(count: Int): String =
            "¿Eliminar $count gasto(s) seleccionado(s)?"

        // Filter dialog labels
        const val FILTER_TITLE = "Filtros"
        const val FILTER_CATEGORY = "Categoría"
        const val FILTER_PERSON = "Persona"
        const val FILTER_DATE_RANGE = "Rango de fechas"
        const val DATE_FROM_LABEL = "Desde"
        const val DATE_TO_LABEL = "Hasta"
        const val FILTER_APPLY = "Aplicar"
        const val FILTER_CLEAR = "Limpiar"

        // SpendDeleteTimeOption labels (enum strings)
        const val TIME_ALL = "Todos"
        const val TIME_LAST_WEEK = "Anteriores a la última semana"
        const val TIME_LAST_MONTH = "Anteriores al último mes"
        const val TIME_LAST_3_MONTHS = "Anteriores a los últimos 3 meses"
        const val TIME_LAST_YEAR = "Anteriores al último año"

        // Aviso resumen — copy idéntico a GroupList pero ensamblado distinto:
        // aquí el sufijo de tiempo es `selectedTime.label.lowercase()` y se
        // omite cuando es `TODOS`. Mantener separación con Strings.GroupList.
        const val SUMMARY_PREFIX = "Se borrarán los gastos"
        const val SUMMARY_BY_CATEGORY = " de la categoría seleccionada"
        const val SUMMARY_BY_PERSON = " pagados por la persona seleccionada"
        const val SUMMARY_FOOTER = ". Esta acción no se puede deshacer."

        /** Ensambla el resumen de borrado (SpendTabScreen.SpendAdvancedDeleteDialog). */
        fun deleteSummary(
            timeSuffix: String?,
            hasCategory: Boolean,
            hasPerson: Boolean
        ): String = buildString {
            append(SUMMARY_PREFIX)
            if (timeSuffix != null) append(" ").append(timeSuffix)
            if (hasCategory) append(SUMMARY_BY_CATEGORY)
            if (hasPerson) append(SUMMARY_BY_PERSON)
            append(SUMMARY_FOOTER)
        }
    }

    // ── BalanceTabScreen ────────────────────────────────────────────────────

    object BalanceTab {
        const val EMOJI_EMPTY = "⚖️"
        const val EMPTY_HEADLINE = "Sin balances todavía"
        const val EMPTY_SUBTITLE = "Añade gastos para ver cómo se reparte"

        const val SECTION_BALANCES = "Balances individuales"
        const val SECTION_TRANSFERS = "Liquidaciones sugeridas"
        const val TRANSFER_BUTTON = "Liquidar"

        // TransferCard
        const val TRANSFER_SUGGESTED = "Pago sugerido"
        const val TRANSFER_DEBTOR_CHIP = "Debe"
        const val TRANSFER_CREDITOR_CHIP = "A"

        // Debt subtitle (debajo del nombre en BalanceCard) — conserva el
        // formato byte-for-byte del original para no romper la lectura.
        const val DEBT_COLON_PREFIX = "Debe:"
        const val NO_DEBT = "Sin deuda pendiente"

        // a11y BalanceCard — stateDescription para TalkBack. Mantener
        // literales byte-for-byte (el audit de Phase 5 validó el formato).
        const val A11Y_OWED_TO_YOU = "Te deben"
        const val A11Y_YOU_OWE = "Debes"
        const val A11Y_BALANCED = "Balance"

        /** TalkBack BalanceCard announcement: "Te deben 12,50 EUR" / etc. */
        fun talkBackBalance(netBalance: Double, currency: String): String {
            val signText = when {
                netBalance > 0 -> A11Y_OWED_TO_YOU
                netBalance < 0 -> A11Y_YOU_OWE
                else -> A11Y_BALANCED
            }
            val absAmount = Common.fmt2(abs(netBalance))
            return "$signText $absAmount $currency"
        }
    }

    // ── UserSettingsScreen ──────────────────────────────────────────────────

    object UserSettings {
        // TopBar
        const val A11Y_BACK = "Volver"
        const val TITLE = "Perfil y cuenta"

        // Email fallback
        const val EMAIL_FALLBACK = "Sesión iniciada"
        const val EMAIL_EMDASH_FALLBACK = "—"

        // Logout dialog
        const val LOGOUT_CONFIRM_BODY = "¿Seguro que quieres cerrar sesión?"

        // Avatar a11y
        const val A11Y_CHANGE_AVATAR = "Cambiar foto de perfil"

        // Profile section
        const val SECTION_PROFILE = "Información del perfil"
        const val A11Y_EDIT_PROFILE = "Editar perfil"
        const val FIELD_EMAIL = "Correo electrónico"
        const val FIELD_DISPLAY_NAME = "Nombre para mostrar"
        const val DISPLAY_NAME_FALLBACK = "Sin nombre"

        // Security section
        const val SECTION_SECURITY = "Seguridad"
        const val CHANGE_PASSWORD = "Cambiar contraseña"

        // Unauthenticated
        const val UNAUTH_HEADLINE = "Sin sesión iniciada"
        const val UNAUTH_EMOJI = "☁️"
        const val UNAUTH_GUEST_TEXT =
            "Estás en modo invitado. Tus grupos se guardan en la nube de forma temporal. " +
            "Crea una cuenta para acceder a ellos desde cualquier dispositivo."
        const val UNAUTH_PERKS_HEADER =
            "Puedes usar DivvyUp sin cuenta, pero si inicias sesión podrás:"
        const val UNAUTH_PERK_CLOUD_EMOJI = "☁️"
        const val UNAUTH_PERK_CLOUD = "Guardar tus grupos en la nube"
        const val UNAUTH_PERK_DEVICE_EMOJI = "📱"
        const val UNAUTH_PERK_DEVICE = "Acceder desde cualquier dispositivo"
        const val UNAUTH_PERK_COMMUNITY_EMOJI = "👥"
        const val UNAUTH_PERK_COMMUNITY = "Unirte a grupos de otros usuarios"
        const val UNAUTH_PERK_SECURE_EMOJI = "🔒"
        const val UNAUTH_PERK_SECURE = "Mantener tus datos seguros"

        const val BUTTON_LOGIN = "Iniciar sesión"
        const val BUTTON_REGISTER = "Crear cuenta nueva"

        // Notification section
        const val SECTION_NOTIFICATIONS = "Notificaciones"
        const val NOTIFICATION_FIELD = "Avisos de gastos"
        const val NOTIFICATION_HELPER = "Recibe avisos locales al añadir, editar o eliminar gastos."

        // Theme section
        const val SECTION_APPEARANCE = "Apariencia"
        const val THEME_SUBTITLE = "Tema de la aplicación"
        const val THEME_SYSTEM = "Sistema"
        const val THEME_LIGHT = "Claro"
        const val THEME_DARK = "Oscuro"
    }

    // ── Auth (compartido por Register y Login) ───────────────────────────────

    object Auth {
        // Logo / hero
        const val LOGO_EMOJI = "💸"
        const val APP_NAME = "DivvyUp"

        // A11y password toggle
        const val A11Y_HIDE_PASSWORD = "Ocultar contraseña"
        const val A11Y_SHOW_PASSWORD = "Mostrar contraseña"

        // Login-only separator
        const val OR_SEPARATOR = "  o  "
    }

    // ── RegisterScreen ──────────────────────────────────────────────────────

    object Register {
        const val HEADLINE = "Crea tu cuenta"
        const val FIELD_EMAIL = "Correo electrónico"
        const val FIELD_PASSWORD_HINT = "Contraseña (mín. 6 caracteres)"
        const val BUTTON_CREATE = "Crear cuenta"
        const val PROMPT_LOGIN = "¿Ya tienes cuenta? Inicia sesión"
    }

    // ── LoginScreen ──────────────────────────────────────────────────────────

    object Login {
        const val HEADLINE = "Inicia sesión para continuar"
        const val FIELD_EMAIL = "Correo electrónico"
        const val FIELD_PASSWORD = "Contraseña"
        const val BUTTON_LOGIN = "Iniciar sesión"
        const val PROVIDER_GOOGLE = "Google"
        const val BUTTON_CONTINUE_GOOGLE = "Continuar con Google"
        const val PROMPT_REGISTER = "¿No tienes cuenta? Regístrate"
    }

    // ── ChangePasswordScreen ─────────────────────────────────────────────────

    object ChangePassword {
        const val A11Y_BACK = "Volver"
        const val TITLE = "Cambiar contraseña"
        const val INTRO = "Introduce tu contraseña actual y una nueva para actualizarla."
        const val FIELD_CURRENT = "Contraseña actual"
        const val FIELD_NEW = "Nueva contraseña"
        const val FIELD_NEW_HINT = "Mínimo 6 caracteres"
        const val FIELD_CONFIRM = "Confirmar nueva contraseña"

        /** Mensaje de error inline en formulario. */
        const val ERROR_MISMATCH = "Las contraseñas no coinciden"

        const val BUTTON_UPDATE = "Actualizar contraseña"
    }

    // ── JoinGroupParticipantScreen ───────────────────────────────────────────

    object JoinGroup {
        const val A11Y_BACK = "Volver"
        const val TITLE = "Unirse al grupo"
        const val BUTTON_CONTINUE = "Continuar"
        const val INSTRUCTION =
            "Selecciona qué participante eres para vincular tu usuario a este grupo."

        /** "Te han invitado a \"<groupName>\"" — ensamblado para preservar comillas. */
        fun invitedToGroup(groupName: String): String =
            "Te han invitado a \"$groupName\""
    }

    // ── ActivityTabScreen ────────────────────────────────────────────────────

    object Activity {
        const val EMOJI_EMPTY = "📋"
        const val EMPTY_HEADLINE = "Sin actividad registrada aún"
        const val EMPTY_SUBTITLE =
            "Los gastos, liquidaciones y cambios en el grupo aparecerán aquí."
        const val SECTION_HISTORY = "Historial de actividad"
        const val MONTH_HINT = "Se muestran los eventos del último mes"
        const val TITLE = "Actividad"
        const val SUBTITLE = "Movimientos recientes en tus grupos"

        /** "$count eventos" — cabecera de sección. */
        fun eventCount(count: Int): String = "$count eventos"

        /** "por $actor" — atribución de evento. */
        fun byActor(actorName: String): String = "por $actorName"

        /** "en $groupName" — grupo al que pertenece el evento. */
        fun inGroup(groupName: String): String = "en $groupName"
    }

    // ── Navegación inferior (shell) ──────────────────────────────────────────

    object Nav {
        const val GROUPS = "Grupos"
        const val ACTIVITY = "Actividad"
        const val PROFILE = "Perfil"
    }

    // ── SettleUpScreen ───────────────────────────────────────────────────────

    object SettleUp {
        const val TITLE = "Liquidar cuentas"
        const val CELEBRATION = "¡Cuentas liquidadas!"
        const val TOTAL_LABEL = "Total a liquidar"
        const val CONFIRM_BUTTON = "Confirmar liquidación"
        const val SNACKBAR_ACTION = "Cerrar"
        const val ACTION_DESELECT_ALL = "Deseleccionar todos"
        const val ACTION_SELECT_ALL = "Seleccionar todos"
        const val SUCCESS_EMOJI = "✅"

        /** "$selected de $total seleccionados" — barra de selección. */
        fun selectionCount(selected: Int, total: Int): String =
            "$selected de $total seleccionados"

        /** Mensaje cuando ya no quedan transferencias que liquidar. */
        const val ALREADY_SETTLED = "Las cuentas ya estan saldadas"
    }

    // ── GroupSettings (pantalla + dialogs + layouts) ─────────────────────────

    object GroupSettings {
        // TopBar
        const val A11Y_BACK = "Volver"
        const val TITLE = "Ajustes del grupo"
        const val SAVE_BUTTON = "Guardar cambios"

        // Section: Información del grupo
        const val SECTION_INFO = "Información del grupo"
        const val AVATAR_FALLBACK_LETTER = "G"
        const val A11Y_CHANGE_GROUP_PHOTO = "Cambiar foto del grupo"
        const val FIELD_GROUP_NAME = "Nombre del grupo *"
        const val ERROR_NAME_EMPTY = "El nombre no puede estar vacío"
        const val FIELD_DESCRIPTION = "Descripción (opcional)"
        const val LABEL_CURRENCY = "Moneda"
        const val LABEL_DEFAULT_CATEGORY = "Categoría por defecto"
        const val NONE_EMOJI = "📦"
        const val NONE_LABEL = "Ninguna"
        const val BUTTON_SHARE_INVITE = "Compartir enlace de invitación"

        // Section: Participantes
        const val SECTION_PARTICIPANTS = "Participantes"
        const val HEADER_SPLIT = "Reparto"
        const val HEADER_IS_ME = "Soy yo"
        const val ME_BADGE = "Yo"
        const val PERCENTAGE_EMDASH = "—"
        const val A11Y_DELETE_PARTICIPANT = "Eliminar participante"
        const val BUTTON_ADD_PARTICIPANT = "Añadir"
        const val BUTTON_SPLIT = "Reparto"

        // Member-only banner
        const val MEMBER_READONLY_BANNER =
            "Solo el creador del grupo puede editar su información y categorías. " +
            "Aquí puedes indicar cuál eres tú."

        // Section: Categorías personalizadas
        const val SECTION_CUSTOM_CATEGORIES = "Categorías personalizadas"
        const val EMPTY_CUSTOM_CATEGORIES = "Aún no has creado categorías para este grupo"
        const val A11Y_BUDGET = "Presupuesto"
        const val A11Y_DELETE_CATEGORY = "Eliminar categoría"
        const val BUTTON_NEW_CATEGORY = "Nueva categoría"

        /** "Presupuesto: 12,50 EUR/mes" — línea de categoría con presupuesto. */
        fun categoryBudgetLine(budget: String, currency: String): String =
            "Presupuesto: $budget $currency/mes"

        // ── Dialogs ──────────────────────────────────────────────────────

        // ConfirmDeleteCategoryDialog
        const val DELETE_CATEGORY_TITLE = "Eliminar categoría"
        fun deleteConfirmCategory(name: String): String =
            "¿Eliminar la categoría \"$name\"?"

        // ConfirmDeleteParticipantDialog
        const val DELETE_PARTICIPANT_TITLE = "Eliminar participante"
        fun deleteConfirmParticipant(name: String): String =
            "¿Eliminar a \"$name\"?"

        // AddCategoryDialog
        const val ADD_CATEGORY_TITLE = "Nueva categoría"
        const val ADD_CATEGORY_EXISTING_LABEL = "Categorías de este grupo:"
        const val ADD_CATEGORY_NAME_LABEL = "Nombre *"
        const val ADD_CATEGORY_PICK_ICON = "Elige un icono:"
        const val ADD_CATEGORY_CUSTOM_EMOJI_LABEL = "O escribe tu propio emoji"
        const val ADD_CATEGORY_CUSTOM_EMOJI_PLACEHOLDER = "Ej: 🌟"
        const val ADD_CATEGORY_ERROR_BLANK = "El nombre no puede estar vacío"
        const val ADD_CATEGORY_ERROR_DUPLICATE = "Ya existe una categoría con ese nombre"
        const val BUTTON_CREATE = "Crear"

        // BudgetEditDialog
        fun budgetDialogTitle(categoryName: String): String =
            "Presupuesto — $categoryName"
        const val BUDGET_INTRO =
            "Establece un límite mensual para esta categoría. " +
            "Déjalo vacío para eliminar el presupuesto."
        fun budgetInputLabel(currency: String): String =
            "Presupuesto mensual ($currency)"
        const val BUDGET_INPUT_PLACEHOLDER = "Ej: 200"
        const val BUDGET_ERROR_INVALID = "Introduce un importe válido"

        // DefaultSplitDialog
        const val SPLIT_TITLE = "Reparto por defecto"
        const val SPLIT_INTRO =
            "Define el porcentaje que corresponde a cada participante por defecto " +
            "al crear gastos. Deben sumar 100%."
        const val SPLIT_TOTAL_LABEL = "Total"
        const val SPLIT_DISTRIBUTE_EQUALLY = "Distribuir equitativamente"
        const val SPLIT_PERCENT_SUFFIX = "%"
        fun splitErrorNot100(currentPct: String): String =
            "Los porcentajes deben sumar 100% (ahora ${currentPct}%)"
    }

    // ── CreateGroupScreen ────────────────────────────────────────────────────

    object CreateGroup {
        // TopBar
        const val A11Y_BACK = "Volver"
        const val TITLE = "Nuevo grupo"

        // Hero / header
        const val HERO_HEADLINE = "Crea tu grupo"
        const val HERO_SUBTITLE = "Paso 1 de 2"
        const val NEXT_BUTTON = "Siguiente"

        // Form labels
        const val FIELD_NAME = "Nombre del grupo"
        const val FIELD_NAME_PLACEHOLDER = "Ej: Vacaciones Ibiza"
        const val ERROR_NAME_REQUIRED = "El nombre es obligatorio"

        const val FIELD_DESCRIPTION = "Descripción"
        const val FIELD_DESCRIPTION_PLACEHOLDER = "Opcional — describe el propósito del grupo"

        const val FIELD_CURRENCY = "Divisa"

        // Currency pill labels (código → etiqueta con emoji).
        const val CURRENCY_EUR = "🇪🇺 Euro"
        const val CURRENCY_USD = "🇺🇸 Dólar"
        const val CURRENCY_GBP = "🇬🇧 Libra"
        const val CURRENCY_MXN = "🇲🇽 Peso MX"
        const val CURRENCY_ARS = "🇦🇷 Peso AR"
        const val CURRENCY_COP = "🇨🇴 Peso CO"
    }

    // ── SpendDetailScreen ────────────────────────────────────────────────────

    object SpendDetail {
        // TopBar
        const val A11Y_BACK = "Volver"
        const val TITLE = "Detalle del gasto"
        const val A11Y_EDIT = "Editar gasto"

        // Sections
        const val SECTION_INFO = "Información"
        const val SECTION_PARTICIPANTS = "Participantes"

        // DetailRow labels
        const val LABEL_PAYER = "Pagó"
        const val LABEL_DATE = "Fecha"
        const val LABEL_SPLIT_TYPE = "Tipo de reparto"
        const val LABEL_RECURRENCE = "Repetición"
        const val LABEL_NEXT_OCCURRENCE = "Próxima generación"
        const val LABEL_ORIGIN = "Origen"
        const val ORIGIN_AUTO_GENERATED = "⚡ Generado automáticamente"
        const val LABEL_NOTES = "Notas"

        // SplitType labels (matches original byte-for-byte)
        const val SPLIT_EQUAL = "Equitativo"
        const val SPLIT_PERCENTAGE = "Por porcentaje"
        const val SPLIT_CUSTOM = "Por importe exacto"

        // Recurrence labels
        const val RECURRENCE_DAILY = "Diario"
        const val RECURRENCE_WEEKLY = "Semanal"
        const val RECURRENCE_MONTHLY = "Mensual"

        // Payer badge inside participants list
        const val PAYER_BADGE = "Pagó"

        // Receipt row
        const val RECEIPT_ATTACHED = "Ticket adjunto"

        /** "dd/MM/yyyy" — formato corto español. */
        fun formatDate(day: Int, month: Int, year: Int): String =
            "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
    }

    // ── AddSpendScreen ───────────────────────────────────────────────────────

    object AddSpend {
        // TopBar
        const val A11Y_BACK = "Volver"
        const val TITLE_ADD = "Añadir gasto"
        const val TITLE_EDIT = "Editar gasto"
        const val CELEBRATION_ADDED = "¡Gasto añadido!"
        const val CELEBRATION_EDITED = "¡Gasto actualizado!"

        // Header / form
        const val HEADER_NEW = "Nuevo gasto"
        const val HEADER_EDIT = "Editar gasto"
        const val SAVE_BUTTON_ADD = "Añadir gasto"
        const val SAVE_BUTTON_EDIT = "Guardar cambios"

        // Fields
        const val FIELD_CONCEPT = "Concepto"
        const val CONCEPT_PLACEHOLDER = "Ej: Cena en La Tagliatella"
        const val ERROR_CONCEPT_REQUIRED = "El concepto es obligatorio"

        const val FIELD_AMOUNT = "Importe"
        const val AMOUNT_PLACEHOLDER = "0.00"
        const val ERROR_AMOUNT_INVALID = "Introduce un importe válido"

        // A11y camera button
        const val A11Y_IMAGE_ATTACHED = "Imagen adjunta"
        const val A11Y_ATTACH_IMAGE = "Adjuntar imagen"

        // "¿Quién pagó?"
        const val SECTION_WHO_PAID = "¿Quién pagó?"

        // Categoría
        const val SECTION_CATEGORY = "Categoría"
        const val SUGGESTED_PREFIX = "💡 "
        const val NO_CATEGORY_PILL = "Sin categoría"

        // Reparto entre (EQUAL)
        const val SECTION_SPLIT_BETWEEN = "Reparto entre"
        const val ACTION_SELECT_ALL = "Seleccionar todos"
        const val ACTION_DESELECT_ALL = "Desmarcar todos"

        // Tipo de reparto (selector pills)
        const val SECTION_SPLIT_TYPE = "Tipo de reparto"
        const val SPLIT_TYPE_EQUAL = "Equitativo"
        const val SPLIT_TYPE_PERCENTAGE = "Porcentaje"
        const val SPLIT_TYPE_CUSTOM = "Exacto"

        // Split mode subtitles (bajo "Nuevo gasto")
        const val SUBTITLE_EQUAL = "Reparto equitativo"
        const val SUBTITLE_PERCENTAGE = "Reparto por porcentaje"
        const val SUBTITLE_CUSTOM = "Reparto por importe exacto"

        // SplitTotalsBadge
        const val PERCENTAGE_SECTION = "Porcentaje por persona"
        const val CUSTOM_SECTION = "Importe por persona"

        // Errors
        fun errorNoParticipants(): String = "Selecciona al menos un participante"
        fun errorPercentNot100(currentPct: String): String =
            "Los porcentajes deben sumar 100% (ahora ${currentPct}%)"
        fun errorCustomSumMismatch(sum: String, total: String): String =
            "La suma ($sum) no coincide con el total ($total)"

        /** "$share $currency" — share individual al lado del check. */
        fun shareLabel(share: String, currency: String): String = "$share $currency"

        /** "Total: $pct%" — texto de badge de porcentaje en el selector. */
        fun totalPctLabel(pct: String): String = "Total: $pct%"

        // Fecha
        const val A11Y_PICK_DATE = "Seleccionar fecha"
        const val DATE_PICKER_ACCEPT = "Aceptar"
        const val DATE_PICKER_CANCEL = "Cancelar"

        // Recurrencia
        const val SECTION_RECURRENCE = "Repetición"
        const val RECURRENCE_ONCE = "Una vez"
        const val RECURRENCE_WEEKLY_LABEL = "Semanal"
        const val RECURRENCE_MONTHLY_LABEL = "Mensual"
        const val RECURRENCE_WEEKLY_HINT = "💡 Este gasto se repetirá cada semana"
        const val RECURRENCE_MONTHLY_HINT = "💡 Este gasto se repetirá cada mes"

        /** "dd/MM/yyyy" — etiqueta del botón de fecha (igual que SpendDetail). */
        fun formatDate(day: Int, month: Int, year: Int): String =
            "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${year.toString().padStart(4, '0')}"
    }

    // ── AddParticipantsScreen (Paso 2 de creación de grupo) ──────────────────

    object AddParticipants {
        // TopBar
        const val TITLE = "Añadir participantes"
        const val STEP_LABEL = "Paso 2 de 2"

        // Form header
        const val FORM_HEADING = "Nuevo participante"
        const val FIELD_NAME = "Nombre *"
        const val FIELD_NAME_PLACEHOLDER = "Ej: Ana García"
        const val ERROR_NAME_REQUIRED = "El nombre es obligatorio"
        const val FIELD_EMAIL = "Email (opcional)"
        const val FIELD_EMAIL_PLACEHOLDER = "ana@ejemplo.com"
        const val BUTTON_ADD = "Añadir participante"

        // Counter header — "$n participante(s) añadido(s)" con plurales manuales
        // para preservar el comportamiento previo (Phase 7 había un patrón
        // similar en GroupList.participantsCount).
        fun participantsAddedCount(n: Int): String =
            "$n participante${if (n != 1) "s" else ""} añadido${if (n != 1) "s" else ""}"

        // Self toggle chip
        const val SELF_BADGE = "Soy yo"

        // Remove chip a11y
        const val A11Y_REMOVE = "Quitar participante"
        const val A11Y_REMOVE_LABEL = "Quitar participante"

        // Empty state
        const val EMPTY_HEADLINE = "Sin participantes todavía"
        const val EMPTY_SUBTITLE = "Añade al menos uno para continuar"
        const val EMPTY_EMOJI = "👥"

        // BottomBar
        const val CONTINUE_EMPTY = "Continuar sin participantes"
        fun continueWithCount(n: Int): String =
            "Abrir grupo ($n participante${if (n != 1) "s" else ""})"
    }

    // ── AddParticipantInGroupScreen ───────────────────────────────────────────

    object AddParticipantInGroup {
        // TopBar
        const val A11Y_BACK = "Volver"
        const val TITLE = "Añadir participante"
        const val SUBMIT_BUTTON = "Añadir participante"

        // Hero
        const val HERO_GROUP_FALLBACK = "Grupo"
        const val HERO_SUBTITLE = "Añadir nuevo miembro"

        // Fields
        const val FIELD_NAME = "Nombre *"
        const val FIELD_NAME_PLACEHOLDER = "Ej: Ana García"
        const val ERROR_NAME_REQUIRED = "El nombre es obligatorio"
        const val FIELD_EMAIL = "Email (opcional)"
        const val FIELD_EMAIL_PLACEHOLDER = "ana@ejemplo.com"
    }

    // ── Analytics (pantalla + secciones) ─────────────────────────────────────

    object Analytics {
        // ── Tab / cabecera general ────────────────────────────────────────
        const val SEARCH_PLACEHOLDER = "Buscar concepto"
        const val A11Y_CLEAR_FILTERS = "Limpiar filtros"
        const val SECTION_CATEGORY = "Categoría"
        const val SECTION_PERSON = "Persona"

        // Card titles
        const val CARD_MONTHLY = "Evolución mensual"
        const val CARD_BY_CATEGORY = "Por categoría"
        const val CARD_BY_PAYER = "Por pagador"
        const val TABLE_HEADER_MONTH = "Mes"
        const val TABLE_HEADER_CATEGORY = "Categoría"
        const val TABLE_HEADER_PAYER = "Pagador"

        // DonutChart
        const val DONUT_COUNT_SUFFIX = "categorías"
        const val DONUT_TOTAL_COUNT = "gastos"
        const val A11Y_EXPAND = "Ampliar"
        const val A11Y_CLOSE = "Cerrar"

        /** "+$n más" — sufijo cuando hay más entradas de las que se muestran en leyenda. */
        fun moreEntries(n: Int): String = "+$n más"

        /** "$amount $currency · $pct%" — fila de leyenda en donut. */
        fun legendLine(amount: String, currency: String, pct: String): String =
            "$amount $currency · $pct%"

        // ── PeriodFilterSelector ─────────────────────────────────────────
        const val PERIOD_LABEL = "Período"
        const val PERIOD_CHIP_CURRENT_MONTH = "Mes"
        const val PERIOD_CHIP_YEAR = "Año"
        const val PERIOD_CHIP_ALL = "Todo"
        const val PERIOD_CHIP_RANGE = "Rango"
        const val PERIOD_RANGE_FROM = "Desde"
        const val PERIOD_RANGE_TO = "Hasta"
        const val PERIOD_ACCEPT = "Aceptar"
        const val PERIOD_CANCEL = "Cancelar"

        // Period labels (AnalyticsPeriod → etiqueta)
        const val PERIOD_ALL = "Todos los periodos"
        const val PERIOD_CURRENT_MONTH = "Mes actual"
        fun periodMonth(monthName: String, year: Int): String = "$monthName $year"
        fun periodYear(year: Int): String = "Año $year"
        fun periodRange(from: String, to: String): String = "$from - $to"

        // ── AnalyticsSummaryCard ─────────────────────────────────────────
        const val TOTAL_LABEL = "Total gastado"
        fun totalValue(amount: String, currency: String): String = "$amount $currency"
        fun spendCountLabel(n: Int): String = "$n gastos"
        const val STAT_AVG = "Promedio"
        const val STAT_MEDIAN = "Mediana"
        const val STAT_MAX = "Mayor gasto"

        /** "$arrow $pct% vs. mes ant." — con flecha ▲/▼. */
        fun trendVsPrevMonth(arrow: String, pct: String): String =
            "$arrow $pct% vs. mes ant."

        // ── BudgetProgressCard ───────────────────────────────────────────
        const val BUDGET_TITLE = "Presupuesto mensual"
        fun budgetSpent(spent: String, budget: String, currency: String): String =
            "$spent / $budget $currency"
        fun budgetOverrun(over: String, currency: String): String =
            "⚠️ Superado en $over $currency"

        // ── NonEqualWarningRow ────────────────────────────────────────────
        const val A11Y_SHOW_NON_EQUAL_WARNING = "Mostrar aviso de gastos no equilibrados"
        fun nonEqualWarningCount(n: Int): String =
            "$n gasto${if (n > 1) "s" else ""} con reparto personalizado"
        const val NON_EQUAL_WARNING_BODY =
            "Las cifras de «Por pagador» muestran lo abonado, no la deuda real de cada persona. " +
            "Consulta la pestaña Balances para ver los saldos exactos."
        const val NON_EQUAL_WARNING_ACK = "Entendido"

        // ── SettlementNetRow / sección liquidación ───────────────────────
        const val SETTLE_SECTION_TITLE = "Resumen de liquidaciones"
        const val SETTLE_ALL_CLEAR = "✅ Todas las cuentas están saldadas"

        // ── Filtros vacíos ────────────────────────────────────────────────
        const val EMOJI_EMPTY_FILTER = "🔍"
        const val EMPTY_FILTER_HEADLINE = "Sin resultados para los filtros aplicados"

        // ── Fullscreen dialog ─────────────────────────────────────────────
        const val FULLSCREEN_EMPTY_DONUT_BARS = "No hay datos para mostrar en este gráfico"
        const val FULLSCREEN_EMPTY_RANKING = "No hay datos para mostrar en el ranking"

        /** "$icon $label" — fila de tabla con icono + nombre. */
        fun tableRowLabel(icon: String, label: String): String = "$icon $label"

        // ── BreakdownTable / Categoría sin nombre ─────────────────────────
        const val UNCATEGORIZED_NAME = "Sin categoría"

        /** "$n gasto(s) · $pct%" — subtítulo de cada entry en ranking. */
        fun rankingSubtitle(spendCount: Int, pct: String): String =
            "$spendCount gasto${if (spendCount == 1) "" else "s"} · $pct%"

        /** "$amount $currency" — columna final de cada entry en ranking. */
        fun rankingAmount(amount: String, currency: String): String = "$amount $currency"

        /** "$amount $currency · $pct%" — fila horizontal de barras. */
        fun barRowAmount(amount: String, currency: String): String = "$amount $currency"
        fun barRowPct(pct: String): String = "$pct%"
        const val BARS_LEGEND_SUFFIX_TEMPLATE = "Importes en "
        /** "Importes en $currency" — línea bajo las barras. */
        fun barsAmountsIn(currency: String): String = "$BARS_LEGEND_SUFFIX_TEMPLATE$currency"

        // Tabla: headers
        const val TABLE_HEADER_GASTOS = "Gastos"
        const val TABLE_HEADER_RANK_NUMBER = "#"
        const val TABLE_HEADER_RANK_AMOUNT = "Cantidad"
        const val TABLE_HEADER_PCT = "%"

        // Fila de pagador desconocido
        const val PAYER_UNKNOWN_FALLBACK = "Desconocido"

        // ── AnalyticsSpendList ────────────────────────────────────────────
        fun listHeader(visible: Int, total: Int): String =
            "Gastos ($visible de $total)"
        fun showMore(remaining: Int): String =
            "Mostrar más ($remaining restantes)"
        const val PAYER_DATE_SEPARATOR = " · "

        /** "$payerName · $dateFormatted" — segunda línea de la fila. */
        fun payerAndDate(payerName: String, dateFormatted: String): String =
            "$payerName$PAYER_DATE_SEPARATOR$dateFormatted"

        // ── ExportFab ─────────────────────────────────────────────────────
        const val EXPORT_BUTTON = "Exportar"
        const val A11Y_EXPORT = "Exportar"
        const val EXPORT_PDF = "PDF"
        const val EXPORT_EXCEL = "Excel"
        const val EXPORT_CSV = "CSV"
        const val EXPORT_TEXT = "Texto"
        const val A11Y_EXPORT_PDF = "Exportar PDF"
        const val A11Y_EXPORT_EXCEL = "Exportar Excel"
        const val A11Y_EXPORT_CSV = "Exportar CSV"
        const val A11Y_EXPORT_TEXT = "Exportar texto"

        // ── ExpandedTab titles (los valores del enum) ─────────────────────
        const val TAB_ROSQUILLA = "Rosquilla"
        const val TAB_BARRAS = "Barras"
        const val TAB_RANKING = "Ranking"
    }
}
