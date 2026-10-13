package pe.kuskago.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos SQLite local de KuskaGo (archivo "kuskago.db" dentro del teléfono).
 * Patrón Singleton: una sola instancia para toda la app.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        SesionEntity::class,
        PreferenciasEntity::class,
        DocumentoEntity::class,
        FamiliarEntity::class,
        PasoRutaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class KuskaDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun sesionDao(): SesionDao
    abstract fun preferenciasDao(): PreferenciasDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun familiarDao(): FamiliarDao
    abstract fun pasoRutaDao(): PasoRutaDao

    companion object {
        @Volatile
        private var INSTANCE: KuskaDatabase? = null

        fun get(context: Context): KuskaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KuskaDatabase::class.java,
                    "kuskago.db",
                )
                    // Prototipo: los datos son pocos y las consultas muy rápidas, así que
                    // se permiten en el hilo principal para no reescribir todas las pantallas.
                    // En producción se usarían funciones suspend + corrutinas.
                    .allowMainThreadQueries()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
