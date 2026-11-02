package pe.kuskago.app.data.identity

import java.text.Normalizer

/**
 * Datos leídos de la foto de un DNI peruano (anverso y/o reverso).
 * Todo es opcional: el usuario siempre revisa y corrige antes de guardar.
 */
data class DniData(
    val dni: String? = null,
    val surnames: String? = null,
    val givenNames: String? = null,
    val address: String? = null,
    val district: String? = null,
    val province: String? = null,
    val department: String? = null,
) {
    val fullName: String?
        get() = listOfNotNull(givenNames, surnames).joinToString(" ").takeIf { it.isNotBlank() }

    val isEmpty: Boolean
        get() = listOfNotNull(dni, surnames, givenNames, address, district, province, department).isEmpty()

    /** Combina lo leído en dos fotos (anverso + reverso). */
    fun merge(o: DniData) = DniData(
        dni ?: o.dni, surnames ?: o.surnames, givenNames ?: o.givenNames,
        address ?: o.address, district ?: o.district, province ?: o.province, department ?: o.department,
    )
}

/**
 * Interpreta el texto reconocido (OCR) de un DNI peruano.
 *
 * Fuentes que usa, en orden de confianza:
 *  1. Zona MRZ (las líneas con "<" del reverso del DNIe): "I<PER12345678<..." y "APELLIDO<APELLIDO<<NOMBRES".
 *  2. Etiquetas impresas: "Primer Apellido", "Pre Nombres", "Dirección", "Departamento / Provincia / Distrito".
 *  3. Un número de 8 dígitos suelto como último recurso para el DNI.
 */
object DniTextParser {

    private fun norm(s: String) =
        Normalizer.normalize(s.uppercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()

    private fun clean(s: String?) = s
        ?.replace(Regex("[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#°/-]"), " ")
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.length >= 2 }

    private fun title(s: String?): String? = clean(s)?.lowercase()?.split(" ")?.joinToString(" ") { w ->
        if (w.length <= 2 && w in setOf("de", "la", "el", "y", "del")) w else w.replaceFirstChar { it.uppercase() }
    }

    fun parse(raw: String): DniData {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val n = lines.map { norm(it) }

        // ---------- 1. MRZ ----------
        var dni: String? = null
        var surnames: String? = null
        var given: String? = null
        val mrzLines = n.map { it.replace(" ", "") }.filter { it.count { c -> c == '<' } >= 3 }
        mrzLines.firstOrNull { it.contains("PER") && Regex("I.?PER\\d{8}").containsMatchIn(it) }?.let {
            dni = Regex("I.?PER(\\d{8})").find(it)?.groupValues?.get(1)
        }
        mrzLines.firstOrNull { it.contains("<<") && !it.any { c -> c.isDigit() } }?.let { line ->
            val parts = line.split("<<").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                surnames = parts[0].replace("<", " ").trim()
                given = parts[1].replace("<", " ").trim()
            }
        }

        // ---------- 2. Etiquetas ----------
        /** Valor de una etiqueta: lo que sigue en la misma línea o, si no hay, la línea siguiente. */
        fun valueAfter(vararg labels: String): String? {
            for (i in n.indices) {
                val label = labels.firstOrNull { n[i].contains(it) } ?: continue
                val rest = n[i].substringAfter(label).trim(' ', ':', '-')
                if (rest.length >= 2) return lines[i].takeLast(rest.length).trim()
                if (i + 1 < lines.size && labels.none { n[i + 1].contains(it) }) return lines[i + 1]
            }
            return null
        }

        if (surnames == null) {
            val ap1 = valueAfter("PRIMER APELLIDO", "APELLIDO PATERNO")
            val ap2 = valueAfter("SEGUNDO APELLIDO", "APELLIDO MATERNO")
            surnames = listOfNotNull(ap1, ap2).joinToString(" ").takeIf { it.isNotBlank() }
        }
        if (given == null) given = valueAfter("PRE NOMBRES", "PRENOMBRES", "NOMBRES")

        var address = valueAfter("DIRECCION", "DOMICILIO")
        var department: String? = null
        var province: String? = null
        var district: String? = null

        // Formato "LIMA/LIMA/MIRAFLORES" o encabezado "DEPARTAMENTO PROVINCIA DISTRITO" + línea de valores
        val ubiIdx = n.indexOfFirst { it.contains("DEPARTAMENTO") && it.contains("DISTRITO") }
        if (ubiIdx >= 0 && ubiIdx + 1 < lines.size) {
            val v = lines[ubiIdx + 1]
            val parts = v.split("/", "-").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size == 3) {
                department = parts[0]; province = parts[1]; district = parts[2]
            } else {
                district = v   // no se puede separar con seguridad: el usuario lo corrige
            }
        } else {
            department = valueAfter("DEPARTAMENTO")
            province = valueAfter("PROVINCIA")
            district = valueAfter("DISTRITO")
        }
        // La línea de dirección a veces trae al final "LIMA - LIMA - SURCO"
        if (district == null && address != null && address.count { it == '-' || it == '/' } >= 2) {
            val parts = address.split("-", "/").map { it.trim() }
            district = parts.last()
            address = parts.dropLast(1).joinToString(" - ").takeIf { parts.size > 3 } ?: parts.first()
        }

        // ---------- 3. Número suelto ----------
        if (dni == null) {
            dni = n.firstNotNullOfOrNull { line ->
                if (line.contains("DNI") || line.contains("IDENTIDAD")) Regex("(?<!\\d)(\\d{8})(?!\\d)").find(line)?.groupValues?.get(1) else null
            } ?: n.firstNotNullOfOrNull { Regex("^\\D*(\\d{8})(?:-?\\d)?\\D*$").find(it)?.groupValues?.get(1) }
        }

        return DniData(
            dni = dni,
            surnames = title(surnames),
            givenNames = title(given),
            address = title(address),
            district = title(district),
            province = title(province),
            department = title(department),
        )
    }
}
