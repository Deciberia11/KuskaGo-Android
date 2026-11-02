package pe.kuskago.app.data

import android.content.Context
import pe.kuskago.app.data.db.DatoUsuarioEntity
import pe.kuskago.app.data.db.DocumentoEntity
import pe.kuskago.app.data.db.FamiliarEntity
import pe.kuskago.app.data.db.KuskaDatabase
import pe.kuskago.app.data.db.PasoRutaEntity
import pe.kuskago.app.data.db.PreferenciasEntity
import pe.kuskago.app.data.db.SesionEntity
import pe.kuskago.app.data.db.TramiteEntity
import pe.kuskago.app.data.db.UsuarioEntity
import java.security.MessageDigest

// ---------- Modelos que usan las pantallas ----------

data class UserAccount(
    val name: String,
    val phone: String,
    val passHash: String,
    val email: String,
)

data class CurrentUser(val dni: String, val name: String)

data class AccessPrefs(
    val big: Boolean = false,
    val listen: Boolean = false,
    val guided: Boolean = false,
    val contrast: Boolean = false,
    val simple: Boolean = false,
    val translate: Boolean = false,
    val textSize: Int = 3,
)

data class Profile(val name: String, val dni: String, val phone: String, val place: String)

/** Documento guardado. [content] = texto si se creó en la app; [uri] = archivo elegido del teléfono. */
data class StoredDoc(
    val name: String,
    val meta: String,
    val icon: String,
    val content: String = "",
    val uri: String = "",
)

/** Trámite del usuario para la pantalla "Mis trámites". */
data class MyTramite(
    val id: Long,
    val type: String,
    val title: String,
    val entity: String,
    val status: String,
    val progress: Int,
    val documentName: String,
    val updated: Long,
)

data class FamilyMember(val name: String, val relation: String, val dni: String, val icon: String)

/**
 * Repositorio de KuskaGo (patrón Repository).
 *
 * Es el único punto por el que las pantallas leen y escriben datos. Por dentro usa la
 * base de datos SQLite local [KuskaDatabase] (Room) y convierte las entidades de la base
 * de datos a los modelos simples de arriba.
 *
 * Las contraseñas se guardan como hash SHA-256, nunca en texto plano.
 */
class KuskaStore(context: Context) {

    private val db = KuskaDatabase.get(context)
    private val usuarios = db.usuarioDao()
    private val sesion = db.sesionDao()
    private val preferencias = db.preferenciasDao()
    private val documentos = db.documentoDao()
    private val familiares = db.familiarDao()
    private val pasosRuta = db.pasoRutaDao()
    private val datosUsuario = db.datoUsuarioDao()
    private val tramites = db.tramiteDao()

    // ======================= CUENTAS =======================

    fun getUser(dni: String): UserAccount? =
        usuarios.buscar(dni)
            ?.takeIf { it.passHash.isNotBlank() }   // la cuenta demo no tiene contraseña propia
            ?.let { UserAccount(it.nombre, it.celular, it.passHash, it.correo) }

    fun saveUser(dni: String, name: String, phone: String, password: String, email: String) {
        val existente = usuarios.buscar(dni)
        usuarios.guardar(
            UsuarioEntity(
                dni = dni,
                nombre = name,
                celular = phone,
                passHash = hash(password),
                correo = email,
                distrito = existente?.distrito.orEmpty(),
            )
        )
        if (existente == null) sembrarDatosIniciales(dni)
    }

    fun updatePassword(dni: String, newPassword: String): Boolean =
        usuarios.cambiarPassword(dni, hash(newPassword)) > 0

    fun checkPassword(dni: String, password: String): Boolean {
        val u = usuarios.buscar(dni) ?: return false
        return u.passHash.isNotBlank() && u.passHash == hash(password)
    }

    // ======================= SESIÓN =======================

    private val settings = context.applicationContext.getSharedPreferences("kuskago_settings", Context.MODE_PRIVATE)

    /** "Recordarme" del inicio de sesión: si es true, la app abre directo en Inicio. */
    fun rememberSession(): Boolean = settings.getBoolean("remember_session", false)

    fun setRememberSession(value: Boolean) {
        settings.edit().putBoolean("remember_session", value).apply()
    }

    fun currentUser(): CurrentUser? =
        sesion.actual()?.let { CurrentUser(it.dni, it.nombre) }

    /**
     * Abre (o cierra con null) la sesión. Si el usuario aún no existe en la tabla
     * (caso de la cuenta de prueba 12345678) se crea con sus datos de ejemplo.
     */
    fun setCurrentUser(user: CurrentUser?) {
        if (user == null) {
            sesion.cerrar()
            return
        }
        if (usuarios.buscar(user.dni) == null) {
            usuarios.guardar(UsuarioEntity(dni = user.dni, nombre = user.name, distrito = "Puno"))
            sembrarDatosIniciales(user.dni)
        }
        sesion.abrir(SesionEntity(dni = user.dni, nombre = user.name))
    }

    /** DNI del usuario con sesión, o null si no hay sesión (no se guarda nada en ese caso). */
    private fun dniActual(): String? = currentUser()?.dni?.takeIf { it.isNotBlank() }

    // ======================= PREFERENCIAS =======================

    fun getPrefs(dni: String): AccessPrefs? {
        if (dni.isBlank()) return null
        val p = preferencias.buscar(dni) ?: return null
        return AccessPrefs(
            big = p.textoGrande,
            listen = p.escuchar,
            guided = p.pasoAPaso,
            contrast = p.altoContraste,
            simple = p.lenguajeSencillo,
            translate = p.traducir,
            textSize = if (p.tamanoTexto in 1..5) p.tamanoTexto else 3,
        )
    }

    fun savePrefs(dni: String, p: AccessPrefs) {
        if (dni.isBlank() || usuarios.buscar(dni) == null) return
        preferencias.guardar(
            PreferenciasEntity(
                dni = dni,
                textoGrande = p.big,
                escuchar = p.listen,
                pasoAPaso = p.guided,
                altoContraste = p.contrast,
                lenguajeSencillo = p.simple,
                traducir = p.translate,
                tamanoTexto = p.textSize,
            )
        )
    }

    // ======================= PERFIL =======================

    fun getProfile(): Profile {
        val cur = currentUser()
        val u = cur?.let { usuarios.buscar(it.dni) }
        return Profile(
            name = u?.nombre?.takeIf { it.isNotBlank() } ?: cur?.name?.takeIf { it.isNotBlank() } ?: "Juan Quispe Mamani",
            dni = cur?.dni?.takeIf { it.isNotBlank() } ?: "----",
            phone = u?.celular?.takeIf { it.isNotBlank() } ?: "9xx xxx x21",
            place = u?.distrito?.takeIf { it.isNotBlank() } ?: "Puno",
        )
    }

    fun saveProfile(name: String, phone: String, place: String) {
        val cur = currentUser() ?: return
        usuarios.actualizarPerfil(cur.dni, name, phone, place)
        sesion.abrir(SesionEntity(dni = cur.dni, nombre = name))
    }

    // ======================= DOCUMENTOS =======================

    fun getDocs(): List<StoredDoc> {
        val dni = dniActual() ?: return DEFAULT_DOCS
        return documentos.listar(dni).map { StoredDoc(it.nombre, it.meta, it.icono, it.contenido, it.uri) }
    }

    /** Guarda la lista completa en el orden recibido (el primero se muestra arriba). */
    fun saveDocs(docs: List<StoredDoc>) {
        val dni = dniActual() ?: return
        documentos.reemplazarLista(
            dni,
            docs.mapIndexed { i, d ->
                DocumentoEntity(dni = dni, nombre = d.name, meta = d.meta, icono = d.icon, contenido = d.content, uri = d.uri, orden = i)
            },
        )
    }

    fun addDoc(doc: StoredDoc) = saveDocs(listOf(doc) + getDocs())

    // ======================= FAMILIA =======================

    fun getFamily(): List<FamilyMember> {
        val dni = dniActual() ?: return DEFAULT_FAMILY
        return familiares.listar(dni).map { FamilyMember(it.nombre, it.parentesco, it.dniFamiliar, it.icono) }
    }

    fun saveFamily(list: List<FamilyMember>) {
        val dni = dniActual() ?: return
        familiares.reemplazarLista(
            dni,
            list.mapIndexed { i, m ->
                FamiliarEntity(dniTitular = dni, nombre = m.name, parentesco = m.relation, dniFamiliar = m.dni, icono = m.icon, orden = i)
            },
        )
    }

    // ======================= RUTA CIUDADANA =======================

    fun getRouteDone(): Set<Int> {
        val dni = dniActual() ?: return setOf(1, 2)
        return pasosRuta.completados(dni).toSet()
    }

    fun saveRouteDone(done: Set<Int>) {
        val dni = dniActual() ?: return
        pasosRuta.reemplazar(dni, done.sorted().map { PasoRutaEntity(dni, it) })
    }

    // ======================= MEMORIA DE DATOS PARA TRÁMITES =======================

    /**
     * Todo lo que ya sabemos del usuario: datos de su cuenta (nombre, DNI, celular, distrito)
     * más lo que respondió en trámites anteriores (tabla dato_usuario).
     * Las claves son las de [PERSONAL_LABELS].
     */
    fun getPersonalData(): Map<String, String> {
        val cur = currentUser() ?: return emptyMap()
        val u = usuarios.buscar(cur.dni)
        val base = buildMap {
            put("dni", cur.dni)
            u?.nombre?.takeIf { it.isNotBlank() }?.let { put("nombre", it) }
            u?.celular?.takeIf { it.isNotBlank() }?.let { put("celular", it) }
            u?.distrito?.takeIf { it.isNotBlank() }?.let { put("distrito", it) }
            u?.correo?.takeIf { it.isNotBlank() }?.let { put("correo", it) }
        }
        val extra = datosUsuario.listar(cur.dni).associate { it.clave to it.valor }
        return base + extra.filterValues { it.isNotBlank() }
    }

    /**
     * Guarda las respuestas para no volver a pedirlas.
     * Nombre, celular y distrito actualizan la cuenta; el DNI no se cambia (es el usuario).
     */
    fun savePersonalData(values: Map<String, String>) {
        val cur = currentUser() ?: return
        val clean = values.mapValues { it.value.trim() }.filterValues { it.isNotBlank() }
        if (clean.isEmpty()) return
        val u = usuarios.buscar(cur.dni)
        if (u != null && (clean.containsKey("nombre") || clean.containsKey("celular") || clean.containsKey("distrito"))) {
            usuarios.actualizarPerfil(
                cur.dni,
                clean["nombre"] ?: u.nombre,
                clean["celular"] ?: u.celular,
                clean["distrito"] ?: u.distrito,
            )
            clean["nombre"]?.let { sesion.abrir(SesionEntity(dni = cur.dni, nombre = it)) }
        }
        val otros = clean.filterKeys { it !in ACCOUNT_KEYS }
        if (otros.isNotEmpty()) {
            datosUsuario.guardarTodos(otros.map { (k, v) -> DatoUsuarioEntity(cur.dni, k, v) })
        }
    }

    /** Guarda dirección y distrito de una cuenta recién creada (por ejemplo, leídos del DNI). */
    fun saveAddressFor(dni: String, address: String, district: String) {
        val u = usuarios.buscar(dni) ?: return
        if (district.isNotBlank()) usuarios.actualizarPerfil(dni, u.nombre, u.celular, district.trim())
        if (address.isNotBlank()) datosUsuario.guardarTodos(listOf(DatoUsuarioEntity(dni, "domicilio", address.trim())))
    }

    /** Datos guardados de trámites (para verlos y borrarlos en "Editar datos"). */
    fun getSavedTramiteData(): List<Pair<String, String>> {
        val cur = currentUser() ?: return emptyList()
        return datosUsuario.listar(cur.dni).map { it.clave to it.valor }
    }

    fun deletePersonalData(key: String) {
        val cur = currentUser() ?: return
        datosUsuario.borrar(cur.dni, key)
    }

    // ======================= MIS TRÁMITES =======================

    fun getTramites(): List<MyTramite> {
        val dni = dniActual() ?: return emptyList()
        return tramites.listar(dni).map {
            MyTramite(it.id, it.tipo, it.titulo, it.entidad, it.estado, it.progreso, it.documentoNombre, it.actualizado)
        }
    }

    /** Registra un trámite cuando el usuario genera su documento. */
    fun addTramite(type: String, title: String, entity: String, documentName: String): Long {
        val dni = dniActual() ?: return -1
        return tramites.insertar(
            TramiteEntity(
                dni = dni, tipo = type, titulo = title, entidad = entity,
                estado = STATUS_GENERATED, progreso = 50, documentoNombre = documentName,
            )
        )
    }

    fun setTramiteStatus(id: Long, status: String) {
        val progress = when (status) {
            STATUS_GENERATED -> 50
            STATUS_SUBMITTED -> 80
            STATUS_DONE -> 100
            else -> 60
        }
        tramites.cambiarEstado(id, status, progress, System.currentTimeMillis())
    }

    fun deleteTramite(id: Long) = tramites.borrar(id)

    // ======================= DATOS DE EJEMPLO =======================

    /** Al crear un usuario se cargan los datos de ejemplo del prototipo. */
    private fun sembrarDatosIniciales(dni: String) {
        documentos.reemplazarLista(
            dni,
            DEFAULT_DOCS.mapIndexed { i, d -> DocumentoEntity(dni = dni, nombre = d.name, meta = d.meta, icono = d.icon, orden = i) },
        )
        familiares.reemplazarLista(
            dni,
            DEFAULT_FAMILY.mapIndexed { i, m ->
                FamiliarEntity(dniTitular = dni, nombre = m.name, parentesco = m.relation, dniFamiliar = m.dni, icono = m.icon, orden = i)
            },
        )
        pasosRuta.reemplazar(dni, listOf(PasoRutaEntity(dni, 1), PasoRutaEntity(dni, 2)))
        tramites.insertar(
            TramiteEntity(
                dni = dni, tipo = "terreno", titulo = "Registro de terreno", entidad = "COFOPRI",
                estado = STATUS_IN_PROGRESS, progreso = 60,
            )
        )
    }

    companion object {
        const val STATUS_IN_PROGRESS = "EN_CURSO"
        const val STATUS_GENERATED = "GENERADO"
        const val STATUS_SUBMITTED = "PRESENTADO"
        const val STATUS_DONE = "FINALIZADO"

        /** Datos que viven en la tabla usuario (el resto va a dato_usuario). */
        private val ACCOUNT_KEYS = setOf("nombre", "dni", "celular", "distrito", "correo")

        /** Nombre visible de cada dato que la app recuerda. */
        val PERSONAL_LABELS = mapOf(
            "nombre" to "Nombre completo",
            "dni" to "DNI",
            "celular" to "Celular",
            "correo" to "Correo",
            "distrito" to "Distrito donde vives",
            "domicilio" to "Dirección de tu casa",
            "edad" to "Edad",
            "predio_ubicacion" to "Ubicación del terreno",
            "predio_area" to "Área del terreno",
            "predio_direccion" to "Dirección del predio",
            "predio_distrito" to "Distrito del predio",
            "posesion_tiempo" to "Tiempo de posesión",
            "negocio_nombre" to "Nombre del negocio",
            "negocio_direccion" to "Dirección del local",
            "negocio_giro" to "Giro del negocio",
        )

        /** Cuenta de prueba del prototipo: DNI 12345678 con cualquier contraseña. */
        const val DEMO_DNI = "12345678"
        const val DEMO_NAME = "Juan Quispe"

        val DEFAULT_DOCS = listOf(
            StoredDoc("DNI.pdf", "Subido 12 nov. 2024", "🪪"),
            StoredDoc("Croquis.jpg", "Subido 12 nov. 2024", "🗺️"),
            StoredDoc("Constancia_posesion.pdf", "Subido 2 sep. 2025", "📜"),
        )
        val DEFAULT_FAMILY = listOf(
            FamilyMember("Rosa Quispe M.", "Madre", "4562xxxx", "👵"),
            FamilyMember("Luis Quispe F.", "Hijo", "7781xxxx", "👦"),
        )

        fun hash(text: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
