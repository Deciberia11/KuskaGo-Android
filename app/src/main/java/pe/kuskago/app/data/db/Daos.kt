package pe.kuskago.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario WHERE dni = :dni LIMIT 1")
    fun buscar(dni: String): UsuarioEntity?

    /** Inserta o actualiza sin borrar la fila (así no se disparan los CASCADE). */
    @Upsert
    fun guardar(usuario: UsuarioEntity)

    @Query("UPDATE usuario SET pass_hash = :hash WHERE dni = :dni")
    fun cambiarPassword(dni: String, hash: String): Int

    @Query("UPDATE usuario SET nombre = :nombre, celular = :celular, distrito = :distrito WHERE dni = :dni")
    fun actualizarPerfil(dni: String, nombre: String, celular: String, distrito: String)
}

@Dao
interface SesionDao {
    @Query("SELECT * FROM sesion WHERE id = 1 LIMIT 1")
    fun actual(): SesionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun abrir(sesion: SesionEntity)

    @Query("DELETE FROM sesion")
    fun cerrar()
}

@Dao
interface PreferenciasDao {
    @Query("SELECT * FROM preferencias WHERE dni = :dni LIMIT 1")
    fun buscar(dni: String): PreferenciasEntity?

    @Upsert
    fun guardar(prefs: PreferenciasEntity)
}

@Dao
abstract class DocumentoDao {
    @Query("SELECT * FROM documento WHERE dni = :dni ORDER BY orden ASC, id DESC")
    abstract fun listar(dni: String): List<DocumentoEntity>

    @Insert
    abstract fun insertarTodos(docs: List<DocumentoEntity>)

    @Query("DELETE FROM documento WHERE dni = :dni")
    abstract fun borrarDeUsuario(dni: String)

    /** Reemplaza la lista completa de documentos del usuario en una sola transacción. */
    @Transaction
    open fun reemplazarLista(dni: String, docs: List<DocumentoEntity>) {
        borrarDeUsuario(dni)
        insertarTodos(docs)
    }
}

@Dao
abstract class FamiliarDao {
    @Query("SELECT * FROM familiar WHERE dni_titular = :dni ORDER BY orden ASC, id ASC")
    abstract fun listar(dni: String): List<FamiliarEntity>

    @Insert
    abstract fun insertarTodos(lista: List<FamiliarEntity>)

    @Query("DELETE FROM familiar WHERE dni_titular = :dni")
    abstract fun borrarDeUsuario(dni: String)

    @Transaction
    open fun reemplazarLista(dni: String, lista: List<FamiliarEntity>) {
        borrarDeUsuario(dni)
        insertarTodos(lista)
    }
}

@Dao
abstract class PasoRutaDao {
    @Query("SELECT paso FROM paso_ruta WHERE dni = :dni")
    abstract fun completados(dni: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertarTodos(pasos: List<PasoRutaEntity>)

    @Query("DELETE FROM paso_ruta WHERE dni = :dni")
    abstract fun borrarDeUsuario(dni: String)

    @Transaction
    open fun reemplazar(dni: String, pasos: List<PasoRutaEntity>) {
        borrarDeUsuario(dni)
        insertarTodos(pasos)
    }
}

@Dao
interface DatoUsuarioDao {
    @Query("SELECT * FROM dato_usuario WHERE dni = :dni ORDER BY clave ASC")
    fun listar(dni: String): List<DatoUsuarioEntity>

    @Upsert
    fun guardarTodos(datos: List<DatoUsuarioEntity>)

    @Query("DELETE FROM dato_usuario WHERE dni = :dni AND clave = :clave")
    fun borrar(dni: String, clave: String)
}

@Dao
interface TramiteDao {
    @Query("SELECT * FROM tramite WHERE dni = :dni ORDER BY actualizado DESC")
    fun listar(dni: String): List<TramiteEntity>

    @Insert
    fun insertar(t: TramiteEntity): Long

    @Query("UPDATE tramite SET estado = :estado, progreso = :progreso, actualizado = :ahora WHERE id = :id")
    fun cambiarEstado(id: Long, estado: String, progreso: Int, ahora: Long)

    @Query("DELETE FROM tramite WHERE id = :id")
    fun borrar(id: Long)
}
