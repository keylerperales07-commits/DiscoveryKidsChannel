# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/),
y este proyecto sigue el estándar de [Versionado Semántico](https://semver.org/lang/es/).


## [2001.2.5.0.51-beta] — 2026-05-16 · Beta Pre-Release · Era 2001

> *Pre-release de pruebas internas. Esta versión puede contener comportamientos inestables. No destinada a producción.*

### Agregado

**Sistema de Tallas (`tallas.mp4`)**
- Se incorpora el segmento **Talla** a la lista de programación como un nuevo tipo de ítem (`PlayItem.Talla`).
- Cuenta con tres variaciones: `tallas_1.mp4`, `tallas_2.mp4` y `tallas_3.mp4`, ubicadas en `res/raw/`.
- La selección es **aleatoria**, evitando repetir la misma variación dos veces seguidas (igual que bumpers y comerciales).
- Las tallas se ubican **entre la enseguida post-programa y el bumper**:
  ```
  Enseguida (1/2) → Talla (1/2/3) → Bumper → Programa
  ```

**ScreenBug dinámico según la talla reproducida**
- La talla elegida determina qué drawable del screenbug se muestra durante el programa siguiente:
  - `tallas_1` → `screenbug_small`
  - `tallas_2` → `screenbug_medium`
  - `tallas_3` → `screenbug_large`
- Implementado mediante el mapa `TALLA_SCREENBUG_MAP` en el `companion object`.
- `fadeInBug()` aplica `setImageResource(currentScreenBugRes)` antes de animar.
- `currentScreenBugRes` se persiste en `SharedPreferences` (`PREF_SCREENBUG_RES`) para que la sesión guardada restaure el screenbug correcto al reanudarse.

### Modificado

**Volumen de música de fondo ajustado al 8%**
- El volumen del `MediaPlayer` de música de fondo (`bg_music`) fue incrementado de **5% → 8%** (`0.08f`) en ambos canales (izquierdo y derecho).
- Cambio aplicado dentro de `startBgMusic()` en `LiveDiscoveryKids.kt`.
- El ajuste aumenta la presencia ambiental de la música sin interferir con el audio del video principal.

**`enseguida2.mp4` reemplazada por enseguida de burbujas**
- El archivo `enseguida2.mp4` fue reemplazado por un nuevo clip de transición con estética de burbujas.
- No hay cambios en la duración ni en la lógica de reproducción.

---

## [2001.2.5.0.50-beta] — 2026-05-15 · Beta Pre-Release · Era 2001

> *Pre-release de pruebas internas. Esta versión puede contener comportamientos inestables. No destinada a producción.*

### Modificado

**Comerciales 1, 2 y 3 actualizados a la Era 2001**
- Los archivos `comercial1`, `comercial2` y `comercial3` fueron reemplazados por versiones basadas en la estética y el contenido del año 2001, en línea con la nueva era del canal.
- El cambio refleja la transición del canal de la Era 2000 a la Era 2001.

**`enseguida1.mp4` actualizado a la Era 2001**
- El archivo `enseguida1.mp4` fue reemplazado por una versión basada en 2001.
- El clip de transición ahora es coherente con la identidad visual de la Era 2001.

**`screenbug.webp` actualizado a la Era 2001**
- El archivo `screenbug.webp` fue reemplazado por una versión basada en 2001.
- La marca de agua del canal en pantalla ahora refleja la identidad visual de la Era 2001.

---

## [2.4.1] — 2026-05-16 · Release · Era 2000

> *Versión estable de corrección de errores y mejoras visuales basada en `2.4.0`.*

### Corregido

**Posición del video guardada en tiempo real**
- El `positionTrackerRunnable` ahora se reprograma cada **16 ms** (~60 fps) en lugar de cada 500 ms.
- Esto elimina el retraso de aproximadamente 1 segundo que se producía al volver de segundo plano, ya que la posición guardada era la del último ciclo de 500 ms en lugar de la posición actual.
- Cambio aplicado en `LiveDiscoveryKids.kt` y portado a `LiveDiscoveryKids_Beta.kt`.

### Agregado

**FadeOut del `VideoView` al comenzar el comercial**
- Cuando se inicia el bloque publicitario (`playCommercial`), el `VideoView` ahora realiza un **fade a negro** de 500 ms antes de reproducir la enseguida pre-comercial.
- La transición del programa al comercial es ahora suave en lugar de abrupta.
- Implementado mediante `videoView.animate().alpha(0f).setDuration(500L).withEndAction { ... }`.

**FadeIn del `VideoView` al reanudar el programa**
- Cuando el programa retoma la reproducción (`beginProgramSegment`) — tanto tras un bloque comercial como en el arranque inicial — el `VideoView` realiza un **fade desde negro** de 500 ms.
- Implementado estableciendo `videoView.alpha = 0f` antes de `videoView.start()` y animando a `1f`.

> **Nota:** Ambas mejoras visuales se aplicaron también en `LiveDiscoveryKids_Beta.kt`.

---

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
| 2001.2.5.0.51-beta   | 2026-05-16 | 🔧 Beta    | Sistema de Tallas; ScreenBug dinámico; volumen al 8%; `enseguida2` de burbujas |
| 2001.2.5.0.50-beta   | 2026-05-15 | 🔧 Beta    | Comerciales 1-3 a Era 2001; `enseguida1.mp4` y `screenbug.webp` a Era 2001 |
| 2.4.1                | 2026-05-16 | 🚀 Release | Bug fix posición en tiempo real; FadeOut al comercial; FadeIn al programa |
| 2000.2.4.0.42-beta   | 2026-05-14 | 🔧 Beta    | `bumper2.mp4` actualizado a 480p (antes 360p)                          |
| 2000.2.4.0.41-beta   | 2026-05-13 | 🔧 Beta    | Volumen al 5%; `enseguida4` y `ya_volvemos4` a 480p                    |
| 2000.2.4.0.40-beta   | 2026-05-12 | 🔧 Beta    | Pausa en AlertDialog de salida; volumen bg_music al 2%                 |
| 2000.2.3.1           | 2026-05-12 | 🚀 Release | Migración de textos a `strings.xml`                                    |
| 2000.2.3.0           | 2026-05-11 | 🚀 Release | Comerciales Y2K agregados; mejora de resolución en bumpers             |

---
