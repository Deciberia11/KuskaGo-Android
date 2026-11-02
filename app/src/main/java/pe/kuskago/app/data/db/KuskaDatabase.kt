package pe.kuskago.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        DatoUsuarioEntity::class,
        TramiteEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class KuskaDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun sesionDao(): SesionDao
    abstract fun preferenciasDao(): PreferenciasDao
    abstract fun documentoDao(): DocumentoDao
    abstract fun familiarDao(): FamiliarDao
    abstract fun pasoRutaDao(): PasoRutaDao
    abstract fun datoUsuarioDao(): DatoUsuarioDao
    abstract fun tramiteDao(): TramiteDao

    companion object {
        /** Versión 1 → 2: se agrega la tabla dato_usuario sin borrar los datos existentes. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dato_usuario` (" +
                        "`dni` TEXT NOT NULL, `clave` TEXT NOT NULL, `valor` TEXT NOT NULL, `actualizado` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`dni`, `clave`), " +
                        "FOREIGN KEY(`dni`) REFERENCES `usuario`(`dni`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
            }
        }

        /** Versión 2 → 3: se agrega la tabla tramite ("Mis trámites"). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tramite` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dni` TEXT NOT NULL, `tipo` TEXT NOT NULL, " +
                        "`titulo` TEXT NOT NULL, `entidad` TEXT NOT NULL, `estado` TEXT NOT NULL, `progreso` INTEGER NOT NULL, " +
                        "`documento_nombre` TEXT NOT NULL, `creado` INTEGER NOT NULL, `actualizado` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`dni`) REFERENCES `usuario`(`dni`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tramite_dni` ON `tramite` (`dni`)")
            }
        }

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
