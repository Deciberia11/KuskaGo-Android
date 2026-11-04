# KuskaGo — App Android (Kotlin + Jetpack Compose)

Versión nativa del prototipo web de KuskaGo. Reproduce las mismas pantallas, colores, textos, validaciones y flujo.

## Cómo abrirlo

1. Descomprime el ZIP.
2. En Android Studio: **File → Open…** y elige la carpeta `KuskaGo`.
3. Espera a que termine la sincronización de Gradle (la primera vez descarga dependencias, necesita internet).
4. Elige un emulador o tu celular y pulsa **Run ▶**.

Requisitos: Android Studio Ladybug o más reciente, JDK 17 (viene con Android Studio). La app funciona desde Android 8.0 (API 26).
Si Android Studio sugiere actualizar el plugin de Android Gradle, puedes aceptar.

## Cuenta de prueba

- **DNI:** `12345678` con cualquier contraseña → entra como *Juan Quispe*.
- También puedes crear tu propia cuenta en "Crear cuenta nueva"; se guarda en el teléfono.

## Pantallas incluidas (las 22 del prototipo de alta fidelidad)

**Ingreso:** Bienvenida · Iniciar sesión · Crear cuenta · Recuperar contraseña · Preferencias (texto grande 1–5, voz, paso a paso, alto contraste, lenguaje sencillo, traductor).

**Inicio:** modo normal (accesos a trámites, documentos, familia, Ruta Ciudadana, KuskaAlert, ayuda y traductor) y modo sencillo.

**Trámites:**
- Buscar trámite → abre el **formulario dinámico** de cada trámite (terreno, constancia, DNI, Pensión 65, herencia, licencia). Al terminar genera el documento, lo guarda en *Mis documentos* y permite **imprimir o guardar como PDF**.
- Detalle de trámite con pestañas (Resumen, Requisitos, Documentos, Plantillas, Ruta, Entidad con enlace al sitio oficial).
- Detalle de requisito.
- Plantillas: llenar → vista previa → guardar / descargar PDF / imprimir / compartir.
- Traductor de burocracia: sube un .txt o pega texto, detecta términos difíciles y los explica (con lectura en voz alta).

**Personal:**
- Mis documentos: subir PDF o foto del teléfono, crear documentos de texto, ver, compartir, reemplazar y eliminar (se guardan en el teléfono).
- Mi familia: agregar familiares (con validación de DNI) y crear trámite familiar de herencia.
- Pedir ayuda: tema → canal → respuesta que lleva a la pantalla adecuada.
- Ruta Ciudadana: pasos que se marcan como completados (se guardan), explicación de cada paso y **mapa real (OpenStreetMap)** con los puntos de atención de tu distrito, tu ubicación, distancia y "Cómo llegar" (abre Google Maps).
- KuskaAlert: avisos **generados desde Mis trámites** (documento listo, recordatorio si no lo presentas en 3 días, esperando respuesta) + filtros Todas / Importantes / Urgentes. El número de la campana en Inicio es real.
- Perfil y Editar datos (nombre, celular y distrito se guardan).
- Flujo completo en modo sencillo con botones grandes y voz.

## Estructura

```
app/src/main/java/pe/kuskago/app/
├── MainActivity.kt            ← navegación entre pantallas
├── data/
│   ├── KuskaStore.kt          ← Repositorio: único acceso a los datos desde las pantallas
│   └── db/
│       ├── Entities.kt        ← tablas SQLite (usuario, sesion, preferencias, documento, familiar, paso_ruta, dato_usuario, tramite)
│       ├── Daos.kt            ← consultas SQL (Room DAOs)
│       └── KuskaDatabase.kt   ← base de datos Room "kuskago.db" (Singleton)
│   └── translator/
│       ├── BureaucracyTranslator.kt ← algoritmo del traductor (sin internet)
│       └── AiClient.kt        ← explicación con IA (Ollama local o Gemini)
│   └── maps/PlaceFinder.kt    ← búsqueda de lugares y direcciones (Nominatim / OpenStreetMap) con caché
│   └── identity/              ← lectura del DNI (ML Kit + DniTextParser) y dirección por GPS
└── ui/
    ├── theme/Theme.kt         ← colores y escalas de texto del prototipo
    ├── components/Components.kt ← botones, tarjetas, campos, franja andina, barra inferior
    ├── components/Extra.kt    ← barra inferior, filas, voz, imprimir/PDF, enlaces
    └── screens/               ← AuthScreens, PreferenceScreens, MainScreens,
                                 TramiteScreens, TranslatorScreen, PersonalScreens, MyTramitesScreen, RouteMap
app/src/main/res/drawable/     ← logo, avatar e ilustración convertidos de los SVG originales
```

## Traductor de burocracia

Tiene dos modos (pestañas en la pantalla):

**1. Sin internet — algoritmo propio** (`data/translator/BureaucracyTranslator.kt`)
1. *Normalización*: minúsculas, sin tildes ni signos.
2. *Detección de términos*: diccionario de ~60 términos de trámites del Perú. Busca coincidencias exactas y
   aproximadas con **distancia de Levenshtein** (tolera errores como "constansia de posecion").
3. *Simplificación por reglas*: ~40 reglas que cambian frases formales por sencillas
   ("sírvase subsanar" → "por favor, corrija"; "bajo apercibimiento de" → "si no lo hace, se va a").
4. *Extracción*: plazos ("10 días hábiles", fechas), documentos mencionados y acciones a realizar.

**2. Con IA** (`data/translator/AiClient.kt`) — explica cualquier texto en formato
*Qué dice / Qué tienes que hacer / Plazo / Palabras difíciles*. Oculta DNI, celulares y correos antes de enviar.

**Ollama (por defecto): IA gratis que corre en tu computadora, sin clave ni registro**
1. Descarga e instala Ollama para Windows: https://ollama.com/download
2. Abre una terminal (cmd) y descarga un modelo (≈2 GB, solo la primera vez):
   ```
   ollama pull llama3.2:3b
   ```
   Si tu PC es lenta usa `llama3.2:1b`; si es potente, `gemma3:4b` o `qwen3:4b` responden mejor en español.
3. **Emulador:** no hace falta nada más; la app usa `http://10.0.2.2:11434` (es el "localhost" de tu PC).
4. **Celular real (misma Wi-Fi que la PC):**
   - Busca la IP de tu PC con `ipconfig` (ej. `192.168.1.50`).
   - Haz que Ollama acepte conexiones de la red: *Configuración de Windows → Variables de entorno* →
     nueva variable de usuario `OLLAMA_HOST` = `0.0.0.0`, y reinicia Ollama.
   - Permite el puerto **11434** en el Firewall de Windows cuando lo pregunte.
   - En la app: Traductor → ✨ Con IA → ⚙️ IA → dirección `http://192.168.1.50:11434` → **Probar conexión**.

**Gemini (alternativa en la nube):** agrega `GEMINI_API_KEY=tu_clave` en `local.properties`
(clave gratis en https://aistudio.google.com), vuelve a compilar y elige "Gemini" en ⚙️ IA.

Nota: la app usa `http://` sin cifrar para hablar con Ollama en tu red local (`usesCleartextTraffic`).
Está bien para el prototipo; en producción la IA iría en un servidor con `https://`.

## Base de datos (SQLite local con Room)

Todos los datos se guardan en el archivo `kuskago.db` dentro del teléfono (no hay servidor).

| Tabla | Clave | Relación |
|---|---|---|
| `usuario` | `dni` (PK) | — |
| `sesion` | `id` = 1 | usuario con la sesión abierta |
| `preferencias` | `dni` (PK, FK → usuario) | 1 a 1 |
| `documento` | `id` (PK), `dni` (FK) | 1 a N |
| `familiar` | `id` (PK), `dni_titular` (FK) | 1 a N |
| `paso_ruta` | (`dni`, `paso`) PK compuesta | 1 a N |
| `dato_usuario` | (`dni`, `clave`) PK compuesta | 1 a N — datos que el usuario ya dio en trámites |
| `tramite` | `id` (PK), `dni` (FK) | 1 a N — "Mis trámites": tipo, entidad, estado y avance |

### Memoria de datos (no volver a pedir lo mismo)
Al iniciar un trámite, el formulario se rellena con lo que la app ya sabe del usuario
(nombre, DNI, celular y distrito de su cuenta + respuestas de trámites anteriores guardadas en `dato_usuario`):
1. Muestra **"Ya tenemos estos datos tuyos"** para confirmarlos.
2. **Solo pregunta lo que falta.**
3. Al terminar, guarda las nuevas respuestas para el próximo trámite
   (por ejemplo, la ubicación del terreno sirve para *Registro de terreno* y *Constancia de posesión*).
El usuario puede ver y borrar estos datos en **Perfil → Editar datos**.
La base de datos está en la **versión 3** con migraciones (`MIGRATION_1_2`, `MIGRATION_2_3`), así que al actualizar la app no se pierden los datos existentes.

### Mis trámites
Cada formulario completado se registra en la tabla `tramite` con su estado:
**Documento listo (50%) → Presentado (80%) → Finalizado (100%)**. La pantalla muestra el avance,
"lo que sigue" en palabras simples, las etapas y acciones (ya lo presenté, ver documento, ver ruta).

### Autocompletar datos con el DNI o la ubicación (gratis)
- **📷 Completar con mi DNI** (Crear cuenta, Editar datos y preguntas de domicilio): se toma una foto del DNI
  (o se elige de la galería) y **ML Kit Text Recognition** lee el texto **en el celular, sin internet**.
  `data/identity/DniTextParser.kt` interpreta el texto: zona MRZ del DNIe (`I<PER12345678`, `APELLIDO<APELLIDO<<NOMBRES`)
  y etiquetas impresas (Primer Apellido, Pre Nombres, Dirección, Departamento/Provincia/Distrito).
  Se pueden escanear las dos caras: la segunda completa lo que faltó. **La foto se borra apenas se lee.**
- **📍 Usar mi ubicación** (dirección de casa, ubicación/dirección/distrito del terreno o del local):
  GPS → dirección con el Geocoder de Android o, si no está disponible, Nominatim (OpenStreetMap).
- Siempre se muestra el resultado en los campos para que la persona **lo revise y corrija** antes de guardar.
- ¿Por qué no se trae la dirección directamente desde RENIEC? Ese dato solo lo entrega el servicio oficial
  (Nivel II, ~S/ 1.20 por consulta, solo para empresas o entidades con convenio). Las APIs gratuitas de DNI solo dan el nombre.

### Mapa (OpenStreetMap, gratis y sin clave)
- Librería **osmdroid** (`org.osmdroid:osmdroid-android`), código en `ui/screens/RouteMap.kt`.
- Los puntos (Municipalidad, Banco de la Nación, COFOPRI, RENIEC) se buscan en el **distrito del perfil**
  con **Nominatim** (`data/maps/PlaceFinder.kt`), respetando su política: 1 consulta/segundo, User-Agent propio
  y caché en el teléfono (cada búsqueda se hace una sola vez).
- "Mi ubicación" pide permiso de ubicación (opcional) y muestra la distancia a cada punto.
- En el emulador, la ubicación se simula en *Extended controls (⋯) → Location*.

### Identidad visual (colores, tipografía e íconos)
Definida en `ui/theme/Theme.kt` (objeto `K` para colores y `KFont` para fuentes).

Paleta "viva": los colores originales del prototipo (verde andino, menta, mostaza, terracota, celeste, crema),
más saturados y luminosos, con todo texto a 4.5:1 o más (WCAG AA) para el público adulto.

| Color | HEX | Uso | Contraste |
|---|---|---|---|
| Verde KuskaGo | `#0B7A55` | principal: botones, enlaces, selección | con blanco 5.3:1 (AA) |
| Menta | `#DFF5EA` | fondos suaves; encima verde `#075C40` | 7.0:1 (AAA) |
| Oro Inti | `#F5B700` | insignias y avisos, siempre con texto oscuro | con tinta 7.8:1 (AAA) |
| Terracota | `#C2410C` | acento andino, "Pedir ayuda" | con blanco 5.2:1 (AA) |
| Celeste | `#1565C0` | información, "Mis trámites" | con blanco 5.8:1 (AA) |
| Rojo alerta | `#C62828` | urgente y errores | con blanco 5.6:1 (AA) |
| Morado | `#6F35B5` | familia | con blanco 7.4:1 (AAA) |
| Crema | `#FFFAF0` | fondo de pantallas | — |
| Tinta | `#14302A` | texto | sobre crema 13.6:1 (AAA) |

- **Tipografías** (Google Fonts, licencia OFL, guardadas en `res/font` para funcionar sin internet):
  **Open Sans** (texto, formularios y títulos en negrita) y **Montserrat** (botones).
- **Íconos:** Material Icons de Google (estilo *Rounded*, el estándar de Android) en lugar de emojis,
  definidos en `ui/components/KIcons.kt`. Barra inferior al estilo Material 3: ícono relleno en la sección activa
  y de contorno en las demás. Siempre van acompañados de texto, y el color nunca es la única señal.

### Accesibilidad en toda la app
- **Texto grande** (niveles 1–5 en Preferencias/Accesibilidad) se aplica a **todas** las pantallas.
- **Recordarme** en el inicio de sesión: la app abre directo en Inicio (o en Modo sencillo si se eligió "paso a paso").

Las claves foráneas usan `ON DELETE CASCADE`. Para **ver las tablas con la app abierta**:
Android Studio → **View → Tool Windows → App Inspection → Database Inspector** → `kuskago.db`.
Ahí puedes ejecutar SQL, por ejemplo `SELECT * FROM documento;`.

Nota: por ser un prototipo, las consultas corren en el hilo principal (`allowMainThreadQueries`).
En producción se usarían funciones `suspend` con corrutinas.

## Diferencias con la versión web

- Las contraseñas se guardan cifradas con hash SHA-256 (la versión web las guardaba en texto plano).
- "Recuperar contraseña" sí actualiza la contraseña si la cuenta existe y ambas contraseñas coinciden.
- "Escuchar explicación" usa el motor de Texto a Voz del teléfono en español.


## Guía de primer uso

La guía se presenta automáticamente una sola vez por cuenta, al llegar a Inicio
tras el primer acceso, y corresponde únicamente al modo elegido (Normal o Sencillo).
Incluye controles resaltados, Anterior, Siguiente y Entendido. No se cierra ni avanza
por tiempo. Si se interrumpe antes de terminar, conserva el avance local.

Al completarla, no aparece automáticamente en posteriores inicios de sesión ni al
cambiar de modo. Las finalizaciones de versiones anteriores también se respetan.
Perfil > Volver a ver el tutorial inicia de nuevo la guía del modo seleccionado cada
vez que se pulsa, sin reiniciar la condición de primer uso. Borrar los datos de la
aplicación elimina este historial local.

El modo se elige en Preferencias o Accesibilidad. Volver al modo normal guarda esa
elección; Configurar después usa el modo normal. La bienvenida muestra el monograma
K ampliado en lugar de la ilustración del personaje.

Los iconos utilizan claves descriptivas y Material Icons, con compatibilidad para
los documentos guardados en versiones anteriores.

Otras mejoras: etiquetas en Avisos y Perfil, estado seleccionado accesible en la
navegación, limpieza de búsquedas y restricciones para datos incompletos.
La recuperación por SMS/correo sigue siendo una simulación del prototipo.
