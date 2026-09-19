package com.example.divvyup.application

/**
 * Servicio de sugerencia de categoría basado en palabras clave.
 * Puro Kotlin — sin dependencias de plataforma. KMP-compatible.
 *
 * Devuelve el nombre exacto de la categoría (tal como está en BD) o null si no hay match.
 */
object CategorySuggestionService {

    /**
     * Reglas: categoryName → lista de keywords en español (minúsculas, sin tildes).
     * El orden importa: la primera regla que haga match gana.
     * "Supermercado" va antes que "Comida" para que "mercadona" no caiga en Comida.
     */
    private val rules: List<Pair<String, List<String>>> = listOf(

        "Supermercado" to listOf(
            "mercadona", "lidl", "aldi", "carrefour", "dia ", " dia", "eroski",
            "hipercor", "alcampo", "consum", "caprabo", "supersol", "froiz",
            "coviran", "spar", "bonpreu", "simply", "ulabox", "hacendado",
            "supermercado", "compra semanal", "compra mensual", "compra del mes",
            "compra del super", "super mercado", "supermercat", "la compra"
        ),

        "Comida" to listOf(
            "restaurante", "cena", "comida", "almuerzo", "desayuno", "merienda",
            "pizza", "sushi", "hamburguesa", "burger", "mcdonalds", "burger king",
            "kebab", "bocadillo", "tapas", "vermut", "vermú", "cafe ", "cafetería",
            "panadería", "pastelería", "heladería", "churros", "bar ", "bares",
            "la tagliatella", "vips", "pans", "subway", "kfc", "telepizza",
            "dominos", "fosters", "chino", "japonés", "italiano", "mexicano",
            "comida para llevar", "delivery", "just eat", "glovo", "uber eats",
            "brunch", "picnic", "picoteo", "aperitivo"
        ),

        "Transporte" to listOf(
            "taxi", "uber", "cabify", "blablacar", "gasolina", "combustible",
            "carburante", "repsol", "cepsa", "bp ", "parking", "aparcamiento",
            "metro", "autobus", "autobús", "bus ", "renfe", "ave ", "cercanías",
            "tren", "avion", "avión", "vueling", "iberia", "ryanair", "easyjet",
            "peaje", "autopista", "ferri", "ferry", "barco", "bicicleta", "patinete",
            "carsharing", "car sharing", "share now", "emov", "bolt", "lime",
            "billete", "abono transporte", "tarjeta transporte"
        ),

        "Alojamiento" to listOf(
            "hotel", "airbnb", "hostal", "pensión", "hostel", "albergue",
            "apartamento", "alquiler", "renta", "habitación", "booking",
            "trivago", "casas rurales", "casa rural", "camping", "bungalow",
            "resort", "villa", "finca", "glamping", "hipoteca", "comunidad"
        ),

        "Ocio" to listOf(
            "cine", "teatro", "concierto", "festival", "show", "espectáculo",
            "parque", "zoo", "acuario", "museo", "exposición", "entrada",
            "discoteca", "club", "sala de fiestas", "bolos", "laser", "karting",
            "escape room", "netflix", "hbo", "disney", "amazon prime", "spotify",
            "steam", "playstation", "xbox", "nintendo", "videojuego", "juego",
            "copa", "copas", "cerveza", "vino", "gin", "mojito", "cocktail",
            "karaoke", "bolera", "billar", "dardos", "fiesta", "cumpleaños"
        ),

        "Compras" to listOf(
            "zara", "h&m", "hm ", "mango", "stradivarius", "bershka", "pull",
            "pull&bear", "massimo", "primark", "amazon", "ikea", "el corte ingles",
            "corte inglés", "mediamarkt", "fnac", "decathlon", "footlocker",
            "ropa", "zapatos", "zapatillas", "bolso", "gafas", "perfume",
            "cosmética", "maquillaje", "electrodoméstico", "tecnología", "móvil",
            "ordenador", "tablet", "libro", "librería", "papelería", "regalo",
            "bazar", "tienda"
        ),

        "Salud" to listOf(
            "farmacia", "medicamento", "medicina", "pastilla", "médico", "doctor",
            "dentista", "dentista", "oculista", "óptica", "hospital", "clínica",
            "urgencias", "analítica", "radiografía", "fisio", "fisioterapia",
            "parafarmacia", "vitaminas", "suplemento", "consulta", "revisión"
        ),

        "Deportes" to listOf(
            "pádel", "padel", "tenis", "fútbol", "futbol", "baloncesto",
            "piscina", "natación", "natacion", "ciclismo", "running", "maratón",
            "triatlón", "yoga", "pilates", "crossfit", "gimnasio", "gym ",
            "senderismo", "escalada", "surf", "esquí", "ski", "snowboard",
            "golf", "squash", "bádminton", "badminton", "voleibol", "rugby",
            "boxeo", "judo", "karate", "artes marciales", "spinning"
        ),

        "Viajes" to listOf(
            "viaje", "vacaciones", "excursión", "excursion", "escapada", "ruta",
            "vuelo", "maleta", "equipaje", "seguro de viaje", "visado", "pasaporte",
            "transfer", "traslado", "touristik", "touroperador", "agencia de viajes",
            "crucero", "travesía", "mochilero", "interrail"
        ),

        "Mascotas" to listOf(
            "veterinario", "veterinaria", "pienso", "comida para el perro",
            "comida para el gato", "perro", "gato", "mascota", "tienda de animales",
            "kiwoko", "maxicostas", "croquetas", "collar", "correa", "jaula",
            "acuario", "peces", "hamster", "conejo", "loro", "caballo"
        )
    )

    /**
     * Normaliza el texto: minúsculas + quitar tildes + colapsar espacios.
     */
    private fun normalize(text: String): String =
        text.lowercase()
            .replace('á', 'a').replace('é', 'e').replace('í', 'i')
            .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
            .replace('à', 'a').replace('è', 'e').replace('ï', 'i')
            .replace('ò', 'o').replace('ù', 'u')
            .replace("ñ", "n")
            .trim()

    /**
     * Devuelve el nombre de la categoría sugerida (tal como está en BD)
     * o `null` si no hay match.
     */
    fun suggest(concept: String): String? {
        if (concept.isBlank()) return null
        val normalized = normalize(concept)
        return rules.firstOrNull { (_, keywords) ->
            keywords.any { kw -> normalized.contains(normalize(kw)) }
        }?.first
    }
}

