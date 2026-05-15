# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/),
y este proyecto sigue el estándar de [Versionado Semántico](https://semver.org/lang/es/).


## [2.4.0] — 2026-05-15 · Release · Era 2000

> *Versión estable de lanzamiento basada en la rama beta `2000.2.4.0.x`.*

### Agregado

**Consolidación de cambios de la beta 2.4.0.x**
- Se integraron en la versión Release 2.4.0 todos los cambios validados durante la rama beta `2000.2.4.0.x`.
- Esta release reúne ajustes de video, audio y estabilidad antes de la publicación estable.

### Corregido

**Pausa al mostrar el AlertDialog de salida**
- El video principal y la música de fondo ahora se pausan automáticamente cuando el `AlertDialog` de confirmación de salida (`showExitConfirmationDialog`) es visible.
- Al cancelar el diálogo (botón **Cancelar**), el video y la música se reanudan exactamente desde donde fueron pausados.

### Modificado

**`bumper2.mp4` actualizado a 480p**
- El archivo `bumper2.mp4` fue reemplazado por una versión de mayor resolución **(480p)**, anteriormente en 360p.
- La calidad visual de este bumper ahora es consistente con el resto de los assets del canal.
- No hay cambios en la duración ni en la lógica de reproducción.

**Volumen de música de fondo ajustado al 5%**
- El volumen del `MediaPlayer` de música de fondo (`bg_music`) fue incrementado de **2% → 5%** (`0.05f`) en ambos canales (izquierdo y derecho).
- El ajuste aumenta ligeramente la presencia ambiental de la música sin interferir con el audio del video principal.

**Assets `enseguida4` y `ya_volvemos4` actualizados a 480p**
- Los archivos `enseguida4` y `ya_volvemos4` fueron reemplazados por versiones de mayor resolución **(480p)**.
- La calidad visual de estos clips ahora es consistente con el resto de los assets de transición del canal.
- No hay cambios en la duración ni en la lógica de reproducción.

**Volumen de música de fondo ajustado al 2%**
- El volumen del `MediaPlayer` de música de fondo (`bg_music`) fue ajustado a `0.02f` (2%) en ambos canales (izquierdo y derecho).
- Este ajuste queda registrado dentro del historial de la rama beta que fue promovida a release.

### Migración incluida

**Migración de textos a `strings.xml`**
- Todos los textos hardcodeados en `LiveDiscoveryKids.kt` fueron extraídos al archivo de recursos `res/values/strings.xml`.
- El código ahora usa `getString(R.string.x)` en todos los casos, eliminando cadenas literales del código fuente.

---

## [2000.2.4.0.42-beta] — 2026-05-14 · Beta Pre-Release · Era 2000

> *Pre-release de pruebas internas. Esta versión puede contener comportamientos inestables. No destinada a producción.*

### Modificado

**`bumper2.mp4` actualizado a 480p**
- El archivo `bumper2.mp4` fue reemplazado por una versión de mayor resolución **(480p)**, anteriormente en 360p.
- La calidad visual de este bumper ahora es consistente con el resto de los assets del canal.
- No hay cambios en la duración ni en la lógica de reproducción.

---

## [2000.2.4.0.41-beta] — 2026-05-13 · Beta Pre-Release · Era 2000

> *Pre-release de pruebas internas. Esta versión puede contener comportamientos inestables. No destinada a producción.*

### Modificado

**Volumen de música de fondo ajustado al 5%**
- El volumen del `MediaPlayer` de música de fondo (`bg_music`) fue incrementado de **2% → 5%** (`0.05f`) en ambos canales (izquierdo y derecho).
- Cambio aplicado dentro de `startBgMusic()` en `LiveDiscoveryKids.kt`.
- El ajuste aumenta ligeramente la presencia ambiental de la música sin interferir con el audio del video principal.

**Assets `enseguida4` y `ya_volvemos4` actualizados a 480p**
- Los archivos `enseguida4` y `ya_volvemos4` fueron reemplazados por versiones de mayor resolución **(480p)**.
- La calidad visual de estos clips ahora es consistente con el resto de los assets de transición del canal.
- No hay cambios en la duración ni en la lógica de reproducción.

---

## [2000.2.4.0.40-beta] — 2026-05-12 · Beta Pre-Release · Era 2000

> *Pre-release de pruebas internas. Esta versión puede contener comportamientos inestables. No destinada a producción.*

### Corregido

**Pausa al mostrar el AlertDialog de salida**
- El video principal y la música de fondo ahora se pausan automáticamente cuando el `AlertDialog` de confirmación de salida (`showExitConfirmationDialog`) es visible.
- Anteriormente, el contenido continuaba reproduciéndose en segundo plano mientras el diálogo estaba activo.
- Al cancelar el diálogo (botón **Cancelar**), el video y la música se reanudan exactamente desde donde fueron pausados.

### Modificado

**Volumen de música de fondo ajustado al 2%**
- El volumen del `MediaPlayer` de música de fondo (`bg_music`) fue ajustado a `0.02f` (2%) en ambos canales (izquierdo y derecho).
- Cambio aplicado dentro de `startBgMusic()` en `LiveDiscoveryKids.kt`.
- El ajuste mejora la presencia ambiental de la música sin tapar el audio del video principal.

---

## [2000.2.3.1] — 2026-05-12 · Corrección de errores · Era 2000

> *Versión de mantenimiento enfocada en calidad de código y buenas prácticas de localización.*

### Corregido

**Migración de textos a `strings.xml`**
- Todos los textos hardcodeados en `LiveDiscoveryKids.kt` fueron extraídos al archivo de recursos `res/values/strings.xml`
- Afecta los siguientes elementos:
  - Título y mensaje del diálogo de reanudación de sesión (`¿Continuar donde estabas?`)
  - Botones del diálogo de reanudación: `Continuar`, `Empezar de nuevo`
  - Descripciones de posición en el diálogo: `Programa N (Xm Ys)`, `Espacio publicitario`, `Presentación de canal`, `Avance de próximo programa`
  - Título y mensaje del diálogo de confirmación de salida (`¿Salir del canal?`)
  - Botones de salida: `Salir y guardar`, `Salir sin guardar`, `Cancelar`
  - Texto de carga inicial del debug overlay (`Espere…`)
- El código ahora usa `getString(R.string.x)` en todos los casos, eliminando cadenas literales del código fuente

### Sin cambios funcionales

Esta versión no introduce nuevas funcionalidades ni modifica el comportamiento de la aplicación.

---

## [2000.2.3.0] — 2026-05-11

### Agregado
- **Nuevos Comerciales (Era Y2K)** — Se incorporó un nuevo conjunto de comerciales inspirados en la estética y el estilo de principios de los años 2000, enriqueciendo la biblioteca de contenido con material nostálgico y culturalmente relevante de esa época.

### Modificado
- **Mejora de Resolución de Bumpers** — Se actualizaron dos bumpers existentes a una resolución superior, mejorando la calidad visual general y asegurando compatibilidad con los estándares de pantalla modernos.

---

## Historial de Versiones

| Versión              | Fecha      | Canal      | Resumen                                                                 |
|----------------------|------------|------------|-------------------------------------------------------------------------|
| 2000.2.4.0.42-beta   | 2026-05-14 | 🔧 Beta    | `bumper2.mp4` actualizado a 480p (antes 360p)                          |
| 2000.2.4.0.41-beta   | 2026-05-13 | 🔧 Beta    | Volumen al 5%; `enseguida4` y `ya_volvemos4` a 480p                    |
| 2000.2.4.0.40-beta   | 2026-05-12 | 🔧 Beta    | Pausa en AlertDialog de salida; volumen bg_music al 2%                 |
| 2000.2.3.1           | 2026-05-12 | 🚀 Release | Migración de textos a `strings.xml`                                    |
| 2000.2.3.0           | 2026-05-11 | 🚀 Release | Comerciales Y2K agregados; mejora de resolución en bumpers             |

---
