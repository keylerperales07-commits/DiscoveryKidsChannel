<p align="center">
  <img src="https://raw.githubusercontent.com/keylerperales07-commits/DiscoveryKidsChannel/main/icon.png" width="140" alt="Discovery Kids Channel Logo"/>
</p>

<h1 align="center">Discovery Kids</h1>

<p align="center">
  Simulador de canal de TV retro para Android — desarrollado en Kotlin.<br/>
  Programación lineal, bumpers, comerciales, transiciones y una experiencia CRT completamente inmersiva.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Build-Gradle-02303A?style=flat-square&logo=gradle&logoColor=white"/>
  <img src="https://img.shields.io/badge/Status-Active-brightgreen?style=flat-square"/>
  <img src="https://img.shields.io/badge/Última_versión-v5.3.0-brightgreen?style=flat-square"/>
  <img src="https://img.shields.io/badge/Era-2010-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/Fase-4-orange?style=flat-square"/>
</p>

---

## 📖 Descripción General

**Discovery Kids** es una aplicación Android que recrea fielmente la experiencia de ver un canal infantil de televisión clásico. Cuenta con reproducción lineal continua con elementos auténticos de transmisión — bumpers, comerciales, un overlay "a continuación" (*nextprogram*) sobre el final de cada programa, música de fondo y un overlay estilo CRT en pantalla completa — todo diseñado para hacerte sentir que estás viendo televisión real otra vez.

El proyecto está organizado en tres etapas evolutivas que reflejan la historia del canal:

| Etapa | Carpeta | Descripción |
|-------|---------|-------------|
| 🧪 Alpha | `/alpha` | Compilaciones prototipo iniciales |
| 🔧 Beta | `/beta` | Compilaciones pre-release con funcionalidades completas |
| 🚀 Release | `/release` | Compilaciones estables de producción |

---

## ✨ Funcionalidades

- 📺 **Reproducción Lineal Continua** — Los programas se reproducen automáticamente en secuencia, igual que un canal de TV real
- 🎬 **Bumpers** — Clips de identidad del canal entre programas, ahora en resolución mejorada
- 📣 **Comerciales Dinámicos** — Bloques de publicidad que rotan con intervalos aleatorios entre 3 y 9 minutos, incluyendo contenido de la era Y2K. Desde la Release 5.4.0 aparecen ÚNICAMENTE interrumpiendo Programas (se eliminó el StandaloneCommercial, el bloque de comercial suelto entre Bumper y Programa)
- 🎬 **Intro y Créditos personalizados** *(Release 5.4.0)* — Opcionales, por programa, configurables en Discovery Kids Launcher → Configuración de Programa. La Intro aparece después del Bumper y antes del Programa; los Créditos, al terminar el Programa. Ninguno tiene un video predeterminado — si se activan sin elegir un archivo, el Launcher bloquea "Iniciar canal" y avisa qué falta
- ⏭️ **NextProgram** — Marco decorativo animado (GIF, uno por programa) con el video del programa EN CURSO mostrado en vivo dentro de su recuadro (sin estirar ni deformar — Release 5.4.1), que anticipa qué sigue en el canal 31 segundos antes del final REAL del bloque (Créditos si están activos, si no el Programa). Reemplaza a los "enseguidas" post-programa (clip aparte, eliminado)
- 🎵 **Música de Fondo** — Música ambiente durante la reproducción de programas (volumen al 8%)
- 🖥️ **Modo Pantalla Completa Inmersivo** — Sin distracciones de interfaz, experiencia TV pura
- 📡 **Overlay Visual CRT** — Efectos de scanlines y pantalla para esa sensación retro de televisor
- 💾 **Reanudación de Sesión** — La app recuerda dónde quedaste al volver desde el fondo
- 🆕 **ScreenBug de 3 fases** — Marca de agua animada con el logo del canal: aparición (GIF), estático (PNG) y salida (GIF, a partir de 46 segundos antes del final del bloque). Desde la Release 5.4.0, si el programa tiene Intro/Créditos activados, la aparición ocurre en la Intro y la salida en los Créditos — la cuenta de tiempo no se reinicia al cambiar de clip, "suma" la duración real de cada uno. Los GIF se reproducen con `GifMovieDrawable`, basado en la API nativa `android.graphics.Movie` (sin librerías externas — Release 2009.5.1.0). Nuevo contenido de Mayo–Julio 2009 (Release 2009.5.1.0), Julio 2009–2011 (Release 2009.5.2.0)
- 🎄 **ScreenBug de Navidad** — Del 1 al 24 de diciembre (inclusive), la app usa automáticamente un set de ScreenBug con temática navideña, con el mismo comportamiento de 3 fases que el normal (Release 2010.5.3.0)
- ⏸ **Pantalla "Ya Volvemos"** — Pantalla intersticial auténtica de "Volvemos en un momento"
- 🎞️ **Transiciones Profesionales FadeIn / FadeOut** — Cada cambio de video aplica un **FadeOut de 500 ms** y un **FadeIn de 1 segundo**, cubriendo bumpers, comerciales, transiciones ya_regresa/continuamos y arranque/retoma de programas
- ⏭️ **Navegación Prev / Next por bloque completo** — Los botones de canal navegan al bloque completo del programa (Bumper → [Intro] → Programa), igual que cambiar de canal en TV real
- ⚙️ **Pantalla de Configuración** — Accesible desde el botón de ajustes, con lista simple estilo Android Settings. Permite alternar música de fondo, efecto CRT y Forzar 4:3, y ajustar la duración del Screenbug y el intervalo entre comerciales — cada opción muestra su valor predeterminado
- 🔄 **Actualizador integrado** — Desde Configuración, "Buscar actualizaciones" consulta el último release de GitHub; si hay una versión más nueva, descarga el `.apk` con OkHttp (mostrando progreso y tamaño en vivo) y abre el instalador del sistema. Un switch "Habilitar versiones Preview" (desactivado por defecto) permite que también instale releases Preview, no solo estables. La pantalla del Actualizador calca el diseño nativo de "Actualización del sistema" de Android
- 🔔 **Aviso de actualización al abrir la app** — Además de "Buscar actualizaciones" en Configuración, la app consulta en silencio al entrar y muestra un AlertDialog propio si hay una versión nueva (Release 2009.5.0.0)
- 🧪 **Discovery Kids Launcher (Experimental)** — Con "Habilitar funciones experimentales" activado en Configuración, la app abre en una pantalla nueva, rediseñada a **Material Design 3 puro** (esquema de color claro/oscuro automático, ActionBar original de Android — Release 2009.5.1.0) desde donde elegís el video de cada programa con el selector de archivos del sistema (sin renombrar ni copiar nada a Videos), cuántos programas querés (hasta 24) y, si querés, un ya_regresa/continuamos propio para cada uno. Desactivado, la app sigue abriendo directo al canal con los 4 programas clásicos (Release 2009.5.0.0)
- 📺 **Contenedor de video en 4:3, siempre** — El contenedor donde vive el video (junto con el ScreenBug y el efecto CRT) está siempre en proporción 4:3, sin excepción (Release 2009.5.2.1). "Forzar 4:3" (Configuración) decide qué pasa con el video *dentro* de esa caja: activado, se estira para llenarla exacto; desactivado, se ajusta preservando su proporción real sin estirarse — un video 16:9, por ejemplo, encaja con franjas arriba/abajo en vez de deformarse.

---

## 🎮 Cómo Usar

¿Querés experimentar tu propio canal de Discovery Kids? Solo necesitás **4 videos** de tu elección. Así se hace:

**1. Descargá 4 videos a tu gusto**

Pueden ser episodios de tu serie favorita, películas cortas, o cualquier contenido que quieras ver como si fuera un canal de TV. El formato recomendado es `.mp4`.

> 🧪 **Alternativa (Experimental):** si activás "Habilitar funciones experimentales" en Configuración, la app abre en el Discovery Kids Launcher, donde podés elegir el video de cada programa con el selector de archivos del sistema — sin necesidad de renombrarlo ni copiarlo a Videos — y elegir cuántos programas querés (hasta 24). Si preferís el método clásico de abajo, dejá Experimental desactivado.
**2. Renombrá los archivos exactamente así**

```
pro1.mp4
pro2.mp4
pro3.mp4
pro4.mp4
```

> ⚠️ Los nombres deben ser exactamente esos — en minúsculas y sin espacios. La app los busca por ese nombre específico.

**3. Copiá los archivos a la carpeta de Videos del dispositivo**

Mové los 4 archivos a la carpeta **Movies** (Películas) del almacenamiento interno de tu Android.

**4. Abrí la app y disfrutá**

La app detecta automáticamente los videos, los intercala con bumpers, comerciales, el overlay nextprogram y música de fondo, y te da una experiencia completa de canal de televisión retro. ¡Listo!

> 💡 Si algún video no existe, la app lo omite automáticamente y continúa con el siguiente.

---

## 🗂️ Estructura del Proyecto

```
DiscoveryKidsChannel/
├── .androidide/editor/         # Configuración del editor de AndroidIDE
├── alpha/                      # Código fuente y assets de la etapa Alpha
├── beta/                       # Código fuente y assets de la etapa Beta
├── release/                    # Compilaciones listas para producción
├── gradle/wrapper/             # Archivos del wrapper de Gradle
├── build.gradle                # Configuración de compilación del proyecto
├── settings.gradle             # Configuración de módulos
├── gradle.properties           # Propiedades de Gradle
├── gradlew / gradlew.bat       # Scripts del wrapper de Gradle
├── icon.png                    # Ícono de la aplicación
├── Programas en Discovery Kids.txt   # Catálogo de programas
└── README.md                   # Este archivo
```

---

## 🛠️ Tecnologías Utilizadas

| Tecnología | Propósito |
|------------|-----------|
| **Kotlin** | Lenguaje principal |
| **Android SDK** | Plataforma base |
| **VideoView** | Motor de reproducción de video |
| **MediaPlayer** | Gestión de audio y medios |
| **Storage Access Framework** | Selección de videos propios por programa (Discovery Kids Launcher) |
| **SharedPreferences** | Persistencia de sesión y configuración |
| **Handler / Looper** | Planificación y temporización |
| **Choreographer** | Renderizado frame a frame y guardado de posición en tiempo real |
| **AndroidX** | Compatibilidad moderna con Android |

---

## ⚙️ Cómo Empezar

### Requisitos Previos

Antes de comenzar, asegurate de tener instalado lo siguiente:

- [AndroidIDE](https://androidide.com/) o Android Studio
- Java 11+ / Kotlin runtime
- Dispositivo Android o emulador (API 21+)
- Permisos de almacenamiento habilitados en el dispositivo

### Instalación

**1. Clonar el repositorio**

```bash
git clone https://github.com/keylerperales07-commits/DiscoveryKidsChannel.git
```

**2. Abrir en AndroidIDE**

- Lanzar **AndroidIDE**
- Seleccionar **Importar Proyecto**
- Navegar hasta la carpeta clonada y confirmar
- Esperar a que Gradle sincronice (la primera sincronización puede tardar varios minutos)

**3. Ejecutar la app**

- Presionar el botón **▶ Ejecutar**
- Seleccionar el dispositivo o emulador de destino
- La app se compilará e instalará automáticamente

> **Consejo:** Si encontrás errores de sincronización de Gradle, probá con **Archivo → Sincronizar Proyecto con Archivos Gradle** o invalidá los cachés y reiniciá.

---

## 📋 Registro de Cambios

Consultá [`CHANGELOG.md`](./CHANGELOG.md) para el historial completo de versiones y cambios.

### 🚀 Última versión estable — `v5.4.1` *(Bug Fix · Era Doki 1.0 · Era 2010 · "Parque Imaginario")*
> *El ScreenBug de inicio/final directamente no aparecía en Intro/Créditos (bug real de cálculo, no de paciencia) — corregido. Y el recuadro de NextProgram: lo que va adentro es el video del programa en curso, sin estirar, no otro gráfico — corregido también.*

- 🐛 **ScreenBug ausente en Intro/Créditos** — los tiempos normales (20s / 46s) se aplicaban tal cual sobre clips que suelen ser más cortos que eso; el cálculo daba negativo y no se agendaba nada. Ahora se ajustan automáticamente a la duración real de cada clip.
- 🎯 **Recuadro de NextProgram corregido** — ahora muestra el video del programa en curso (achicado sin estirar, dentro del recuadro), en vez de reposicionar el GIF ahí.

<details>
<summary>📜 Versiones estables anteriores</summary>

**`v5.4.0`** *(Release · Era Doki 1.0 · Era 2010 · "Parque Imaginario")* — NextProgram reposicionado en pantalla y ScreenBug/NextProgram atados a Intro/Créditos (personalizados por programa, opcionales) en vez de siempre al Programa. Se eliminó StandaloneCommercial: los comerciales solo interrumpen Programas. 2 bug fixes de arranque: ANR al abrir la app, y la cantidad de programas quedándose vieja si el Launcher seguía en segundo plano.
- 🐛 ANR "Discovery Kids no responde" al abrir la app — la precarga de los GIFs de ScreenBug/NextProgram decodificaba hasta 8 GIFs de forma sincrónica en el hilo principal dentro de `onCreate()`; ahora corre en un hilo aparte.
- 🐛 Cantidad de programas desactualizada — si `LiveDiscoveryKids` seguía vivo en segundo plano y la cantidad de programas cambiaba mientras tanto, la lista de reproducción quedaba con el valor viejo; `onResume()` ahora la reconstruye.
- ⏭️ StandaloneCommercial eliminado — los comerciales ahora solo interrumpen Programas en curso.
- 🎬 Intro y Créditos personalizados por programa — opcionales, sin video predeterminado, configurables en Discovery Kids Launcher → Configuración de Programa.
- ✅ Validación antes de iniciar el canal — el Launcher bloquea "Iniciar canal" si algo activado no tiene un video elegido.

**`v5.3.0`** *(Release · Era Doki 1.0 · Era 2010 · "Parque Imaginario")* — Cambio de Era (2009→2010). BUG FIX definitivo del fadeOut/fadeIn de programas — causa raíz real esta vez: el fix anterior no contemplaba una reanudación (tras un corte comercial, volver de Configuración, etc.). Nuevo ScreenBug de Navidad (1–24 de diciembre), con el mismo comportamiento de 3 fases que el normal.
- 🐛 FadeOut/FadeIn de programas — bug definitivo. El fix anterior calculaba el momento del fadeOut como si el video siempre arrancara desde el segundo 0, sin restar el punto real de reanudación. Corregido con el mismo cálculo que ya usaba correctamente el resto de la app.
- 🎄 Nuevo ScreenBug de Navidad — del 1 al 24 de diciembre, elegido automáticamente por fecha.

**`v5.2.1`** *(Bug Fix · Era Doki 1.0 · Era 2009 · "Parque Imaginario")* — TextureView eliminado por completo (motor de video, switch "Recortar 4:3" y AlertDialog de 720p+), 2 bugs de arrastre con causa raíz encontrada — el contenedor de video ya no cambia de forma con "Forzar 4:3" (siempre 4:3), y el programa ahora sí hace fadeOut al terminar — y rediseño: la ActionBar de Configuración deja de ser un header hecho a mano, y el logo del Launcher pasa de la ActionBar al cuerpo de la pantalla.
- 🗑️ **TextureView eliminado por completo** — motor de video, switch "Recortar 4:3" y el AlertDialog de 720p+ que lo recomendaba. `DkVideoView` vuelve a ser una sola clase, envoltorio de `VideoView` clásico.
- 🐛 **"Forzar 4:3" — causa raíz real.** El contenedor de video **siempre** está en 4:3, sin excepción; "Forzar 4:3" decide si el video se estira para llenarlo exacto o se ajusta preservando su proporción real dentro de esa misma caja.

**`v5.2.0`** *(Era Doki 1.0 · Era 2009 · "Parque Imaginario")* — Release de investigación a fondo: 2 bugs de arrastre finalmente resueltos con causa raíz identificada — el ScreenBug "reiniciándose" al reanudar y el video estirado a 16:9 con "Forzar 4:3" desactivado. El ítem de Configuración del Launcher pasa al menú de overflow original de Android, y "Usar TextureView" se renombra a "Recortar 4:3" con deshabilitado condicional. Nuevo Screenbug de Julio 2009 – 2011.
- 🐛 **ScreenBug: causa raíz real encontrada.** Cada resume (abrir Configuración y volver, o un ratito en segundo plano) ocultaba el ScreenBug de golpe y no lo restauraba hasta el próximo evento programado — ahora se restaura de inmediato si corresponde, sin reiniciar la animación.

**`v5.1.0`** *(Era Doki 1.0 · Era 2009 · "Parque Imaginario")* — Discovery Kids Launcher rediseñado a Material Design 3 puro (esquema claro/oscuro automático, ActionBar original de Android en vez del MenuBar hecho a mano). 3 bug fixes de la 2009.5.0.0: "Forzar 4:3" no respetaba el switch, recorte de video con TextureView, y el ScreenBug reiniciándose al cambiar de Activity o volver de segundo plano. Ajustes de timing del ScreenBug de 3 fases y reproducción de GIF migrada a una solución 100% nativa (sin librerías externas). Nuevo Screenbug de Mayo–Julio 2009.
- 🎨 **Discovery Kids Launcher a Material Design 3** — ActionBar original de Android (no más MenuBar custom), esquema de color azul con versión clara y oscura automática (DayNight), componentes Material 3 (`MaterialButton`, `MaterialCardView`).

**`v5.0.0`** *(Era Doki 1.0 · Era 2009 · "Parque Imaginario")* — Primera versión de la rama 5.x — arranca la Fase 4. Discovery Kids Launcher pasa a ser la pantalla de inicio (Experimental), con selector de video por programa, hasta 24 programas y ya_regresa/continuamos personalizados.
- 🧪 **Discovery Kids Launcher (Experimental)** — nueva pantalla de inicio real de la app, detrás de "Habilitar funciones experimentales" en Configuración (desactivado por defecto). Elegí el video de cada programa sin renombrar ni copiar nada, cuántos programas querés (1–24) y personalizá el ya_regresa/continuamos de cada uno.
- ⚙️ Se eliminó de Configuración el ítem "Elegir programas" (ahora vive en Discovery Kids Launcher).

**`v4.6.1`** *(Era Doki 1.0 · Era 2009)* — Cambio de Era (2008→2009), 3 Screenbug secuenciales, y 2 bug fixes críticos.
- 🎬 3 Screenbug secuenciales — animación de entrada (start.gif), imagen estática (screenbug.png), animación de salida (end.gif) en momentos específicos del programa.
- 🐛 AppUpdater arreglado — ya no cree que hay versión más nueva cuando actualizas de una preview a la release final de la misma versión.
- 🐛 Prev/Next arreglado — ahora navega en orden real a través del playlist, no saltando entre programas.

</details>

</details>


---

## ⚠️ Notas Importantes

- **Intro y Créditos no tienen video predeterminado.** Si los activás en Configuración de Programa sin elegir un archivo, el Launcher bloquea "Iniciar canal" y te avisa cuáles faltan — no se saltean en silencio.
- La primera sincronización de Gradle puede tardar varios minutos según la velocidad de conexión.
- Gradle descargará todas las dependencias necesarias automáticamente.
- Otorgá los permisos de almacenamiento si el sistema lo solicita.
- Volvé a sincronizar el proyecto si aparecen errores de compilación al abrirlo.

---

## 👤 Autor

**Keyler David Perales García**
[@keylerperales07-commits](https://github.com/keylerperales07-commits)

---

## 📄 Licencia

Este proyecto no especifica una licencia por el momento. Todos los derechos están reservados por el autor salvo que se indique lo contrario. Si deseás usar o contribuir a este proyecto, por favor contactá directamente al autor.

---

<p align="center">
  Hecho con ❤️ y nostalgia &nbsp;·&nbsp;
  <a href="https://github.com/keylerperales07-commits/DiscoveryKidsChannel">Ver en GitHub</a>
</p>
