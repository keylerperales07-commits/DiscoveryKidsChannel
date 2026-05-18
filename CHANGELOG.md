# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
y este proyecto sigue el estándar de [Versionado Semántico](https://semver.org/lang/es/).


## [2001.2.5.0] — 2026-05-18 · Release · Era 2001

> *Versión estable de producción. Consolida todos los cambios validados durante la rama beta `2001.2.5.0.5x`.*

### Corregido

**BUG FIX URGENTE: `VideoView` invisible durante el bloque comercial**
- Al agregar el `FadeOut` del `VideoView` en `2.4.1`, nunca se colocó la lógica para restablecer el `alpha` al **comenzar** el bloque comercial.
- Como consecuencia, la enseguida pre-comercial, el comercial y el `ya_volvemos` se reproducían de forma completamente **invisible** (el `VideoView` quedaba en `alpha = 0f` al terminar el FadeOut).
- **Fix:** Se agrega `videoView.alpha = 1f` (sin animación, sin fadeIn) al inicio del bloque `withEndAction` del FadeOut en `playCommercial()`. Garantiza que el bloque comercial sea visible desde el primer frame.

### Modificado

**Selección de tallas: de aleatoria a basada en hora y día de la semana**
- Se elimina la selección aleatoria de tallas (`candidates.random()`).
- La talla ahora se determina según la franja horaria (lunes a viernes):
  - `tallas_1` → 06:00 – 12:59
  - `tallas_2` → 13:00 – 16:29
  - `tallas_3` → 16:30 – 23:59
  - `00:00 – 05:59` → **sin talla** (se salta la reproducción y se usa `screenbug.webp` directamente)
- De **sábado a domingo** se elige siempre `tallas_4`, y en ese caso también se usa `screenbug.webp`.
- La variable `lastTallaRes` fue eliminada, ya que ya no aplica.
- El `ScreenBug` resultante:
  - `tallas_1` → `screenbug_small` | `tallas_2` → `screenbug_medium` | `tallas_3` → `screenbug_large`
  - `tallas_4` / sin talla → `screenbug` (`screenbug.webp`)

**Enseguidas post-programa: de aleatorias a basadas en horario**
- Se elimina la selección aleatoria de enseguidas post-programa (`candidates.random()`).
- La enseguida ahora se determina según la franja horaria (lunes a viernes):
  - `enseguida1` → 00:00 – 12:59
  - `enseguida2` → 13:00 – 16:29
  - `enseguida5` → 16:30 – 23:59
- De **sábado a domingo** se usa siempre `enseguida1.mp4`.
- `ENSEGUIDAS_POST_PROGRAMA` actualizado para incluir `enseguida5`.

**`comercial4.mp4` actualizado a la Era 2001**
- El archivo `comercial4.mp4` fue reemplazado por una versión basada en la estética del año 2001.
- No hay cambios en la lógica de selección ni reproducción.

**Volumen de música de fondo ajustado al 8%**
- El volumen del `MediaPlayer` de música de fondo (`bg_music`) fue incrementado de **5% → 8%** (`0.08f`) en ambos canales.

**`enseguida2.mp4` reemplazada por enseguida de burbujas**
- El archivo `enseguida2.mp4` fue reemplazado por un nuevo clip de transición con estética de burbujas.

### Agregado

**Sistema de Tallas (`tallas.mp4`)**
- Se incorpora el segmento **Talla** a la lista de programación como un nuevo tipo de ítem (`PlayItem.Talla`).
- Cuatro variaciones: `tallas_1.mp4`, `tallas_2.mp4`, `tallas_3.mp4` y `tallas_4.mp4`.
- Las tallas se ubican **entre la enseguida post-programa y el bumper**:
  ```
  Enseguida (1/2/5) → Talla (1/2/3/4) → Bumper → Programa
  ```

**ScreenBug dinámico según la talla reproducida**
- La talla elegida determina qué drawable del screenbug se muestra durante el programa siguiente.
- Implementado mediante `TALLA_SCREENBUG_MAP` en el `companion object`.
- `currentScreenBugRes` se persiste en `SharedPreferences` para restaurar el screenbug al reanudarse.

**Assets actualizados a la Era 2001**
- `comercial1.mp4`, `comercial2.mp4`, `comercial3.mp4` → versiones Era 2001.
- `enseguida1.mp4` → versión Era 2001.
- `screenbug.webp` → versión Era 2001.

---

## [2001.2.5.0.52-beta] — 2026-05-17 · Beta Pre-Release · Era 2001

> *Pre-release de pruebas internas. Esta versión puede contener comportamientos inestables. No destinada a producción.*

### Corregido

**BUG FIX URGENTE: `VideoView` invisible durante el bloque comercial**
- Al agregar el `FadeOut` del `VideoView` en `2.4.1` (portado a beta en `2001.2.5.0.51`) nunca se colocó la lógica para restablecer el `alpha` al **comenzar** el bloque comercial.
- Como consecuencia, la enseguida pre-comercial, el comercial y el `ya_volvemos` se reproducían de forma completamente **invisible** (el `VideoView` quedaba en `alpha = 0f` al terminar el FadeOut).
- **Fix:** Se agrega `videoView.alpha = 1f` (sin animación, sin fadeIn) al inicio del bloque `withEndAction` del FadeOut en `playCommercial()`, inmediatamente antes de reproducir la enseguida pre-comercial. Esto garantiza que el bloque comercial sea visible desde el primer frame.

### Modificado

**Enseguidas post-programa: de aleatorias a basadas en horario**
- Se elimina la selección aleatoria de enseguidas post-programa (`candidates.random()`).
- La enseguida ahora se determina según la franja horaria (lunes a viernes):
  - `enseguida1` → 00:00 – 12:59
  - `enseguida2` → 13:00 – 16:29
  - `enseguida5` → 16:30 – 23:59
- De **sábado a domingo** se usa siempre `enseguida1.mp4`.
- La variable `lastEnseguidaPostProgramaRes` (usada para evitar repetición aleatoria) ya no cumple función activa; se conserva declarada para posibles usos futuros.
- `ENSEGUIDAS_POST_PROGRAMA` actualizado para incluir `enseguida5`.

**`comercial4.mp4` actualizado a la Era 2001**
- El archivo `comercial4.mp4` fue reemplazado por una versión basada en la estética y el contenido del año 2001, en línea con la Era 2001 del canal.
- No hay cambios en la lógica de selección ni reproducción.

**Selección de tallas: de aleatoria a basada en hora y día de la semana**
- Se elimina la selección aleatoria de tallas (`candidates.random()`).
- La talla ahora se determina según la franja horaria (lunes a viernes):
  - `tallas_1` → 06:00 – 12:59
  - `tallas_2` → 13:00 – 16:29
  - `tallas_3` → 16:30 – 23:59
  - `00:00 – 05:59` → **sin talla** (se salta la reproducción y se usa `screenbug.webp` directamente)
- De **sábado a domingo** se elige siempre `tallas_4`, y en ese caso también se usa `screenbug.webp`.
- La variable `lastTallaRes` (usada para evitar repetición aleatoria) fue eliminada, ya que ya no aplica.
- El `ScreenBug` resultante sigue siendo:
  - `tallas_1` → `screenbug_small` | `tallas_2` → `screenbug_medium` | `tallas_3` → `screenbug_large`
  - `tallas_4` / sin talla → `screenbug` (`screenbug.webp`)

---

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

## [2.4.2] — 2026-05-17 · Release · Era 2000

> *Versión estable de corrección de errores basada en `2.4.1`.*

### Corregido

**Alpha del `VideoView` no se restablecía al iniciar el bloque comercial**
- Al ejecutarse el `FadeOut` del `VideoView` antes del bloque comercial, el `alpha` quedaba en `0f` al terminar la animación.
- Los videos del bloque comercial (enseguida pre-comercial, comercial y `ya_volvemos`) se reproducían de forma invisible porque nunca se restableció el `alpha`.
- Se agregó `videoView.alpha = 1f` (sin animación) al inicio del `withEndAction` del `FadeOut`, asegurando que el bloque comercial completo sea visible desde el primer frame.
- Cambio aplicado en `playCommercial()` de `LiveDiscoveryKids.kt`.

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
| 2001.2.5.0           | 2026-05-18 | 🚀 Release | Bug fix VideoView invisible; tallas y enseguidas por horario; tallas_4 fin de semana; ScreenBug dinámico; assets a Era 2001; volumen al 8% |
| 2001.2.5.0.52-beta   | 2026-05-17 | 🔧 Beta    | Bug fix urgente: VideoView invisible en comercial; tallas por horario; tallas_4 en fin de semana; enseguidas por horario; comercial4 a Era 2001 |
| 2001.2.5.0.51-beta   | 2026-05-16 | 🔧 Beta    | Sistema de Tallas; ScreenBug dinámico; volumen al 8%; `enseguida2` de burbujas |
| 2001.2.5.0.50-beta   | 2026-05-15 | 🔧 Beta    | Comerciales 1-3 a Era 2001; `enseguida1.mp4` y `screenbug.webp` a Era 2001 |
| 2.4.2                | 2026-05-17 | 🚀 Release | Bug fix urgente: alpha del `VideoView` no se restablecía al iniciar el bloque comercial |
| 2.4.1                | 2026-05-16 | 🚀 Release | Bug fix posición en tiempo real; FadeOut al comercial; FadeIn al programa |
| 2000.2.4.0.42-beta   | 2026-05-14 | 🔧 Beta    | `bumper2.mp4` actualizado a 480p (antes 360p)                          |
| 2000.2.4.0.41-beta   | 2026-05-13 | 🔧 Beta    | Volumen al 5%; `enseguida4` y `ya_volvemos4` a 480p                    |
| 2000.2.4.0.40-beta   | 2026-05-12 | 🔧 Beta    | Pausa en AlertDialog de salida; volumen bg_music al 2%                 |
| 2000.2.3.1           | 2026-05-12 | 🚀 Release | Migración de textos a `strings.xml`                                    |
| 2000.2.3.0           | 2026-05-11 | 🚀 Release | Comerciales Y2K agregados; mejora de resolución en bumpers             |

---
