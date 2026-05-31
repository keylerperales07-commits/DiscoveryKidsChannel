# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
y este proyecto sigue el estándar de [Versionado Semántico](https://semver.org/lang/es/).


## [2003.3.1.0.11-beta] — Beta · Era 2003 · Última beta de la sub-rama 3.1.x

> *Última beta antes de la Release 3.1.0. Completa el sistema de cortinillas con los colores amarillo y verde, y mejora la resolución de la cortinilla verde existente.*

### Agregado

**`ya_regresa4.mp4` + `continuamos4.mp4` — Cortinilla Discovery Kids Verde**
- Cuarto par de transición comercial con la cortinilla en color verde.
- `ya_regresa4` se suma a la lista `YA_REGRESA`; `continuamos4` queda mapeado en `YA_REGRESA_CONTINUAMOS_MAP`.
- La lista de pares de transición pasa de 3 a 4 elementos.

**`enseguida4.mp4` — Cortinilla Discovery Kids Amarillo**
- Cuarta enseguida post-programa con la cortinilla en color amarillo.
- Se suma a `ENSEGUIDAS_POST_PROGRAMA`. Con 4 elementos, la selección aleatoria anti-repetición tiene siempre 3 candidatos disponibles.

### Modificado

**`enseguida2.mp4` — Cortinilla Verde actualizada a mejor resolución**
- La cortinilla verde existente fue reemplazada por una versión de mayor calidad visual.
- Sin cambios en código; solo reemplazo del archivo en `res/raw/`.

---

## [2003.3.1.0.10-beta] — Beta · Era 2003 · Inicio de la sub-rama 3.1.x

> *Primera beta de la sub-rama 3.1.x. Transición al año 2003 con nuevos assets y expansión del sistema de cortinillas comerciales.*

### Agregado

**`ya_regresa3.mp4` y `continuamos3.mp4` — cortinilla Discovery Kids Rosa**
- Se agrega un tercer par de transición comercial con la cortinilla de color rosa.
- `ya_regresa3` se suma a la lista `YA_REGRESA` para selección aleatoria anti-repetición.
- `continuamos3` queda mapeado a `ya_regresa3` en `YA_REGRESA_CONTINUAMOS_MAP`.
- La lista de pares disponibles pasa de 2 a 3.

**`enseguida3.mp4` — cortinilla Discovery Kids Azul**
- Se agrega una tercera enseguida post-programa con la cortinilla de color azul.
- Se suma a `ENSEGUIDAS_POST_PROGRAMA` para selección aleatoria anti-repetición.
- Con 3 enseguidas, la selección es verdaderamente aleatoria (siempre hay 2 candidatos).

### Modificado

**Assets actualizados a la Era 2003**
- `bumper.mp4` reemplazado por versión con footage original del año 2003.
- `comercial1.mp4` a `comercial4.mp4` actualizados a la estética del año 2003.

### Nota técnica

> ⚠️ Los videos de programas (`pro1–4.mp4`) deben estar en **resolución 480p o inferior**. Resoluciones superiores (720p+) causan que el `VideoView` use aceleración de hardware (`SurfaceView`) que renderiza en una capa separada y tapa el ScreenBug. Esto es una limitación conocida del sistema actual; se resolverá en una versión futura con `TextureView`.

---

## [2002.3.0.1] — Release · Era 2002 · Corrección de errores

> *Versión de mantenimiento. Corrige el screenbug invisible tras la eliminación del sistema de Tallas en 3.0.0.*

### Corregido

**Screenbug invisible — `fadeInBug()` con referencia eliminada**
- `fadeInBug()` referenciaba `currentScreenBugRes`, eliminado en la Beta 3.0.0.4 junto con el sistema de Tallas. El método llamaba `screenBug.setImageResource(currentScreenBugRes)` que causaba un crash silencioso, impidiendo que el screenbug apareciera después de los 20 segundos de inicio de programa.
- Fix: `setImageResource(R.drawable.screenbug)` directamente, que es el único screenbug disponible desde que se eliminaron `screenbug_small/medium/large`.
- Limpieza: constante `PREF_SCREENBUG_RES` eliminada del `companion object`.

---

## [2002.3.0.0] — Release · Era 2002 · Primera release de la rama 3.x.x · Fase 2.0

> *Primera versión estable de la rama 3.x.x. Consolida todos los cambios validados durante las betas `3.0.0.1` a `3.0.0.4`. Marca el inicio oficial de la fase 2.0 de Discovery Kids, ambientada en el período 2002–2005.*

### Agregado

**`PlayItem.StandaloneCommercial` — comerciales en la programación lineal**
- Nuevo tipo de ítem que aparece entre la enseguida post-programa y el bumper, como ocurría en la televisión real de 2002–2005.
- Es independiente del bloque publicitario que interrumpe programas: no incluye transiciones ni lógica de `breakQueue`.
- Reutiliza la lista `COMMERCIALS` y el filtro anti-repetición existentes.
- Secuencia del canal: `Enseguida → StandaloneCommercial → Bumper → Programa`.

### Eliminado

**Sistema de Tallas — eliminación completa**
- `PlayItem.Talla`, `playTalla()`, `TALLAS`, `TALLA_SCREENBUG_MAP` y `currentScreenBugRes` removidos.
- `screenbug_small`, `screenbug_medium` y `screenbug_large` eliminados. Ahora siempre se usa `screenbug.webp`.
- `tallas_1–4.mp4` y `enseguida5.mp4` eliminados del proyecto.

**Selección de enseguidas por horario — eliminada**
- La lógica horaria (lunes–viernes por franja / fin de semana) fue reemplazada por selección aleatoria con anti-repetición entre `enseguida1` y `enseguida2`.

### Modificado

**Assets actualizados a la Era 2002–2005**
- `bumper1–4.mp4`, `comercial1–4.mp4`, `enseguida1.mp4`, `enseguida2.mp4` y `screenbug.webp` actualizados a la estética del período 2002–2005.
- Logo de la app actualizado.

**Renombrado de videos de transición comercial**
- `ya_volvemos.mp4` → `continuamos1.mp4`
- `ya_volvemos2.mp4` → `continuamos2.mp4`
- `enseguida3.mp4` → `ya_regresa1.mp4`
- `enseguida4.mp4` → `ya_regresa2.mp4`
- `ENSEGUIDA_YA_VOLVEMOS_MAP` y `ENSEGUIDAS_PRE_COMERCIAL` actualizados con los nuevos nombres (`YA_REGRESA`, `YA_REGRESA_CONTINUAMOS_MAP`).

---

## [2002.3.0.0.4-beta] — Beta · Era 2002

> *Cuarta beta de la rama 3.x.x. Elimina por completo el sistema de Tallas y renombra los videos de transición comercial.*

### Eliminado

**Sistema de Tallas — eliminación completa**
- `PlayItem.Talla` removido del `sealed class PlayItem` y de la playlist.
- `playTalla()` eliminada.
- `TALLAS` y `TALLA_SCREENBUG_MAP` eliminados del `companion object`.
- Los archivos `tallas_1.mp4`, `tallas_2.mp4`, `tallas_3.mp4` y `tallas_4.mp4` eliminados de `res/raw/` (los videos ya no existen).
- `screenbug_small`, `screenbug_medium` y `screenbug_large` eliminados de `res/drawable/`. Ahora siempre se usa `screenbug.webp`.
- La secuencia del canal queda: `Enseguida → StandaloneCommercial → Bumper → Programa`.

### Modificado

**Renombrado de videos de transición**
- `ya_volvemos.mp4` → `continuamos1.mp4`
- `ya_volvemos2.mp4` → `continuamos2.mp4`
- `enseguida3.mp4` → `ya_regresa1.mp4`
- `enseguida4.mp4` → `ya_regresa2.mp4`
- `ENSEGUIDA_YA_VOLVEMOS_MAP` y `ENSEGUIDAS_PRE_COMERCIAL` actualizados con los nuevos nombres.

---

## [2002.3.0.0.3-beta] — Beta · Era 2002

> *Tercera beta de la rama 3.x.x. Simplifica la selección de enseguidas y actualiza assets a la Era 2002.*

### Modificado

**Selección de enseguidas — eliminación del horario**
- La selección de enseguidas post-programa ya no depende del horario ni del día de la semana.
- Ahora se elige aleatoriamente entre `enseguida1` y `enseguida2` con anti-repetición, igual que bumpers y comerciales.
- Se eliminó `enseguida5` de la lista y del proyecto (`res/raw/`).

**Assets actualizados a la Era 2002**
- `enseguida1.mp4` y `enseguida2.mp4` actualizados a contenido de la Era 2002.
- Logo de la app actualizado (cambio externo).

---

## [2002.3.0.0.2-beta] — Beta · Era 2002–2005

> *Segunda beta de la rama 3.x.x. Agrega comerciales independientes a la programación lineal y actualiza los assets a la Era 2002–2005.*

### Agregado

**Comerciales en la programación lineal (`PlayItem.StandaloneCommercial`)**
- Se agrega un nuevo tipo de ítem `PlayItem.StandaloneCommercial` a la secuencia de canal. Aparece entre la enseguida post-programa y la talla, como ocurría en la televisión real de la era 2002–2005.
- Es independiente del bloque publicitario que interrumpe programas (`playCommercial`): no incluye transiciones ya_volvemos ni lógica de `breakQueue`.
- Reutiliza la lista `COMMERCIALS` y el filtro anti-repetición existentes.
- La secuencia del canal queda: `Enseguida → Comercial → Bumper → Talla → Programa`.

### Modificado

**Assets actualizados a la Era 2002–2005**
- `comercial1.mp4` a `comercial4.mp4` reemplazados por versiones de la Era 2002–2005.
- `screenbug.webp` actualizado a la identidad visual del período 2002–2005.

---

## [2002.3.0.0.1-beta] — Beta · Era 2002 · Inicio de la rama 3.x.x y de la fase 2.0

> *Primera versión beta de la rama 3.x.x. Marca el inicio oficial del desarrollo de la **Gran Update 3.0.0** y de la **fase 2.0** de Discovery Kids, ambientada a partir del año 2002.*

### Contexto

Esta beta inaugura una nueva etapa del proyecto. La rama 2.x.x (Era 1998–2001) queda cerrada con la versión 2.6.0. A partir de aquí, el desarrollo avanza hacia la fase 2.0, que expandirá la arquitectura del canal para reflejar la evolución del canal en el período 2002–2005.

### Modificado

**Bumpers actualizados a la Era 2002–2005**
- Los archivos `bumper1.mp4` a `bumper4.mp4` fueron reemplazados por nuevas versiones basadas en la estética del período 2002–2005.
- El contenido de los bumpers ahora refleja la identidad visual actualizada del canal para esta nueva era.
- La lógica de selección aleatoria con anti-repetición no requiere cambios.

---

## [2001.2.6.0] — Release · Era 2001 · Última versión de la fase 1.1 y de la rama 2.x.x

> *Esta es una de las últimas versiones de la fase 1.1 (Era 1998–2001) y de la rama 2.x.x, previa a la Gran Update 3.0.0 que rediseñará la arquitectura completa del canal.*

### Corregido

**Reescritura completa del manejo de segundo plano — Pausa Universal**

Las versiones 2.5.x intentaron resolver el bug de reanudación con múltiples flags de estado. Cada fix parcial cubría un caso pero dejaba otro descubierto. La solución definitiva elimina toda esa lógica condicional:

- **`onPause()` — pausa universal**: pausa el `VideoView` y `bgPlayer` siempre, sin condiciones. La posición se toma de `lastVideoPositionMs`, actualizado cada 16ms por el tracker.
- **`onResume()` — reanudación universal**: hace `seekTo(lastVideoPositionMs)` + `start()` siempre. El `onCompletion` registrado por `playUri()` continúa la secuencia correctamente para cualquier tipo de ítem.
- **`positionTrackerRunnable` simplificado**: actualiza `lastVideoPositionMs` cada 16ms para cualquier video, sin distinguir tipo de ítem.
- **Tracker global**: se inicia en `onCreate()` y corre durante toda la vida de la Activity.

### Agregado

- **`bumper5.mp4`** incorporado a la rotación de bumpers como avance de la Gran Update 3.0.0
- **Intervalo de cortes comerciales ajustado a 9 minutos** para una programación más realista

---

## [2001.2.5.2] — Release · Era 2001 · Corrección de errores

> *Versión de mantenimiento enfocada en corregir el comportamiento de segundo plano durante un bloque comercial.*

### Corregido

**Posición incorrecta al volver de segundo plano durante un bloque comercial**

Al enviar la app a segundo plano mientras se reproducía un bloque comercial (enseguida pre-comercial, comercial o ya_volvemos) y volver a primer plano, el video reiniciaba desde el comienzo del clip en lugar de retomar desde donde se había pausado.

- **Causa raíz:** `onPause()` asignaba `commercialPausedMs` con `videoView.currentPosition` en el momento crítico en que Android ya había pausado el `VideoView`, devolviendo `0` o un valor incorrecto — el mismo bug clásico resuelto para programas en `BUG FIX 1998.2.0.1` mediante el `positionTrackerRunnable`. Al llamar luego `videoView.seekTo(0)` en `onResume()`, el clip comercial comenzaba desde el principio, rompiendo la secuencia del corte publicitario.

- **Corrección:**
  - `positionTrackerRunnable` ahora también actualiza `commercialPausedMs` cada 16 ms cuando `isInCommercialBlock` es `true`, manteniendo siempre disponible la posición correcta del clip comercial activo.
  - `playCommercial()` llama a `startPositionTracker()` (en lugar de `stopPositionTracker()`) para mantener el tracker activo durante toda la secuencia del bloque comercial.
  - `onPause()` elimina la lectura directa de `videoView.currentPosition` para el caso comercial; utiliza el valor ya actualizado por el tracker.
  - `onResume()` llama a `startPositionTracker()` después de reanudar para cubrir posibles backgrounds consecutivos dentro del mismo bloque.

---

## [2001.2.5.1] — Release · Era 2001 · Corrección de errores

> *Versión de mantenimiento enfocada en corregir el comportamiento de reanudación de sesión después de bloques comerciales.*

### Corregido

**Posición incorrecta al reanudar después de un comercial**

Se identificaron y corrigieron tres causas encadenadas que producían que al reanudar la app después de haber salido durante o después de un bloque comercial, el programa comenzara desde un punto incorrecto:

- **`saveChannelState()` guardaba la posición del comercial activo** en lugar de la posición del programa donde debía retomarse. Cuando el usuario salía durante el bloque comercial, `videoView.currentPosition` apuntaba al frame del comercial, no al punto de retoma del programa. Ahora se guarda `commercialResumeMs` cuando `isInCommercialBlock` es verdadero.

- **`breakQueue` no se persistía**. Al reanudar, `beginProgramSegment` era llamado con `isFirstPlay = false` pero con `breakQueue` vacío. Esto producía que el programa continuara sin ningún corte comercial pendiente (los breaks posteriores desaparecían) o, en versiones anteriores con `isFirstPlay = true`, que todos los breaks se recalcularan desde cero, insertando un corte comercial en una posición ya consumida.

- **Corrección**: `breakQueue` se serializa como cadena (`"ms1,ms2,ms3"`) y se persiste en `SharedPreferences` bajo la clave `break_queue`. Al restaurar, se deserializa y se asigna antes de llamar a `beginProgramSegment`, garantizando que los breaks pendientes estén disponibles sin recalcular.

---

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
| 2002.3.0.0.4-beta    | 2026-05-26 | 🔧 Beta    | Tallas eliminadas; renombrado ya_volvemos→continuamos1/2, enseguida3/4→ya_regresa1/2; screenbug S/M/L eliminados |
| 2001.2.5.2           | 2026-05-21 | 🚀 Release | Bug fix: posición incorrecta al volver de segundo plano durante un bloque comercial |
| 2001.2.5.1           | —          | 🚀 Release | Bug fix: posición incorrecta al reanudar sesión después de un comercial; persistencia de `breakQueue` |
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
