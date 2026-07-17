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
  <img src="https://img.shields.io/badge/Última_versión-v5.2.0-brightgreen?style=flat-square"/>
  <img src="https://img.shields.io/badge/Era-2009-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/Fase-4-orange?style=flat-square"/>
</p>

---

## 📖 Descripción General

**Discovery Kids** es una aplicación Android que recrea fielmente la experiencia de ver un canal infantil de televisión clásico. Cuenta con reproducción lineal continua con elementos auténticos de transmisión — bumpers, comerciales, pantallas de transición (*enseguidas*), música de fondo y un overlay estilo CRT en pantalla completa — todo diseñado para hacerte sentir que estás viendo televisión real otra vez.

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
- 📣 **Comerciales Dinámicos** — Bloques de publicidad que rotan durante la programación con intervalos aleatorios entre 3 y 9 minutos, incluyendo contenido de la era Y2K
- ➡️ **Enseguidas** — Clips de transición "a continuación" entre programas
- 🎵 **Música de Fondo** — Música ambiente durante la reproducción de programas (volumen al 8%)
- 🖥️ **Modo Pantalla Completa Inmersivo** — Sin distracciones de interfaz, experiencia TV pura
- 📡 **Overlay Visual CRT** — Efectos de scanlines y pantalla para esa sensación retro de televisor
- 💾 **Reanudación de Sesión** — La app recuerda dónde quedaste al volver desde el fondo
- 🆕 **ScreenBug de 3 fases** — Marca de agua animada con el logo del canal: aparición (GIF), estático (PNG) y salida (GIF), apareciendo/desapareciendo de forma instantánea en momentos específicos del programa. Los GIF se reproducen con `GifMovieDrawable`, basado en la API nativa `android.graphics.Movie` (sin librerías externas — Release 2009.5.1.0). Nuevo contenido de Mayo–Julio 2009 (Release 2009.5.1.0)
- ⏸ **Pantalla "Ya Volvemos"** — Pantalla intersticial auténtica de "Volvemos en un momento"
- 🎞️ **Transiciones Profesionales FadeIn / FadeOut** — Cada cambio de video aplica un **FadeOut de 500 ms** y un **FadeIn de 1 segundo**, cubriendo enseguidas, bumpers, comerciales, transiciones ya_regresa/continuamos y arranque/retoma de programas
- ⏭️ **Navegación Prev / Next por bloque completo** — Los botones de canal navegan al bloque completo del programa (Enseguida → StandaloneCommercial → Bumper → Programa), igual que cambiar de canal en TV real
- ⚙️ **Pantalla de Configuración** — Accesible desde el botón de ajustes, con lista simple estilo Android Settings. Permite alternar música de fondo, efecto CRT y Forzar 4:3, y ajustar la duración del Screenbug y el intervalo entre comerciales — cada opción muestra su valor predeterminado
- 🔄 **Actualizador integrado** — Desde Configuración, "Buscar actualizaciones" consulta el último release de GitHub; si hay una versión más nueva, descarga el `.apk` con OkHttp (mostrando progreso y tamaño en vivo) y abre el instalador del sistema. Un switch "Habilitar versiones Preview" (desactivado por defecto) permite que también instale releases Preview, no solo estables. La pantalla del Actualizador calca el diseño nativo de "Actualización del sistema" de Android
- 🔔 **Aviso de actualización al abrir la app** — Además de "Buscar actualizaciones" en Configuración, la app consulta en silencio al entrar y muestra un AlertDialog propio si hay una versión nueva (Release 2009.5.0.0)
- 🧪 **Discovery Kids Launcher (Experimental)** — Con "Habilitar funciones experimentales" activado en Configuración, la app abre en una pantalla nueva, rediseñada a **Material Design 3 puro** (esquema de color claro/oscuro automático, ActionBar original de Android — Release 2009.5.1.0) desde donde elegís el video de cada programa con el selector de archivos del sistema (sin renombrar ni copiar nada a Videos), cuántos programas querés (hasta 24) y, si querés, un ya_regresa/continuamos propio para cada uno. Desactivado, la app sigue abriendo directo al canal con los 4 programas clásicos (Release 2009.5.0.0)
- 🖥️ **Compatibilidad con video 720p+** — Motor de video opcional (Configuración → "Compatibilidad de video" → "Recortar 4:3", antes llamado "Usar TextureView"), para que el ScreenBug no quede oculto detrás de videos de alta resolución. Si detecta un programa de 720p o más sin esta opción activada, la app avisa con un diálogo (Release 2009.5.0.0). Se deshabilita mientras "Forzar 4:3" esté activado, porque en ese caso ya se recorta a 4:3 de todas formas (Release 2009.5.2.0). "Forzar 4:3" ahora sí respeta el switch en ambos motores de video — antes recortaba siempre, sin importar el estado (Release 2009.5.1.0) — y con el switch desactivado, el video ya no se estira a 16:9: se ajusta preservando su proporción real (Release 2009.5.2.0)

---

## 🎮 Cómo Usar

¿Querés experimentar tu propio canal de Discovery Kids? Solo necesitás **4 videos** de tu elección. Así se hace:

**1. Descargá 4 videos a tu gusto**

Pueden ser episodios de tu serie favorita, películas cortas, o cualquier contenido que quieras ver como si fuera un canal de TV. El formato recomendado es `.mp4`.

> 🧪 **Alternativa (Experimental):** si activás "Habilitar funciones experimentales" en Configuración, la app abre en el Discovery Kids Launcher, donde podés elegir el video de cada programa con el selector de archivos del sistema — sin necesidad de renombrarlo ni copiarlo a Videos — y elegir cuántos programas querés (hasta 24). Si preferís el método clásico de abajo, dejá Experimental desactivado.
## ⚠️ Nota técnica

> Los videos de programas (`pro1–4.mp4`) deben estar en **resolución 480p o inferior**. De lo contrario tendras problemas de programación.

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

La app detecta automáticamente los videos, los intercala con bumpers, comerciales, enseguidas y música de fondo, y te da una experiencia completa de canal de televisión retro. ¡Listo!

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
| **VideoView / TextureView** | Motor de reproducción de video (TextureView opcional, para 720p+) |
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

### 🚀 Última versión estable — `v5.2.0` *(Release · Era Doki 1.0 · Era 2009 · "Parque Imaginario")*
> *Release de investigación a fondo: 2 bugs de arrastre finalmente resueltos con causa raíz identificada — el ScreenBug "reiniciándose" al reanudar y el video estirado a 16:9 con "Forzar 4:3" desactivado. El ítem de Configuración del Launcher pasa al menú de overflow original de Android, y "Usar TextureView" se renombra a "Recortar 4:3" con deshabilitado condicional. Nuevo Screenbug de Julio 2009 – 2011.*

- 🐛 **ScreenBug: causa raíz real encontrada.** Cada resume (abrir Configuración y volver, o un ratito en segundo plano) ocultaba el ScreenBug de golpe y no lo restauraba hasta el próximo evento programado — ahora se restaura de inmediato si corresponde, sin reiniciar la animación.
- 🐛 **Video estirado a 16:9 con "Forzar 4:3" desactivado: causa raíz real encontrada.** `DkVideoView` se reescribió con fit de aspecto real compartido entre los dos motores de video — ya no se estira, se ajusta preservando la proporción real (pillarbox/letterbox).
- 🎨 El ítem "Configuración" del Launcher pasa al menú de overflow original de Android (3 puntos) en vez de ícono fijo en la ActionBar.
- ⚙️ "Usar TextureView" se renombra a **"Recortar 4:3"** y se deshabilita mientras "Forzar 4:3" esté activado.

<details>
<summary>📜 Versiones estables anteriores</summary>

**`v5.1.0`** *(Era Doki 1.0 · Era 2009 · "Parque Imaginario")* — Discovery Kids Launcher rediseñado a Material Design 3 puro (esquema claro/oscuro automático, ActionBar original de Android en vez del MenuBar hecho a mano). 3 bug fixes de la 2009.5.0.0: "Forzar 4:3" no respetaba el switch, recorte de video con TextureView, y el ScreenBug reiniciándose al cambiar de Activity o volver de segundo plano. Ajustes de timing del ScreenBug de 3 fases y reproducción de GIF migrada a una solución 100% nativa (sin librerías externas). Nuevo Screenbug de Mayo–Julio 2009.
- 🎨 **Discovery Kids Launcher a Material Design 3** — ActionBar original de Android (no más MenuBar custom), esquema de color azul con versión clara y oscura automática (DayNight), componentes Material 3 (`MaterialButton`, `MaterialCardView`).
- 🐛 **"Forzar 4:3" corregido** — antes recortaba el video a 4:3 sin importar si el switch estaba activado o desactivado; ahora sí lo respeta, en ambos motores de video (VideoView/TextureView).
- 🎬 **ScreenBug**: timing ajustado (start se oculta a los 15 s sin fade, PNG aparece inmediato, end se oculta 5 s después de mostrarse en vez de loopear 20 s), y los GIF ahora se reproducen con una solución nativa propia (`GifMovieDrawable`, sin librerías externas).

**`v5.0.0`** *(Era Doki 1.0 · Era 2009 · "Parque Imaginario")* — Primera versión de la rama 5.x — arranca la Fase 4. Discovery Kids Launcher pasa a ser la pantalla de inicio (Experimental), con selector de video por programa, hasta 24 programas y ya_regresa/continuamos personalizados. AlertDialog para video 720p+, motor de video TextureView opcional, y aviso de actualización al abrir la app.
- 🧪 **Discovery Kids Launcher (Experimental)** — nueva pantalla de inicio real de la app, detrás de "Habilitar funciones experimentales" en Configuración (desactivado por defecto). Elegí el video de cada programa sin renombrar ni copiar nada, cuántos programas querés (1–24) y personalizá el ya_regresa/continuamos de cada uno.
- 🖥️ **Compatibilidad de video 720p+** — nuevo motor de video opcional (TextureView) para que el ScreenBug no quede detrás del video en resoluciones altas; aviso automático si detecta un programa así sin la opción activada.
- 🔔 **Aviso de actualización al abrir la app** — además de "Buscar actualizaciones" en Configuración, ahora también se consulta solo al entrar.
- ⚙️ Se eliminó de Configuración el ítem "Elegir programas" (ahora vive en Discovery Kids Launcher).

**`v4.6.1`** *(Era Doki 1.0 · Era 2009)* — Cambio de Era (2008→2009), 3 Screenbug secuenciales, y 2 bug fixes críticos.
- 🎬 3 Screenbug secuenciales — animación de entrada (start.gif), imagen estática (screenbug.png), animación de salida (end.gif) en momentos específicos del programa.
- 🎬 Se agregó bumper6 como indicando que esta cerca la fase 4.
- 🐛 AppUpdater arreglado — ya no cree que hay versión más nueva cuando actualizas de una preview a la release final de la misma versión.
- 🐛 Prev/Next arreglado — ahora navega en orden real a través del playlist, no saltando entre programas.

</details>


---

## ⚠️ Notas Importantes

- **Los videos de programas (`pro1–4.mp4`) deben estar en resolución 480p o inferior si usás el motor de video clásico (VideoView).** Resoluciones de 720p en adelante causan que `VideoView` active aceleración de hardware, lo que hace que el ScreenBug quede oculto detrás del video. Desde la Release 2009.5.0.0 podés activar "Recortar 4:3" (antes "Usar TextureView") en Configuración → "Compatibilidad de video" para reproducir 720p+ sin ese problema (requiere reabrir el canal); si no la activás, la app te avisa con un diálogo apenas detecta un video así. Esa opción se deshabilita mientras "Forzar 4:3" esté activado (Release 2009.5.2.0). El switch "Forzar 4:3" (Configuración) funciona correctamente en ambos motores desde la Release 2009.5.1.0 — antes recortaba el video a 4:3 sin importar su estado — y desde la 2009.5.2.0, con "Forzar 4:3" desactivado el video ya no se estira a 16:9: se ajusta preservando su proporción real.
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
