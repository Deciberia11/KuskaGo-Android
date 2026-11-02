package pe.kuskago.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Modelo de datos de KuskaGo (SQLite mediante Room).
 *
 *   usuario (dni PK)
 *     ├── preferencias   (1 a 1, dni PK/FK)
 *     ├── documento      (1 a N, dni FK)
 *     ├── familiar       (1 a N, dni_titular FK)
 *     ├── paso_ruta      (1 a N, PK compuesta dni + paso)
 *     ├── dato_usuario   (1 a N, PK compuesta dni + clave): datos que el usuario ya respondió
 *     │                  en trámites (dirección del terreno, edad...) para no volver a pedirlos
 *     └── tramite        (1 a N, dni FK): trámites iniciados por el usuario y su estado
 *   sesion (una sola fila: quién tiene la sesión abierta)
 *
 * Todas las FK usan ON DELETE CASCADE: si se borra un usuario, se borran sus datos.
 */

@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey val dni: String,
    val nombre: String,
    val celular: String = "",
    @ColumnInfo(name = "pass_hash") val passHash: String = "",
    val correo: String = "",
    val distrito: String = "",
)

@Entity(tableName = "sesion")
data class SesionEntity(
    @PrimaryKey val id: Int = 1,
    val dni: String,
    val nombre: String,
)

@Entity(
    tableName = "preferencias",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["dni"],
            childColumns = ["dni"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PreferenciasEntity(
    @PrimaryKey val dni: String,
    @ColumnInfo(name = "texto_grande") val textoGrande: Boolean = false,
    val escuchar: Boolean = false,
    @ColumnInfo(name = "paso_a_paso") val pasoAPaso: Boolean = false,
    @ColumnInfo(name = "alto_contraste") val altoContraste: Boolean = false,
    @ColumnInfo(name = "lenguaje_sencillo") val lenguajeSencillo: Boolean = false,
    val traducir: Boolean = false,
    @ColumnInfo(name = "tamano_texto") val tamanoTexto: Int = 3,
)

@Entity(
    tableName = "documento",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["dni"],
            childColumns = ["dni"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dni")],
)
data class DocumentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dni: String,
    val nombre: String,
    val meta: String,
    val icono: String,
    val contenido: String = "",
    val uri: String = "",
    val orden: Int = 0,
)

@Entity(
    tableName = "familiar",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["dni"],
            childColumns = ["dni_titular"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dni_titular")],
)
data class FamiliarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "dni_titular") val dniTitular: String,
    val nombre: String,
    val parentesco: String,
    @ColumnInfo(name = "dni_familiar") val dniFamiliar: String,
    val icono: String,
    val orden: Int = 0,
)

@Entity(
    tableName = "paso_ruta",
    primaryKeys = ["dni", "paso"],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["dni"],
            childColumns = ["dni"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PasoRutaEntity(
    val dni: String,
    val paso: Int,
)

/**
 * Datos personales reutilizables entre trámites (memoria de formularios).
 * Ej.: (12345678, "predio_ubicacion", "Sector Chuquibambilla, Puno").
 */
@Entity(
    tableName = "dato_usuario",
    primaryKeys = ["dni", "clave"],
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["dni"],
            childColumns = ["dni"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DatoUsuarioEntity(
    val dni: String,
    val clave: String,
    val valor: String,
    /** Fecha de la última actualización (milisegundos). */
    val actualizado: Long = System.currentTimeMillis(),
)

/**
 * Trámite iniciado por el usuario (sección "Mis trámites").
 * estado: GENERADO → PRESENTADO → FINALIZADO  (o EN_CURSO para el de ejemplo)
 */
@Entity(
    tableName = "tramite",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["dni"],
            childColumns = ["dni"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dni")],
)
data class TramiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dni: String,
    /** Tipo del formulario: terreno, constancia, dni, pension65, herencia, licencia. */
    val tipo: String,
    val titulo: String,
    val entidad: String,
    val estado: String,
    /** Avance de 0 a 100. */
    val progreso: Int,
    @ColumnInfo(name = "documento_nombre") val documentoNombre: String = "",
    val creado: Long = System.currentTimeMillis(),
    val actualizado: Long = System.currentTimeMillis(),
)
