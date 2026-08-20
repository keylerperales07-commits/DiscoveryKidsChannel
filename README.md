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
  <img src="https://img.shields.io/badge/Última_versión-v5.8.0-brightgreen?style=flat-square"/>
  <img src="https://img.shields.io/badge/Preview-v6.0.0.0.2-yellow?style=flat-square"/>
  <img src="https://img.shields.io/badge/Era-2012-blue?style=flat-square"/>
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
- 🎄 **ScreenBugs de eventos** — Navidad (1-24 dic, 3 fases completas, Release 2010.5.3.0) y Halloween (31 de octubre, reemplaza solo el logo estático del medio — RELEASE 2014.6.1.0). Desde Configuración: switch maestro "Activar eventos" + selector para forzar manualmente un evento puntual o dejar que la app decida sola por fecha ("Normal") — Release 5.8.0
- 📣 **Continuamos con horario** *(Preview 2013.6.0.0.2)* — El clip de vuelta del corte comercial ("continuamos") depende de la hora real del dispositivo: mañanera entre las 00:00 y las 11:59, tardía entre las 12:00 y las 23:59. Personalizable por programa desde Configuración de Programa, igual que antes
- 🎞️ **FadeOut / FadeIn en el límite Programa↔comercial** *(acotado en la Preview 2013.6.0.0.2)* — El Programa se apaga con **FadeOut de 500 ms** justo antes de cortar a un bloque comercial, y se enciende con **FadeIn de 1 segundo** al retomarse justo después. Todo lo demás — Bumpers, Intro, Créditos, y los clips dentro del bloque comercial (comercial y continuamos) — corta en seco
- 🖼️ **GIF "próximo programa"** *(Preview 2013.6.0.0.2)* — `proximo_programa_screenbug.gif` sustituye brevemente al ScreenBug estático una vez por segmento, a mitad del programa: nunca dentro del primer minuto de arrancado ni del último minuto antes de un comercial o del final del programa/episodio. Aparece y desaparece sin animación (corte seco), visible 15 segundos
- ⏭️ **Navegación Prev / Next por bloque completo** — Los botones de canal navegan al bloque completo del programa (Bumper → [Intro] → Programa), igual que cambiar de canal en TV real
- ⚙️ **Pantalla de Configuración** — Accesible desde el botón de ajustes, con lista simple estilo Android Settings. Permite alternar música de fondo, efecto CRT y Forzar 4:3, y ajustar la duración del Screenbug y el intervalo entre comerciales — cada opción muestra su valor predeterminado
- 🔄 **Actualizador integrado** — Desde Configuración, "Buscar actualizaciones" consulta el último release de GitHub; si hay una versión más nueva, descarga el `.apk` con OkHttp (mostrando progreso y tamaño en vivo) y abre el instalador del sistema. Un switch "Habilitar versiones Preview" (desactivado por defecto) permite que también instale releases Preview, no solo estables. La pantalla del Actualizador calca el diseño nativo de "Actualización del sistema" de Android
- 🔔 **Aviso de actualización al abrir la app** — Además de "Buscar actualizaciones" en Configuración, la app consulta en silencio al entrar y muestra un AlertDialog propio si hay una versión nueva (Release 2009.5.0.0)
- 🎨 **Discovery Kids Launcher** — Pantalla de inicio real de la app (RELEASE 2013.6.0.0: ya no depende de ningún interruptor "Experimental", ELIMINADO por completo), rediseñada a la estética del sitio web clásico de Discovery Kids Latinoamérica: franja verde curva con el logo, botón "Iniciar canal" en píldora amarilla, panel de contenido semitransparente (se ve el fondo de horario detrás) — celeste de mañana, anaranjado de tarde, violeta de noche —, con el ícono de la app cambiando según el horario también. Desde acá elegís cuántos programas querés (hasta 24) y, para cada uno, entrás a "⚙️ Opciones" para elegir sus Episodios (uno o más videos, Release 5.8.0), continuamos, Intro, Créditos y más.
- 🎬 **Episodios de Programa** — Un programa puede tener varios videos en vez de uno solo. Entre episodios se muestran los comerciales de siempre; al terminar el último, salen los Créditos (si están configurados). Cada video debe durar al menos 1 minuto (Release 5.8.0)
- 📺 **Contenedor de video en 4:3, siempre** — El contenedor donde vive el video (junto con el ScreenBug y el efecto CRT) está siempre en proporción 4:3, sin excepción (Release 2009.5.2.1). "Forzar 4:3" (Configuración) decide qué pasa con el video *dentro* de esa caja: activado, se estira para llenarla exacto; desactivado, se ajusta preservando su proporción real sin estirarse — un video 16:9, por ejemplo, encaja con franjas arriba/abajo en vez de deformarse.

---

## 🎮 Cómo Usar

¿Querés experimentar tu propio canal de Discovery Kids? Solo necesitás **4 videos** de tu elección. Así se hace:

**1. Descargá 4 videos a tu gusto**

Pueden ser episodios de tu serie favorita, películas cortas, o cualquier contenido que quieras ver como si fuera un canal de TV. El formato recomendado es `.mp4`.

> 🎨 **Recomendado:** desde RELEASE 2013.6.0.0 la app abre directo en el Discovery Kids Launcher (ya no hace falta activar nada) — ahí podés elegir el video de cada programa con el selector de archivos del sistema, sin necesidad de renombrarlo ni copiarlo a Videos, y elegir cuántos programas querés (hasta 24). Si preferís el método clásico de renombrar archivos (abajo), dejalo así nomás — igual funciona como respaldo si no elegís un video para algún programa.

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

> ℹ️ **Corrección sobre el IDE (RELEASE 2013.6.0.0):** el cambio de IDE anunciado para la 6.1.0 ya **no** va a ser a CodeAssist. El proyecto pasa a otro IDE — que a diferencia de CodeAssist, **sí es compatible con Android Studio**. ⚠️ *Nota de Claude: el nombre exacto del nuevo IDE no quedó claro en el pedido original ("Code No THE Go") — Keyler, si podés confirmarlo lo corrijo en la próxima actualización.*

Antes de comenzar, asegurate de tener instalado lo siguiente:

- [AndroidIDE](https://androidide.com/) o Android Studio
- Java 11+ / Kotlin runtime
- Dispositivo Android o emulador (API 24+)
- Permisos de almacenamiento habilitados en el dispositivo

### Instalación

**1. Clonar el repositorio**

```bash
git clone https://github.com/keylerperales07-commits/DiscoveryKidsChannel.git
```

**2. Abrir en AndroidIDE (o Android Studio)**

- Lanzar **AndroidIDE** (o Android Studio)
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

> ✅ **Versión estable disponible — `v2014.6.1.0`** *(Era Doki 1.0 · Era 2014)* — NextProgram ya no aparece entre episodios de un mismo programa (solo antes del próximo programa real). Nuevo evento Halloween (31 de octubre) — quedan solo Navidad y Halloween. ActionBar del Discovery Kids Launcher con el color del header. Edición Normal: ícono según horario investigado a fondo, screenbug oculto al aparecer NextProgram. Edición HD: screenbug_start corregido a 5s.

### 🚀 Última versión estable — `v2014.6.1.0` *(Release · Era Doki 1.0 · Era 2014)*
> *NextProgram ya no aparece entre episodios de un mismo programa. Nuevo evento Halloween — quedan solo Navidad y Halloween. ActionBar del Launcher con el color del header. Edición Normal: ícono investigado a fondo, screenbug oculto al aparecer NextProgram. Edición HD: screenbug_start a 5s.*

- 🐛 **NextProgram entre episodios** — corregido en ambas ediciones: ahora solo aparece antes del próximo programa real, no entre episodios de uno mismo.
- 🐛 **Ícono según horario (Edición Normal)** — investigado a fondo (mismo código que en HD, donde ya funciona); se recomienda desinstalación limpia si venís actualizando desde antes del fix anterior.
- 🐛 **screenbug oculto al aparecer NextProgram (Edición Normal)** y **screenbug_start a 5s (Edición HD)**.
- 🎨 **ActionBar del Launcher** con el color del header (ambas ediciones).
- 🆕 **Evento Halloween** (31 de octubre) — 🗑️ eliminados Año Nuevo, Pascua y Día de la Tierra (ambas ediciones).

<details>
<summary>📜 Versiones estables anteriores</summary>

**`v2013.6.0.0.01`** *(Era Doki 1.0 · Era 2013 · "La Era Planetaria")* — Consolida la Preview 2013.6.0.0.3. NextProgram2 corregido a exactamente 30s antes del final. Crash al iniciar corregido a fondo (mismo bug que hacía que el ícono de la app no cambiara según el horario). Se elimina por completo "Funciones experimentales". Launcher con cuadros semitransparentes, acentos amarillos y texto negro/amarillo. Nueva app hermana: Discovery Kids HD.
- 🐛 **NextProgram2** corregido a 30s exactos antes del final (antes compartía el tiempo con NextProgram1).
- 🐛 **Crash al iniciar cuando cambia el horario** + **ícono que no cambiaba** — misma causa, investigada a fondo y corregida.
- 🗑️ **"Funciones experimentales" eliminado** — Discovery Kids Launcher es ahora, siempre, la pantalla de inicio.
- 🎨 **Launcher con transparencia** — panel semitransparente (se ve el fondo de horario detrás), acentos amarillos en los cuadros sin color propio, texto negro/títulos amarillos.
- 🆕 **Discovery Kids HD** — nueva app hermana, disponible como asset adicional en este mismo release. Ver [📺 Discovery Kids HD](#-discovery-kids-hd) más abajo.

**`v5.8.0`** *(Era Doki 1.0 · Era 2012 · "Parque Imaginario")* — Episodios de Programa (varios videos por programa, comerciales entre ellos, créditos al final), sistema de Eventos rediseñado (switch maestro + selector manual, movido a Configuración), banner de clasificación al iniciar un programa, restricción de duración mínima, y 4 correcciones: video corrido en Nextprogram2, ActionBar tapando contenido (edge-to-edge), y líneas de texto pegadas.
- 🆕 **Episodios de Programa** — un programa puede tener varios videos en vez de uno solo; entre episodios se muestran comerciales, al terminar el último salen los Créditos.
- 🆕 **Sistema de Eventos rediseñado** — switch maestro "Activar eventos" + selector manual de evento (Normal/Navidad/Año Nuevo/Pascua/Día de la Tierra — la lista de eventos disponibles cambió más adelante, ver `v2014.6.1.0` arriba), movido a Configuración.
- 🆕 **Banner de clasificación** — `clasif_banner.gif` al arrancar cada programa, visible 17s.
- ⚙️ **Duración mínima de 1 minuto** para cualquier episodio.
- 🐛 **Video corrido con Nextprogram2** en Créditos — corregido con un ajuste de posición específico.
- 🐛 **ActionBar tapando contenido** (edge-to-edge de `targetSdk 36`) — corregido con manejo explícito de insets en las 3 pantallas con ActionBar.
- 🐛 **Líneas de texto pegadas** — agregado espaciado entre líneas en los textos multilínea afectados.

**`v5.7.0`** *(Era Doki 1.0 · Era 2012 · "Parque Imaginario")* — Cambio de Era (2011→2012). Consolida la Preview 2011.5.6.0.60 (CrtOverlayView único y compartido, ancho real del recuadro de NextProgram) como Release estable. Nuevo contenido: comerciales de la Era 2012.
- 🎨 **Cambio de Era 2011 → 2012** — nuevos comerciales correspondientes.
- 🐛 **Un solo CrtOverlayView, compartido** entre el video y el recuadro de NextProgram (antes había dos instancias animadas en paralelo).

**`v5.5.0`** *(Era Doki 1.0 · Era 2011 · "Parque Imaginario")* — Cambio de Era (2010→2011). Nueva Activity Configuración de Programa (extraída del Launcher), NextProgram personalizado, 4 ScreenBugs de eventos (Navidad, Año Nuevo, Pascua, Día de la Tierra — todos configurables), y 4 correcciones: ActionBar tapando contenido, ScreenBug repitiéndose entre Intro/Programa/Créditos, CRT ausente en NextProgram, y ajuste fino de la posición del recuadro.
- 🆕 **Nueva Activity: Configuración de Programa** — la sección "Programas" del Launcher (cantidad, videos, personalizaciones) pasa a tener su propia pantalla.
- 🎬 **NextProgram personalizado por programa** — imagen o GIF propio en vez del de fábrica.
- 🎄 **3 ScreenBugs de eventos nuevos** — Año Nuevo, Pascua (fechas 2026-2030), Día de la Tierra — más Navidad, ahora también configurable.

**`2011.5.6.0.60-preview`** — 2 bug fixes de investigación a fondo sobre NextProgram, consolidados como estables en la v5.7.0: CrtOverlayView único y compartido, y el ancho real del recuadro corregido con precisión de píxel.

> ℹ️ El historial completo de versiones (incluyendo `v5.0.0` a `v5.4.1` y anteriores) vive en [`CHANGELOG.md`](./CHANGELOG.md).

</details>

---

## 📺 Discovery Kids HD

A partir de la RELEASE 2013.6.0.0, el asset del release en GitHub incluye, además del `.apk` de siempre, un **`.apk` de la variante HD** — la misma app, pero pensada para reproducir contenido en alta definición, sin los elementos pensados para simular la baja definición de la Era 2005–2012: sin efecto CRT, sin música de fondo, y sin el forzado de proporción 4:3 (el video respeta su proporción real en pantalla completa).

Es un paquete (`applicationId`) distinto — `com.keyler.discoverykidshd` — así que se puede tener instalada junto a la app normal sin conflicto.

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
