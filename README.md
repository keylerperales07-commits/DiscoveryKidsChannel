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
  <img src="https://img.shields.io/badge/Última_versión-v4.6.1-brightgreen?style=flat-square"/>
  <img src="https://img.shields.io/badge/Era-2009-blue?style=flat-square"/>
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
- 🆕 **Nuevo Screenbug** — Marca de agua con el logo del canal en pantalla, con aparición y ocultado adaptativos al reanudar
- ⏸ **Pantalla "Ya Volvemos"** — Pantalla intersticial auténtica de "Volvemos en un momento"
- 🎞️ **Transiciones Profesionales FadeIn / FadeOut** — Cada cambio de video aplica un **FadeOut de 500 ms** y un **FadeIn de 1 segundo**, cubriendo enseguidas, bumpers, comerciales, transiciones ya_regresa/continuamos y arranque/retoma de programas
- ⏭️ **Navegación Prev / Next por bloque completo** — Los botones de canal navegan al bloque completo del programa (Enseguida → StandaloneCommercial → Bumper → Programa), igual que cambiar de canal en TV real
- ⚙️ **Pantalla de Configuración** — Accesible desde el botón de ajustes, con lista simple estilo Android Settings. Permite alternar música de fondo, efecto CRT y Forzar 4:3, y ajustar la duración del Screenbug y el intervalo entre comerciales — cada opción muestra su valor predeterminado
- 🔄 **Actualizador integrado** — Desde Configuración, "Buscar actualizaciones" consulta el último release de GitHub; si hay una versión más nueva, descarga el `.apk` con OkHttp (mostrando progreso y tamaño en vivo) y abre el instalador del sistema. Un switch "Habilitar versiones Preview" (desactivado por defecto) permite que también instale releases Preview, no solo estables. La pantalla del Actualizador calca el diseño nativo de "Actualización del sistema" de Android
- 🎬 **Discovery Kids Launcher** — Desde Configuración → "Elegir programas", una pantalla nueva (mismo diseño que Configuración) donde elegís con un switch cuáles de los 4 programas querés que salgan al aire. El que desactivás se saltea en la programación y en la navegación Prev/Next, igual que si el archivo no estuviera en Videos

---

## 🎮 Cómo Usar

¿Querés experimentar tu propio canal de Discovery Kids? Solo necesitás **4 videos** de tu elección. Así se hace:

**1. Descargá 4 videos a tu gusto**

Pueden ser episodios de tu serie favorita, películas cortas, o cualquier contenido que quieras ver como si fuera un canal de TV. El formato recomendado es `.mp4`.
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
| **VideoView** | Motor de reproducción de video |
| **MediaPlayer** | Gestión de audio y medios |
| **SharedPreferences** | Persistencia de sesión |
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

### 🚀 Última versión estable — `v4.6.1` *(Release · Era Doki 1.0 · Era 2009)*
> *Cambio de Era (2008→2009), 3 Screenbug secuenciales, y 2 bug fixes críticos.*

- 🎬 **3 Screenbug secuenciales** — animación de entrada (start.gif), imagen estática (screenbug.png), animación de salida (end.gif) en momentos específicos del programa.
- 🎬 Se agregó **bumper6** como indicando que esta cerca la fase 4.
- 🐛 **AppUpdater arreglado** — ya no cree que hay versión más nueva cuando actualizas de una preview a la release final de la misma versión.
- 🐛 **Prev/Next arreglado** — ahora navega en orden real a través del playlist, no saltando entre programas.


---

## ⚠️ Notas Importantes

- **Los videos de programas (`pro1–4.mp4`) deben estar en resolución 480p o inferior.** Resoluciones de 720p en adelante causan que `VideoView` active aceleración de hardware, lo que hace que el ScreenBug quede oculto detrás del video. Esta es una limitación conocida del sistema actual que se resolverá en una versión futura con `TextureView`.
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
