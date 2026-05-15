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
  <img src="https://img.shields.io/badge/Version-2.4.0-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/Release-2.4.0-orange?style=flat-square"/>
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
- 📣 **Comerciales Dinámicos** — Bloques de publicidad que rotan durante la programación, incluyendo contenido de la era Y2K
- ➡️ **Enseguidas** — Clips de transición "a continuación" entre programas
- 🎵 **Música de Fondo** — Música ambiente durante la reproducción de programas (volumen al 5%)
- 🖥️ **Modo Pantalla Completa Inmersivo** — Sin distracciones de interfaz, experiencia TV pura
- 📡 **Overlay Visual CRT** — Efectos de scanlines y pantalla para esa sensación retro de televisor
- 💾 **Reanudación de Sesión** — La app recuerda dónde quedaste al volver desde el fondo
- 🆕 **Nuevo Screenbug** — Marca de agua con el logo del canal en pantalla
- ⏸ **Pantalla "Ya Volvemos"** — Pantalla intersticial auténtica de "Volvemos en un momento"

---

## 🎮 Cómo Usar

¿Querés experimentar tu propio canal de Discovery Kids? Solo necesitás **4 videos** de tu elección. Así se hace:

**1. Descargá 4 videos a tu gusto**

Pueden ser episodios de tu serie favorita, películas cortas, o cualquier contenido que quieras ver como si fuera un canal de TV. El formato recomendado es `.mp4`.

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
| **Choreographer** | Renderizado frame a frame |
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

### Última versión estable — `v2.4.0`
- ✅ Release estable basada en la rama beta `2000.2.4.0.x`
- ✅ `bumper2.mp4` actualizado a 480p
- ✅ Volumen de música de fondo ajustado al 5%
- ✅ `enseguida4` y `ya_volvemos4` actualizados a 480p
- ✅ Pausa automática al mostrar el `AlertDialog` de salida
- ✅ Migración de textos a `strings.xml`

<details>
<summary>v2000.2.3.0</summary>

- ✅ Nuevos comerciales de la era Y2K (~estética del año 2000)
- ✅ Dos bumpers actualizados a mayor resolución

</details>

---

## 🔧 Última versión Beta — `v2000.2.4.0.42`

> *Pre-release activa en pruebas. No destinada a producción.*

- 📺 **`bumper2.mp4` actualizado a 480p** — Mejor resolución en el bumper 2, anteriormente en 360p
- 🎵 **Volumen de música de fondo al 5%**
- ➡️ **`enseguida4` y `ya_volvemos4` a 480p**
- ⏸ **Pausa automática al abrir el diálogo de salida**

Consultá [`CHANGELOG.md`](./CHANGELOG.md) para ver las notas completas de esta beta.

---

## ⚠️ Notas Importantes

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
