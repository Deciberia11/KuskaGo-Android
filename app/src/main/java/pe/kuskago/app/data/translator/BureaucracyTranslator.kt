package pe.kuskago.app.data.translator

import java.text.Normalizer

/**
 * Traductor de burocracia SIN INTERNET (algoritmo basado en reglas).
 *
 * Etapas:
 *  1. Normalización: minúsculas, sin tildes ni signos → permite comparar "Posesión" con "posesion".
 *  2. Detección de términos: diccionario de términos legales/administrativos. Se buscan
 *     coincidencias exactas y también aproximadas con distancia de Levenshtein, para tolerar
 *     errores de tipeo o plurales ("constansia", "observaciones").
 *  3. Simplificación: reglas que reemplazan frases formales por frases sencillas
 *     ("a efectos de" → "para", "sírvase" → "por favor").
 *  4. Extracción: plazos (días, meses, fechas), documentos mencionados y acciones que
 *     la persona debe realizar (oraciones con "debe", "tiene que", "presentar", "pagar"...).
 */
object BureaucracyTranslator {

    data class Term(
        val label: String,
        val official: String,
        val simple: String,
        val patterns: List<String>,
        val isDocument: Boolean = false,
    )

    data class Result(
        val simplifiedText: String,
        val terms: List<Term>,
        val deadlines: List<String>,
        val documents: List<String>,
        val actions: List<String>,
    ) {
        val isEmpty: Boolean get() = terms.isEmpty() && deadlines.isEmpty() && actions.isEmpty()
    }

    // ------------------------------------------------------------------
    // 1. Normalización
    // ------------------------------------------------------------------
    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9ñ ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    // ------------------------------------------------------------------
    // 2. Diccionario
    // ------------------------------------------------------------------
    val GLOSSARY: List<Term> = listOf(
        Term("Constancia de posesión", "Constancia de posesión debidamente suscrita",
            "Documento que confirma que usas o vives en ese terreno, firmado por tu municipalidad o comunidad.",
            listOf("constancia de posesion"), isDocument = true),
        Term("Declaratoria de herederos", "Declaratoria judicial o notarial de herederos",
            "Documento que dice quiénes son los familiares que heredan los bienes de una persona que falleció.",
            listOf("declaratoria de herederos", "declaracion de herederos"), isDocument = true),
        Term("Sucesión intestada", "Trámite de sucesión intestada",
            "Repartir los bienes de alguien que murió sin dejar testamento, siguiendo lo que dice la ley.",
            listOf("sucesion intestada")),
        Term("Servidumbre de paso", "Constitución de servidumbre de paso",
            "Permiso para cruzar por el terreno de un vecino cuando es la única forma de llegar al tuyo.",
            listOf("servidumbre de paso", "servidumbre")),
        Term("Subsanar", "Subsanar las observaciones",
            "Corregir lo que está mal o agregar lo que falta en tu trámite.",
            listOf("subsanar", "subsane", "subsanacion", "subsanado")),
        Term("Observación", "Expediente observado",
            "La entidad revisó tu trámite y encontró un error o algo que falta.",
            listOf("observacion", "observaciones", "observado", "observada")),
        Term("Inadmisible", "Declarar inadmisible la solicitud",
            "Tu solicitud no se acepta por ahora porque falta algo. Si lo completas, pueden aceptarla.",
            listOf("inadmisible")),
        Term("Improcedente", "Declarar improcedente",
            "Tu solicitud fue rechazada porque no cumple lo que pide la ley.",
            listOf("improcedente")),
        Term("Notificación", "Notificación administrativa",
            "Aviso oficial que te envía la entidad. Desde ese día empiezan a contar los plazos.",
            listOf("notificacion", "notificado", "notificada", "notifiquese")),
        Term("Días hábiles", "Plazo en días hábiles",
            "Solo se cuentan los días de lunes a viernes. No cuentan sábados, domingos ni feriados.",
            listOf("dias habiles", "dia habil")),
        Term("Apercibimiento", "Bajo apercibimiento",
            "Advertencia: si no haces lo que te piden, habrá una consecuencia (por ejemplo, cierran tu trámite).",
            listOf("apercibimiento")),
        Term("Recurso de reconsideración", "Interponer recurso de reconsideración",
            "Pedir a la misma entidad que vuelva a revisar su decisión, presentando una prueba nueva.",
            listOf("recurso de reconsideracion", "reconsideracion")),
        Term("Recurso de apelación", "Interponer recurso de apelación",
            "Pedir a una autoridad superior que revise la decisión porque no estás de acuerdo.",
            listOf("recurso de apelacion", "apelacion", "apelar")),
        Term("Silencio administrativo positivo", "Aplicación del silencio administrativo positivo",
            "Si la entidad no te responde en el plazo, tu pedido se considera aprobado.",
            listOf("silencio administrativo positivo", "silencio positivo")),
        Term("Silencio administrativo negativo", "Aplicación del silencio administrativo negativo",
            "Si la entidad no te responde en el plazo, tu pedido se considera rechazado y puedes reclamar.",
            listOf("silencio administrativo negativo", "silencio negativo")),
        Term("TUPA", "Texto Único de Procedimientos Administrativos (TUPA)",
            "Lista oficial de cada entidad con sus trámites, requisitos, costos y plazos.",
            listOf("tupa", "texto unico de procedimientos administrativos")),
        Term("Derecho de trámite", "Pago por derecho de trámite",
            "El pago que debes hacer para que la entidad atienda tu trámite.",
            listOf("derecho de tramite", "tasa", "derecho de pago"), isDocument = true),
        Term("Declaración jurada", "Declaración jurada con firma del administrado",
            "Documento donde afirmas, con tu firma, que lo que dices es verdad. Mentir en él es delito.",
            listOf("declaracion jurada"), isDocument = true),
        Term("Firma legalizada", "Legalización de firma ante notario",
            "Un notario confirma que la firma del documento es realmente tuya.",
            listOf("firma legalizada", "legalizacion", "legalizada", "legalizado")),
        Term("Copia fedateada", "Copia certificada o fedateada",
            "Copia que la entidad sella para confirmar que es igual al documento original.",
            listOf("copia fedateada", "fedateada", "copia certificada", "fedatario"), isDocument = true),
        Term("Partida registral", "Partida registral en SUNARP",
            "La ficha oficial de tu terreno o casa en Registros Públicos (SUNARP).",
            listOf("partida registral", "partida electronica", "ficha registral"), isDocument = true),
        Term("Título de propiedad", "Título de propiedad inscrito",
            "Documento que prueba que eres dueño o dueña del terreno o casa.",
            listOf("titulo de propiedad"), isDocument = true),
        Term("Saneamiento físico legal", "Saneamiento físico legal del predio",
            "Poner en orden los papeles y las medidas del terreno para que todo coincida y quede registrado.",
            listOf("saneamiento fisico legal", "saneamiento")),
        Term("Predio", "Predio rústico o urbano",
            "Terreno o inmueble (casa, lote, chacra).",
            listOf("predio", "predios")),
        Term("Inmueble", "Bien inmueble",
            "Algo que no se puede mover: un terreno, una casa o un local.",
            listOf("inmueble", "inmuebles")),
        Term("Poseedor", "Poseedor o posesionario",
            "La persona que vive en el terreno o lo usa, aunque todavía no tenga título.",
            listOf("posesionario", "poseedor", "posesionaria", "poseedora")),
        Term("Colindantes", "Propietarios colindantes",
            "Los vecinos cuyos terrenos están pegados al tuyo.",
            listOf("colindante", "colindantes")),
        Term("Linderos", "Linderos y medidas perimétricas",
            "Los límites de tu terreno: dónde empieza y dónde termina.",
            listOf("linderos", "lindero")),
        Term("Memoria descriptiva", "Memoria descriptiva del predio",
            "Documento técnico que describe el terreno: ubicación, medidas y límites.",
            listOf("memoria descriptiva"), isDocument = true),
        Term("Plano perimétrico", "Plano perimétrico y de ubicación",
            "Dibujo del terreno con sus límites, medidas y ubicación, hecho por un profesional.",
            listOf("plano perimetrico", "plano de ubicacion", "croquis"), isDocument = true),
        Term("Búsqueda catastral", "Certificado de búsqueda catastral",
            "Documento de SUNARP que dice si tu terreno ya está registrado a nombre de alguien.",
            listOf("busqueda catastral"), isDocument = true),
        Term("Carga o gravamen", "Cargas y gravámenes",
            "Una deuda o limitación sobre el bien, por ejemplo una hipoteca o un embargo.",
            listOf("gravamen", "gravamenes", "cargas y gravamenes")),
        Term("Usufructo", "Derecho de usufructo",
            "Derecho a usar algo y aprovechar lo que produce, aunque el dueño sea otra persona.",
            listOf("usufructo")),
        Term("Carta poder", "Carta poder con firma legalizada",
            "Documento con el que autorizas a otra persona para que haga el trámite por ti.",
            listOf("carta poder", "poder notarial", "apoderado", "apoderada"), isDocument = true),
        Term("Administrado", "El administrado",
            "Tú: la persona que hace el trámite ante la entidad.",
            listOf("administrado", "administrada")),
        Term("Expediente", "Número de expediente",
            "La carpeta con todos los papeles de tu trámite. Tiene un número para hacer seguimiento.",
            listOf("expediente", "expedientes")),
        Term("Mesa de partes", "Mesa de partes",
            "La ventanilla (física o virtual) donde se entregan documentos a la entidad.",
            listOf("mesa de partes")),
        Term("Resolución", "Resolución administrativa",
            "La decisión oficial y por escrito de la entidad sobre tu trámite.",
            listOf("resolucion", "resuelve", "resuelto")),
        Term("Otorgamiento", "Otorgamiento",
            "Entrega o aprobación de algo que pediste (por ejemplo, una licencia).",
            listOf("otorgamiento", "otorgar", "otorga")),
        Term("Prescripción adquisitiva", "Prescripción adquisitiva de dominio",
            "Volverte dueño de un terreno porque lo usas como dueño desde hace muchos años (por lo general 10).",
            listOf("prescripcion adquisitiva", "prescripcion adquisitiva de dominio")),
        Term("Cónyuge", "Cónyuge supérstite",
            "Esposo o esposa. \"Supérstite\" significa que sigue vivo(a).",
            listOf("conyuge", "superstite")),
        Term("Acta de defunción", "Acta o partida de defunción",
            "Documento de RENIEC que confirma que una persona falleció.",
            listOf("acta de defuncion", "partida de defuncion"), isDocument = true),
        Term("Acta de nacimiento", "Acta o partida de nacimiento",
            "Documento de RENIEC que confirma tu nacimiento y quiénes son tus padres.",
            listOf("acta de nacimiento", "partida de nacimiento"), isDocument = true),
        Term("Causante", "El causante",
            "La persona que falleció y deja bienes para heredar.",
            listOf("causante", "de cujus")),
        Term("Heredero forzoso", "Herederos forzosos",
            "Familiares que por ley siempre heredan: hijos, cónyuge y, si no hay hijos, padres.",
            listOf("heredero forzoso", "herederos forzosos")),
        Term("Domicilio legal", "Domicilio legal o fiscal",
            "La dirección oficial donde te envían avisos y notificaciones.",
            listOf("domicilio legal", "domicilio fiscal", "domicilio procesal")),
        Term("RUC", "Registro Único de Contribuyentes (RUC)",
            "Número de SUNAT que identifica a una persona o negocio para pagar impuestos.",
            listOf("ruc", "registro unico de contribuyentes"), isDocument = true),
        Term("Zonificación", "Compatibilidad de uso y zonificación",
            "Revisión de si en esa zona está permitido el tipo de negocio que quieres abrir.",
            listOf("zonificacion", "compatibilidad de uso")),
        Term("ITSE", "Inspección Técnica de Seguridad en Edificaciones (ITSE)",
            "Revisión de seguridad del local (salidas, extintores, instalaciones eléctricas).",
            listOf("itse", "inspeccion tecnica de seguridad", "defensa civil")),
        Term("Archivo del expediente", "Archivo del expediente",
            "Cierran tu trámite y ya no siguen revisándolo.",
            listOf("archivese", "archivo del expediente", "archivar")),
        Term("Desistimiento", "Desistimiento del procedimiento",
            "Cuando tú decides abandonar o cancelar tu trámite.",
            listOf("desistimiento", "desistir")),
        Term("Caducidad", "Caducidad del procedimiento",
            "Que algo vence y deja de valer porque pasó el tiempo.",
            listOf("caducidad", "caduca", "caducado")),
        Term("Vigente", "Documento vigente",
            "Que todavía vale y no ha vencido.",
            listOf("vigente", "vigencia")),
        Term("Notario", "Notario público",
            "Abogado autorizado para dar fe de documentos y firmas.",
            listOf("notario", "notaria", "notarial")),
        Term("Juzgado de paz", "Juzgado de Paz",
            "Autoridad de justicia cercana a tu comunidad para casos sencillos.",
            listOf("juzgado de paz", "juez de paz")),
        Term("Testamento", "Testamento",
            "Documento donde una persona dice cómo repartir sus bienes después de morir.",
            listOf("testamento")),
        Term("DNI", "Documento Nacional de Identidad",
            "Tu documento de identidad de RENIEC.",
            listOf("dni", "documento nacional de identidad"), isDocument = true),
        Term("Voucher de pago", "Comprobante de pago",
            "El papel o constancia que te dan al pagar, como prueba del pago.",
            listOf("voucher", "comprobante de pago", "recibo de pago", "constancia de pago"), isDocument = true),
    )

    // ------------------------------------------------------------------
    // Distancia de Levenshtein (similitud entre palabras)
    // ------------------------------------------------------------------
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /** Errores tolerados según el largo de la frase buscada. */
    private fun tolerance(len: Int) = when {
        len < 8 -> 0     // palabras cortas: solo coincidencia exacta (evita "predio" ≈ "precio")
        len < 14 -> 1
        else -> 2
    }

    private fun containsTerm(tokens: List<String>, normText: String, pattern: String): Boolean {
        // Coincidencia exacta respetando límites de palabra
        if (Regex("(^| )" + Regex.escape(pattern) + "( |$)").containsMatchIn(normText)) return true
        val tol = tolerance(pattern.length)
        if (tol == 0) return false
        // Coincidencia aproximada: ventana deslizante con la misma cantidad de palabras
        val n = pattern.split(" ").size
        if (tokens.size < n) return false
        for (i in 0..tokens.size - n) {
            val window = tokens.subList(i, i + n).joinToString(" ")
            if (kotlin.math.abs(window.length - pattern.length) <= tol && levenshtein(window, pattern) <= tol) return true
        }
        return false
    }

    fun detectTerms(text: String): List<Term> {
        val norm = normalize(text)
        if (norm.isBlank()) return emptyList()
        val tokens = norm.split(" ")
        return GLOSSARY.filter { t -> t.patterns.any { containsTerm(tokens, norm, it) } }
    }

    // ------------------------------------------------------------------
    // 3. Reglas de simplificación (sobre el texto original, respetando tildes)
    // ------------------------------------------------------------------
    private val RULES: List<Pair<Regex, String>> = listOf(
        "dentro del plazo de (\\d+) d[ií]as h[aá]biles" to "en un máximo de $1 días hábiles (de lunes a viernes, sin contar feriados)",
        "en un plazo (?:no mayor )?de (\\d+) d[ií]as h[aá]biles" to "en un máximo de $1 días hábiles (de lunes a viernes, sin contar feriados)",
        ",?\\s*bajo apercibimiento de" to ". Si no lo hace, se va a",
        "mediante la presente se (?:le )?notifica|por medio de la presente se (?:le )?notifica|se (?:le )?notifica" to "le avisamos",
        "al administrado|a la administrada|al \\(a la\\) administrado\\(a\\)" to "a usted",
        "disponer el archivo del expediente|archivar el expediente|el archivo del expediente" to "cerrar su trámite",
        "acreditar" to "demostrar",
        "conforme al" to "según el",
        "a efectos de|a fin de|con el objeto de|con la finalidad de|con el prop[oó]sito de" to "para",
        "en virtud de|en m[eé]rito a|en m[eé]rito de" to "por",
        "de conformidad con|conforme a lo dispuesto (?:en|por)|de acuerdo a lo establecido en|seg[uú]n lo dispuesto en" to "según",
        "s[ií]rvase subsanar" to "por favor, corrija",
        "s[ií]rvase presentar" to "por favor, presente",
        "s[ií]rvase" to "por favor,",
        "se le requiere|se le solicita|se le exhorta a" to "le pedimos",
        "el \\(la\\) administrado\\(a\\)|el administrado|la administrada" to "usted",
        "el \\(la\\) solicitante|el solicitante|la solicitante|el recurrente|la recurrente" to "usted",
        "debiendo" to "y tiene que",
        "deber[aá]n" to "tienen que",
        "deber[aá]" to "tiene que",
        "adjuntar" to "agregar",
        "adjunte" to "agregue",
        "acompa[nñ]ar" to "presentar junto con",
        "previamente" to "antes",
        "posteriormente" to "después",
        "asimismo" to "también",
        "no obstante|sin perjuicio de ello" to "pero",
        "en caso de que|en el supuesto de que" to "si",
        "por lo expuesto|en tal sentido|por consiguiente" to "por eso",
        "otorgamiento" to "entrega",
        "subsanar" to "corregir",
        "subsane" to "corrija",
        "suscrita" to "firmada",
        "suscrito" to "firmado",
        "suscribir" to "firmar",
        "debidamente" to "",
        "dicho documento|el referido documento|el citado documento" to "ese documento",
        "la presente" to "esta carta",
        "a la brevedad posible" to "lo antes posible",
        "proceder[aá] a" to "va a",
    ).map { (p, r) ->
        // Solo palabras completas: (?<!\p{L}) y (?!\p{L}) funcionan también con letras con tilde.
        Regex("(?<!\\p{L})(?:$p)(?!\\p{L})", RegexOption.IGNORE_CASE) to r
    }

    fun simplify(text: String): String {
        var out = text.replace(Regex("\\s+"), " ").trim()
        for ((rx, rep) in RULES) out = out.replace(rx, rep)
        out = out
            .replace(";", ".")
            .replace(Regex(",\\s*\\."), ".")
            .replace(Regex(" {2,}"), " ")
            .replace(Regex(" ([.,])"), "$1")
        // Mayúscula al inicio de cada oración
        return out.split(Regex("(?<=[.!?])\\s+")).joinToString(" ") { s ->
            s.replaceFirstChar { c -> c.uppercaseChar() }
        }
    }

    // ------------------------------------------------------------------
    // 4. Extracción de plazos, documentos y acciones
    // ------------------------------------------------------------------
    private val DEADLINE_RX = Regex(
        "(\\b\\d{1,3}\\s*(?:\\([a-záéíóú ]+\\)\\s*)?(?:d[ií]as?(?:\\s+h[aá]biles|\\s+calendario|\\s+naturales)?|meses?|a[nñ]os?|horas?)\\b)" +
            "|(\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b)",
        RegexOption.IGNORE_CASE,
    )

    private val ACTION_WORDS = listOf(
        "debe", "debera", "tiene que", "presentar", "presente", "adjuntar", "adjunte", "pagar", "abone",
        "subsanar", "subsane", "corregir", "acudir", "acercarse", "apersonarse", "remitir", "entregar", "sirvase", "le pedimos",
    )

    private fun sentences(text: String): List<String> =
        text.replace(Regex("\\s+"), " ").split(Regex("(?<=[.;!?])\\s+"))
            .map { it.trim() }.filter { it.length > 3 }

    fun extractDeadlines(text: String): List<String> =
        DEADLINE_RX.findAll(text).map { it.value.trim() }.distinct().take(5).toList()

    fun extractActions(simplified: String): List<String> =
        sentences(simplified).filter { s ->
            val n = normalize(s)
            ACTION_WORDS.any { n.contains(it) }
        }.take(5)

    // ------------------------------------------------------------------
    // Proceso completo
    // ------------------------------------------------------------------
    fun translate(text: String): Result {
        val terms = detectTerms(text)
        val simplified = simplify(text)
        return Result(
            simplifiedText = simplified,
            terms = terms,
            deadlines = extractDeadlines(text),
            documents = terms.filter { it.isDocument }.map { it.label },
            actions = extractActions(simplified),
        )
    }

    /** Texto de ejemplo para probar el traductor. */
    const val SAMPLE =
        "Mediante la presente se notifica al administrado que su expediente N° 2025-0456 ha sido observado. " +
            "Sírvase subsanar las observaciones dentro del plazo de 10 días hábiles, debiendo adjuntar la constancia de posesión " +
            "debidamente suscrita y la copia fedateada de su DNI, bajo apercibimiento de declarar inadmisible su solicitud " +
            "y disponer el archivo del expediente. Asimismo, deberá acreditar el pago por derecho de trámite conforme al TUPA vigente."
}
