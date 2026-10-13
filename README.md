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
- Ruta Ciudadana: pasos que se marcan como completados (se guardan), explicación de cada paso y puntos de atención con "Cómo llegar" en Google Maps.
- KuskaAlert: filtros Todas / Importantes / Urgentes, detalle y recordatorios.
- Perfil y Editar datos (nombre, celular y distrito se guardan).
- Flujo completo en modo sencillo con botones grandes y voz.

## Estructura

```
app/src/main/java/pe/kuskago/app/
├── MainActivity.kt            ← navegación entre pantallas
├── data/
│   ├── KuskaStore.kt          ← Repositorio: único acceso a los datos desde las pantallas
│   └── db/
│       ├── Entities.kt        ← tablas SQLite (usuario, sesion, preferencias, documento, familiar, paso_ruta)
│       ├── Daos.kt            ← consultas SQL (Room DAOs)
│       └── KuskaDatabase.kt   ← base de datos Room "kuskago.db" (Singleton)
│   └── translator/
│       ├── BureaucracyTranslator.kt ← algoritmo del traductor (sin internet)
│       └── AiClient.kt        ← explicación con IA (Ollama local o Gemini)
└── ui/
    ├── theme/Theme.kt         ← colores y escalas de texto del prototipo
    ├── components/Components.kt ← botones, tarjetas, campos, franja andina, barra inferior
    ├── components/Extra.kt    ← barra inferior, filas, voz, imprimir/PDF, enlaces
    └── screens/               ← AuthScreens, PreferenceScreens, MainScreens,
                                 TramiteScreens, TranslatorScreen, PersonalScreens
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

Las claves foráneas usan `ON DELETE CASCADE`. Para **ver las tablas con la app abierta**:
Android Studio → **View → Tool Windows → App Inspection → Database Inspector** → `kuskago.db`.
Ahí puedes ejecutar SQL, por ejemplo `SELECT * FROM documento;`.

Nota: por ser un prototipo, las consultas corren en el hilo principal (`allowMainThreadQueries`).
En producción se usarían funciones `suspend` con corrutinas.

## Diferencias con la versión web

- Las contraseñas se guardan cifradas con hash SHA-256 (la versión web las guardaba en texto plano).
- "Recuperar contraseña" sí actualiza la contraseña si la cuenta existe y ambas contraseñas coinciden.
- "Escuchar explicación" usa el motor de Texto a Voz del teléfono en español.
