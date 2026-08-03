package com.example.divvyup.integration.ui

import kotlin.math.abs
import kotlin.math.round

/**
 * Strings centralizados para las 5 pantallas principales del rediseño visual
 * (Phase 6). Antes vivían inline en cada Composable como literales; ahora son
 * una sola fuente para editar la copia.
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
}
