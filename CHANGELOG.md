# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
y este proyecto sigue el estándar de [Versionado Semántico](https://semver.org/lang/es/).


## [2006.4.1.0] — Release · Era Doki 1.0 · Era 2006 — 2026-06-22

> *Release estable que consolida las Preview 4.1.0.10 → 4.1.0.12: evolución visual del `CrtOverlayView` a la Era 2006, pantalla de Configuración nueva, 4 comerciales standalone actualizados, y el Screenbug conmemorativo por las 10 semanas de la app (lanzamiento `1996.1.0`).*

### Agregado

**Pantalla de Configuración (`SettingsActivity`) — nueva, diseño final**
- Lista simple estilo Android Settings, accesible desde el botón ⚙️ en el canal. Cinco opciones, cada una con su valor predeterminado indicado en la descripción:

  | Opción | Tipo | Predeterminado |
  |---|---|---|
  | Música de fondo | Switch | Activado |
  | Efecto CRT | Switch | Activado |
  | Forzar 4:3 | Switch | Desactivado |
  | Duración del Screenbug | Diálogo numérico | 20 s |
  | Intervalo entre comerciales | Diálogo Min/Max | 3–9 min |

- **Forzar 4:3** controla los `layoutParams` del `VideoView` (no del contenedor, que sigue siendo siempre 4:3): Desactivado → `match_parent`/`match_parent` (el video respeta su proporción real dentro del marco); Activado → `match_parent`/`wrap_content` (se estira, comportamiento histórico).
- **Duración del Screenbug** e **intervalo entre comerciales** reemplazan lo que antes eran constantes fijas en código (`BUG_SHOW_DELAY`, `BREAK_INTERVAL_MIN_MS`/`MAX_MS`), ahora leídas dinámicamente desde `SettingsManager`.
- `txtSettingsVersion` (footer de la pantalla) ya no es texto hardcodeado: `settingsVersionInfo()` lo completa en `onCreate()` leyendo el nombre visible de la app y el `versionName` reales desde `PackageManager`, para que nunca quede desactualizado en futuras Preview/Release.

**`comercial3.mp4` y `comercial4.mp4` — sumados a la rotación de la Era 2006**
- Junto con `comercial1`/`comercial2` (ya actualizados a la Era 2006), los 4 comerciales standalone rotan aleatoriamente sin repetir el mismo dos veces seguidas.

**Screenbug conmemorativo — 10 semanas de la app**
- Por esta semana (desde el 22 de junio de 2026), el Screenbug muestra un logo conmemorativo por las 10 semanas desde el lanzamiento de la primera versión de la app (`1996.1.0`).
- Es un reemplazo directo del archivo de imagen del Screenbug existente — no requiere `SettingsManager` ni lógica nueva, usa el mismo mecanismo de fadeIn/fadeOut ya implementado. Vuelve al logo estándar de la Era 2006 al cabo de una semana.

### Modificado

**`CrtOverlayView` — evolucionado al estándar visual de la Era 2006**
- Los televisores CRT de mediados/fines de los 2000 (Trinitron y sucesores) tenían tubos más planos, barrido más fino y fuentes de alimentación más estables que los de los 90:

  | Parámetro | Antes (Era 1999/2000) | Ahora (Era 2006) |
  |---|---|---|
  | `scanlineAlpha` | `100` | `65` |
  | `scanlineGlowAlpha` | `25` | `16` |
  | `scanlineSpacing` | `2px` | `3px` |
  | `phosphorAlpha` | `30` | `18` |
  | `vignetteAlpha` | `210` | `150` |
  | `flickerIntensity` | `0.065` | `0.035` |
  | `borderWidth` | `18dp` | `12dp` |
  | `borderAlpha` | `210` | `150` |

- El control "Brillo del CRT" (slider 0–100% en una Preview intermedia) se simplificó a **Efecto CRT** activar/desactivar: `CrtOverlayView.effectEnabled: Boolean`, reutilizando toda la lógica de escalado de alphas sin tocar los valores base de la tabla anterior.

**Pantalla de Configuración — diseño final**
- Tras una iteración intermedia con menú estilo OSD de TV CRT (verde fósforo, modos Completa/Profesional), el diseño se simplificó a una lista única estilo Android Settings: fondo gris oscuro neutro, sin secciones que ocultar, todas las opciones siempre visibles.

---



## [2006.4.1.0.12-preview] — Preview · Era Doki 1.0 — 2026-06-21

> *Preview para el 21 de junio de 2026. Rediseña por completo la pantalla de Configuración a una lista simple (estilo Android Settings), elimina el modo debug configurable y el selector Completa/Profesional, cambia el brillo del CRT por un simple activar/desactivar, y agrega tres opciones nuevas: duración del Screenbug, intervalo de comerciales y Forzar 4:3.*

### Agregado

**Duración del Screenbug — configurable**
- Nueva opción en Configuración: segundos antes de que el Screenbug aparezca al iniciar un segmento (antes `BUG_SHOW_DELAY` fija en 20 s como `const val`).
- Pasa a ser `bugShowDelayMs`, una propiedad de instancia de `LiveDiscoveryKids` leída desde `SettingsManager.getScreenbugDelaySec()` en `applySettings()`.
- Se edita desde un diálogo simple (`AlertDialog` + `EditText` numérico) al tocar el item en Configuración.

**Intervalo entre comerciales — configurable (rango Min/Max)**
- Nueva opción en Configuración: rango de minutos para el intervalo aleatorio entre cortes comerciales (antes `BREAK_INTERVAL_MIN_MS`/`BREAK_INTERVAL_MAX_MS` fijas en 3–9 min como `const val`).
- Pasan a ser `breakIntervalMinMs`/`breakIntervalMaxMs`, propiedades de instancia leídas desde `SettingsManager.getCommercialMinMinutes()`/`getCommercialMaxMinutes()` en `applySettings()`. `calcBreaks()` las usa sin cambios en su lógica interna.
- `SettingsManager.setCommercialInterval()` intercambia min/max automáticamente si se guardan invertidos, para no producir un rango inválido.
- Se edita desde un diálogo con dos campos numéricos (Mínimo / Máximo).

**Forzar 4:3 — configurable**
- Nueva opción en Configuración (on/off). Controla `AspectRatioFrameLayout.forceAspectRatio`:
  - **Activado** *(default)* — comportamiento histórico: `onMeasure()` siempre fuerza una proporción 4:3 (`width = height * 4 / 3`), sin importar el tamaño real de pantalla.
  - **Desactivado** — `onMeasure()` deja pasar las specs originales sin modificar, por lo que el contenedor usa el tamaño real de pantalla definido en `activity_main.xml` (`match_parent`/`match_parent`).
- El setter de `forceAspectRatio` llama `requestLayout()` internamente cuando el valor cambia, así que el cambio se refleja de inmediato si se modifica desde Configuración mientras el canal está visible.

### Modificado

**Pantalla de Configuración (`SettingsActivity`) — rediseño completo**
- Se reemplaza el diseño de menú OSD de TV CRT (verde fósforo, tarjetas, tipografía monoespaciada) por una lista simple estilo Android Settings / AndroidIDE: fondo gris oscuro neutro, rótulo de sección pequeño en gris-azulado, título de item en blanco, descripción en gris debajo.
- Se elimina el selector de modo **Completa**/**Profesional**: ya no hay secciones que mostrar/ocultar: todas las opciones están siempre visibles en una sola lista.
- Cada item ahora indica su valor predeterminado directamente en su descripción (ej. *"Reproduce música ambiental durante los programas (Predeterminado: Activado)"*).
- Paleta de colores simplificada en `colors.xml`: se elimina el acento verde fósforo (`dk_phosphor_green`, `dk_phosphor_green_dim`, `dk_amber`, `dk_surface`, `dk_surface_alt`) en favor de una escala de grises neutra (`dk_bg`, `dk_stroke`, `dk_text_primary`, `dk_text_secondary`).
- Se eliminan del proyecto los drawables `bg_mode_selected.xml`, `bg_mode_unselected.xml`, `bg_settings_card.xml` y `bg_settings_screen.xml`, ya sin uso.

**Brillo del CRT → Efecto CRT (activar/desactivar)**
- El control deslizante de 0–100% (`brightnessMultiplier: Float`, Preview 4.1.0.11) se reemplaza por un simple switch on/off.
- `CrtOverlayView.brightnessMultiplier` → `CrtOverlayView.effectEnabled: Boolean`. Internamente se sigue usando un factor (`1f`/`0f`) para reutilizar toda la lógica de escalado de alphas ya existente (scanlines, phosphor mask, vignette, bordes, flicker), sin modificar los valores base de la Era 2006.

### Eliminado

**Modo debug configurable**
- Se elimina la opción "Modo debug" de Configuración. El overlay de FPS/RAM/versión vuelve a mostrarse automáticamente en builds Preview, sin posibilidad de ocultarlo desde el usuario (`setupDebugInfo()` vuelve a ser incondicional, como antes de la Preview 4.1.0.11).
- Se elimina `SettingsManager.isDebugModeEnabled()`/`setDebugModeEnabled()` y `LiveDiscoveryKids.refreshDebugVisibility()`.

**Selector de modo Completa/Profesional**
- Se elimina `SettingsManager.Mode` (enum `COMPLETA`/`PROFESIONAL`) y `getSettingsMode()`/`setSettingsMode()`. La pantalla de Configuración ya no tiene secciones condicionales.

---



## [2006.4.1.0.11-preview] — Preview · Era Doki 1.0 — 2026-06-20

> *Preview para el 20 de junio de 2026. Agrega una pantalla de Configuración nueva (`SettingsActivity`) con modos Completa y Profesional, e incorpora `comercial3`/`comercial4` de la Era 2006 a la rotación de comerciales standalone.*

### Agregado

**Pantalla de Configuración (`SettingsActivity`) — nueva**
- Nueva Activity accesible desde un botón ⚙️ en la esquina superior derecha del canal (mismo patrón de autohide que los botones Prev/Next, visible 3 s tras tocar la pantalla).
- Diseño tipo menú OSD ("On-Screen Display") de un TV CRT de configuración: fondo casi negro, acento verde fósforo P1, tipografía monoespaciada en rótulos de sección — coherente con el lenguaje visual de `CrtOverlayView`.
- Dos modos de visualización, alternables desde la propia pantalla:
  - **Completa** — muestra todas las opciones disponibles (secciones "General" y "Avanzado").
  - **Profesional** *(modo por defecto)* — muestra únicamente la sección "General" (música de fondo), ocultando los controles más técnicos.
  - El modo solo afecta qué se muestra; los valores guardados son los mismos en ambos modos.
- Tres opciones configurables, persistidas vía `SettingsManager` (wrapper sobre `SharedPreferences`, key `dk_settings`):
  - **Música de fondo** (on/off) — sección General.
  - **Modo debug** (on/off) — sección Avanzada. Controla la visibilidad del overlay de FPS/RAM/versión, que antes era siempre visible en Preview.
  - **Brillo del CRT** (0–100%, control deslizante) — sección Avanzada. Escala la intensidad de `CrtOverlayView` (scanlines, phosphor mask, vignette, flicker y bordes) sin modificar los valores base definidos por la Era.

**`SettingsManager` — nuevo objeto singleton**
- Único punto de lectura/escritura de configuración; tanto `LiveDiscoveryKids` como `SettingsActivity` pasan siempre por aquí.
- Valores por defecto: música de fondo `true`, modo debug `false`, brillo CRT `1.0f` (100%), modo de pantalla `PROFESIONAL`.

**`CrtOverlayView.brightnessMultiplier` — nueva propiedad (0.0–1.0)**
- Escala todos los alphas (`scanlineAlpha`, `scanlineGlowAlpha`, `phosphorAlpha`, `vignetteAlpha`, `borderAlpha`) y la intensidad del flicker en `onDraw()`, sin tocar los valores base de la Era 2006.
- El caché de shaders de vignette/borde ahora también se invalida cuando cambia el brillo, no solo cuando cambia el tamaño de la vista.
- Se aplica desde `LiveDiscoveryKids` en `onCreate()` y se refresca en cada `onResume()`, por si el usuario volvió de `SettingsActivity` con un valor nuevo.

**`comercial3.mp4` y `comercial4.mp4` — agregados a la rotación de la Era 2006**
- Se suman a `comercial1`/`comercial2` (ya actualizados en la Preview 4.1.0.10) en la lista `COMMERCIALS` de `LiveDiscoveryKids.kt`.
- Los 4 comerciales ahora rotan aleatoriamente en los bloques `StandaloneCommercial`, sin repetir el mismo dos veces seguidas — mismo mecanismo ya usado para bumpers.

### Modificado

**`setupDebugInfo()` — ahora condicional al setting de modo debug**
- Si el modo debug está deshabilitado en Configuración, el `TextView` de debug se oculta y no se inicia el monitor de FPS/RAM.
- Se agrega `refreshDebugVisibility()`, llamada en `onResume()`, que limpia el handler antes de reevaluar el estado para evitar monitores duplicados al entrar y salir de Configuración repetidamente.

**`startBgMusic()` — ahora condicional al setting de música de fondo**
- Si la música de fondo está deshabilitada en Configuración, la función no inicia el `MediaPlayer` y registra el motivo en el log.

---



## [2006.4.1.0.10-preview] — Preview · Era Doki 1.0 — 2026-06-19

> *Preview para el 19 de junio de 2026. Evoluciona el `CrtOverlayView` al estándar visual de la era 2006, reemplaza los comerciales 1 y 2 por versiones de esa era, y documenta el Screenbug especial por las 10 semanas de la app (lanzamiento 22 de junio).*

### Modificado

**`CrtOverlayView` — evolucionado al estándar visual de la Era 2006**
- Los televisores CRT de mediados/fines de los 2000 tenían tubos más planos, barrido más fino y fuentes de alimentación más estables que los de los 90, por lo que el overlay se vuelve más limpio sin perder identidad retro.
- `scanlineAlpha`: `100` → `65`.
- `scanlineGlowAlpha`: `25` → `16`.
- `scanlineSpacing`: `2px` → `3px` (barrido menos denso).
- `phosphorAlpha`: `30` → `18` (máscara RGB más discreta).
- `vignetteAlpha`: `210` → `150` (menor oscurecimiento de bordes).
- `flickerIntensity`: `0.065` → `0.035` (parpadeo casi imperceptible).
- `borderWidth`: `18dp` → `12dp` (marco casi imperceptible, propio de TV de pantalla plana CRT).
- `borderAlpha`: `210` → `150` (borde más tenue).
- `scanlineScrollSpeed` se mantiene sin cambios (`0.5f`).

**`comercial1.mp4` y `comercial2.mp4` — reemplazados por versiones de la Era 2006**
- Ambos comerciales fueron actualizados al estilo publicitario de la Era 2006, continuando el reemplazo iniciado en `comercial3` y `comercial4` con la Release 2005.4.0.1.

### Planeado

**Screenbug especial — 10 semanas de la app (sin código todavía)**
- Se documenta la intención de mostrar un Screenbug conmemorativo por las 10 semanas desde el lanzamiento de la primera versión de la app (`1996.1.0`).
- Coincide con el lanzamiento de la versión menor `2006.4.1.0`, programado para el **22 de junio de 2026**, fecha en la que se cumplen exactamente las 10 semanas.
- Pendiente de implementación: lógica de activación, asset del nuevo screenbug y ventana de visibilidad.

---



## [2005.4.0.1] — Release · Era Doki 1.0 · Sub-rama 4.0.x — 2026-06-17

> *Release de corrección. Corrige la asignación de ya_regresa/continuamos para que sea determinística por programa en lugar de aleatoria.*

### Corregido

**`ya_regresa` / `continuamos` — asignación determinística por programa**
- El sistema anterior usaba un "shuffled pool" de 4 slots que se consumía en orden a lo largo de todos los programas, causando que el par ya_regresa/continuamos mostrado no correspondiera al programa en reproducción.
- Se elimina el pool (`yaRegresaPool`, `yaRegresaPoolIndex`) y se reemplaza por indexación directa: `ENSEGUIDAS_PRE_COMERCIAL[currentProgramIndex % size]`.
- Asignación resultante: programa 1 → ya_regresa1/continuamos1 · programa 2 → ya_regresa2/continuamos2 · programa 3 → ya_regresa3/continuamos3 · programa 4 → ya_regresa4/continuamos4.
- Esta asignación es fija y no varía entre sesiones ni ciclos.

**`calcBreaks()` — zona de protección de 3 minutos al final del programa**
- Se agrega la constante `BREAK_CUTOFF_MS = 3 * 60 * 1_000L` (3 min).
- `calcBreaks()` ahora compara cada posición de corte contra `durationMs - BREAK_CUTOFF_MS` en lugar de `durationMs`.
- Ningún corte comercial se programa dentro de los últimos 3 minutos del programa, garantizando que el final nunca sea interrumpido por un bloque publicitario.

---



## [2005.4.0.0] — Release · Era Doki 1.0 · Sub-rama 4.0.x — 2026-06-16

> *Primera release estable de la Era Doki 1.0. Consolida todos los cambios de las betas 4.0.0.1 → 4.0.0.4: 8 bumpers Era Doki, logo y comerciales fase 3.0, bg_music reducido, enseguidas actualizadas, nombre app → Discovery Kids LA, ya_regresa1–4 y continuamos1–4 Era Doki, intervalo de comerciales aleatorio 3–9 min.*

### Agregado

**8 bumpers de la Era Doki 1.0 — `bumper1.mp4` a `bumper8.mp4`**
- Se incorporan `bumper1.mp4` a `bumper8.mp4` basados en material original de Discovery Kids de la Era Doki 1.0, período 2005–2009.
- Reemplazan la identidad visual de la Era 2004 por la estética característica de Doki como mascota del canal.

### Eliminado

**`enseguida3.mp4` y `enseguida4.mp4` — eliminados**
- Los clips de enseguida 3 y 4, correspondientes a eras anteriores, fueron eliminados del proyecto.
- `ENSEGUIDAS_POST_PROGRAMA` ahora contiene únicamente los clips 1 y 2.

### Modificado

**Logo actualizado al estilo de la fase 3.0 (2005–2009)**
- El logo del canal fue reemplazado por la versión correspondiente a la fase 3.0 del período 2005–2009.

**4 comerciales reemplazados por versiones de la fase 3.0 (2005)**
- `comercial1.mp4` a `comercial4.mp4` reemplazados por versiones del estilo publicitario de la fase 3.0.

**`bg_music` — duración reducida de 1:00:54 h a 10:02 min**
- El archivo de música de fondo fue reemplazado por una versión más corta (10:02 min).

**`enseguida1.mp4` y `enseguida2.mp4` — reemplazados por versiones Era Doki**
- Reemplazados por versiones basadas en la estética de la Era Doki 1.0 (2005–2009).

**Nombre de la aplicación actualizado a Discovery Kids LA**
- Build type release: `Discovery Kids` → `Discovery Kids LA`
- Build type beta/debug: `Discovery Kids Beta` → `Discovery Kids LA Beta`

**`ya_regresa1.mp4` a `ya_regresa4.mp4` — reemplazados por versiones Era Doki**
- Los 4 clips pre-comercial reemplazados por versiones de la Era Doki 1.0.

**`continuamos1.mp4` a `continuamos4.mp4` — reemplazados por versiones Era Doki**
- Los 4 clips post-comercial reemplazados por versiones de la Era Doki 1.0.

### Corregido

**Intervalo de comerciales — aleatorio entre 3 y 9 minutos**
- `BREAK_INTERVAL_MS` reemplazado por `BREAK_INTERVAL_MIN_MS` (3 min) y `BREAK_INTERVAL_MAX_MS` (9 min).
- `calcBreaks()` genera cada intervalo de forma aleatoria e independiente entre los dos límites.

---


## [2005.3.4.2] — Release · Era 2004 → Era Doki · Sub-rama 3.4.x — 2026-06-16

> *Micro-release de corrección. Corrige el intervalo de comerciales de fijo 9 min a aleatorio 3–9 min, y reemplaza bumper6 por el nuevo bumper de aviso de la Era Doki.*

### Corregido

**Intervalo de comerciales — corregido de fijo 9 min a aleatorio entre 3 y 9 min**
- `BREAK_INTERVAL_MS` (constante fija de 9 min) fue reemplazado por un rango dinámico: `BREAK_INTERVAL_MIN_MS` (3 min) y `BREAK_INTERVAL_MAX_MS` (9 min).
- `calcBreaks()` ahora genera cada intervalo de forma aleatoria e independiente usando `Math.random()` entre los dos límites.
- El primer corte puede ocurrir en cualquier punto entre los 3 y los 9 minutos del programa; cada corte subsiguiente elige su propio intervalo aleatoriamente.
- Esto corrige el comportamiento anterior donde los comerciales aparecían siempre exactamente a los 9, 18, 27 min, etc.

### Modificado

**`bumper6.mp4` — reemplazado por bumper de aviso de la Actualización La Era Doki**
- El archivo `bumper6.mp4` fue reemplazado por un nuevo clip que anuncia la próxima actualización al nuevo Discovery Kids de la Era Doki.
- Funciona como puente visual entre la Era 2004 y la próxima Era Doki 1.0.

---


## [2005.4.0.0.4] — Beta · Era Doki 1.0 · Sub-rama 4.0.x — 2026-06-16

> *Cuarta beta de la Era Doki 1.0. Reemplaza los 4 clips ya_regresa y los 4 clips continuamos por versiones de la Era Doki. Incluye el intervalo de comerciales aleatorio 3–9 min portado desde la Release 2005.3.4.2.*

### Modificado

**`ya_regresa1.mp4` a `ya_regresa4.mp4` — reemplazados por versiones Era Doki**
- Los 4 clips pre-comercial (`ya_regresa1`, `ya_regresa2`, `ya_regresa3`, `ya_regresa4`) fueron reemplazados por versiones basadas en la estética de la Era Doki 1.0 (2005–2009).
- El sistema de transición pre-comercial queda completamente actualizado a la identidad visual de la era actual.

**`continuamos1.mp4` a `continuamos4.mp4` — reemplazados por versiones Era Doki**
- Los 4 clips post-comercial (`continuamos1`, `continuamos2`, `continuamos3`, `continuamos4`) fueron reemplazados por versiones basadas en la estética de la Era Doki 1.0 (2005–2009).
- El mapeo `ya_regresa → continuamos` se mantiene intacto; solo se actualizó el contenido visual de los clips.

**Intervalo de comerciales — corregido de fijo 9 min a aleatorio entre 3 y 9 min**
- `BREAK_INTERVAL_MS` (constante fija de 9 min) reemplazado por `BREAK_INTERVAL_MIN_MS` (3 min) y `BREAK_INTERVAL_MAX_MS` (9 min).
- `calcBreaks()` ahora genera cada intervalo de forma aleatoria e independiente entre los dos límites.

---


## [2005.4.0.0.3] — Beta · Era Doki 1.0 · Sub-rama 4.0.x — 2026-06-15

> *Tercera beta de la Era Doki 1.0. Actualiza el sistema de enseguidas a la Era Doki, cambia el nombre de la app a Discovery Kids LA y ajusta el FadeIn experimentalmente a 1 s.*

### Eliminado

**`enseguida3.mp4` y `enseguida4.mp4` — eliminados**
- Los clips de enseguida 3 y 4, correspondientes a eras anteriores, fueron eliminados del proyecto.
- `ENSEGUIDAS_POST_PROGRAMA` ahora contiene únicamente los clips 1 y 2.

### Modificado

**`enseguida1.mp4` y `enseguida2.mp4` — reemplazados por versiones Era Doki**
- Los archivos `enseguida1.mp4` y `enseguida2.mp4` fueron reemplazados por versiones basadas en la estética de la Era Doki 1.0 (2005–2009).
- El sistema de enseguidas queda reducido a 2 clips, ambos coherentes con la identidad visual de la era actual.

**Nombre de la aplicación actualizado a Discovery Kids LA**
- Build type release: `Discovery Kids` → `Discovery Kids LA`
- Build type beta/debug: `Discovery Kids Beta` → `Discovery Kids LA Beta`
- Cambio aplicado en `app_name` / `app_name_beta` en `res/values/strings.xml`. Sin cambios en `applicationId`.

**`TRANSITION_FADE_IN_MS` — ajustado experimentalmente a 1 segundo**
- Ajuste experimental del FadeIn de transición para evaluar el impacto visual en el ritmo de las transiciones de la Era Doki.
- ⚠️ Este cambio no es definitivo y puede revertirse o ajustarse en versiones futuras.

---


## [2005.4.0.0.2] — Beta · Era Doki 1.0 · Sub-rama 4.0.x — 2026-06-14

> *Segunda beta de la Era Doki 1.0. Reemplaza los 4 comerciales por versiones de la fase 3.0 (2005), reduce la duración de `bg_music` y actualiza el logo al estilo de la fase 3.0.*

### Modificado

**4 comerciales reemplazados por versiones de la fase 3.0 (2005)**
- Los archivos `comercial1.mp4`, `comercial2.mp4`, `comercial3.mp4` y `comercial4.mp4` fueron reemplazados por versiones basadas en el estilo publicitario de la fase 3.0 (período 2005).
- El contenido comercial ahora es coherente con la estética de la Era Doki 1.0.

**`bg_music` — duración reducida de 1:00:54 h a 10:02 min**
- El archivo de música de fondo fue reemplazado por una versión más corta (10:02 min), eliminando el loop de una hora que se usaba desde eras anteriores.
- No hay cambios en el volumen ni en la lógica de reproducción.

**Logo actualizado al estilo de la fase 3.0 (2005–2009)**
- El logo del canal fue reemplazado por la versión correspondiente a la fase 3.0 del período 2005–2009.
- El nuevo logo es coherente con la identidad visual de la Era Doki 1.0.

---


## [2005.4.0.0.1] — Beta · Era Doki 1.0 · Sub-rama 4.0.x — 2026-06-13

> *Primera beta de la Era Doki 1.0 (2005–2009). Agrega 8 bumpers originales de la era.*

### Agregado

**8 bumpers de la Era Doki 1.0 (2005–2009)**
- Se incorporan `bumper1.mp4` a `bumper8.mp4` basados en material original de Discovery Kids de la Era Doki 1.0, período 2005–2009.
- Estos bumpers reemplazan la identidad visual de la Era 2004 por la estética característica de Doki como mascota del canal.

---


## [2005.3.4.1] — Release · Era 2004 · Sub-rama 3.4.x — 2026-06-13

> *Micro-release de transición hacia la Era Doki. Agrega bumper6.mp4 como aviso de la próxima actualización.*

### Agregado

**`bumper6.mp4` — aviso de proximidad de la Era Doki**
- Se agrega `bumper6.mp4`: clip de aviso de que se acerca la actualización de la Era Doki al canal.
- Funciona como puente visual entre la Era 2004 y la próxima Era Doki 1.0.

---


## [2004.3.4.0] — Release · Era 2004 · Sub-rama 3.4.x — 2026-06-12

> *Release 3.4.0. Corrige dos bugs críticos de segundo plano y agrega navegación por bloque completo en los botones Prev/Next.*

### Corregido

**ScreenBug — reinicio del counter de 20 s al volver de segundo plano**
- `scheduleSegmentLogic()` usaba `BUG_SHOW_DELAY` (20 s) fijo desde el momento de reanudación, ignorando cuántos ms del segmento ya habían transcurrido antes de pausar. Al volver de segundo plano, el screenbug siempre esperaba 20 s desde cero aunque ya debiera estar visible.
- Nueva variable `currentSegmentStartMs` guarda la posición del programa al inicio de cada segmento.
- `scheduleSegmentLogic()` calcula `elapsed = segmentStartMs - currentSegmentStartMs` y ajusta: `bugShowDelay = max(0, BUG_SHOW_DELAY - elapsed)`.
- Si `elapsed >= BUG_SHOW_DELAY`, el screenbug aparece inmediatamente con `setBugAlpha(1f)` sin animación redundante.
- El delay de `fadeOutBug()` se ajusta igualmente para mantener coherencia temporal.

**VideoView — FadeIn redundante al volver de segundo plano**
- `beginProgramSegment()` podía recibir una animación de alpha a medias (FadeOut interrumpido al ir al fondo) que dejaba el `VideoView` en un estado de alpha indefinido.
- Se agrega `videoView.animate().cancel()` y `videoView.alpha = 0f` antes de `setVideoURI()` para garantizar estado limpio antes de cada reanudación.

### Agregado

**Botones Prev / Next — navegación por bloque completo (Enseguida → StandaloneCommercial → Bumper → Programa)**
- Antes, los botones saltaban directamente al `PlayItem.Program` destino, omitiendo la Enseguida, el StandaloneCommercial y el Bumper que preceden a cada programa en el playlist.
- `goToAdjacentProgram()` ahora calcula `blockStartIdx`: busca el `PlayItem.Enseguida` en `programIdx - 3` y, si existe, posiciona `playlistIndex` allí para que `advance()` arranque el bloque completo.
- Si no hay Enseguida antes del programa (e.g. primer ítem del playlist), el fallback cae al Bumper o al propio programa.

---


## [2004.3.4.0.41] — Beta · Era 2004 · Sub-rama 3.4.x — 2026-06-11

> *Beta de corrección de screenbug. Corrige el fadeIn redundante del screenbug al reanudar desde segundo plano.*

### Corregido

**Screenbug — fadeIn redundante al reanudar desde segundo plano**
- `scheduleSegmentLogic()` siempre programaba `fadeInBug()` con `BUG_SHOW_DELAY` (20 s) fijo desde el momento de reanudación, sin considerar cuántos ms del segmento ya habían transcurrido antes de pausar.
- Nueva variable `currentSegmentStartMs` guarda el inicio del segmento activo en tiempo de programa. `scheduleSegmentLogic` calcula `elapsed = pausedPositionMs - currentSegmentStartMs` y ajusta el delay: `bugShowDelay = max(0, BUG_SHOW_DELAY - elapsed)`.
- Si `elapsed >= BUG_SHOW_DELAY`, el screenbug aparece inmediatamente con `setBugAlpha(1f)` sin animación.
- El delay de `fadeOutBug()` también se ajusta con `elapsed` para mantener coherencia.

*Mismos cambios de compilación que la 3.4.0.40 — ver entrada correspondiente.*

---


## [2004.3.3.2] — Release · Era 2004 · Sub-rama 3.3.x — 2026-06-11

> *Bug fix del screenbug al reanudar desde segundo plano.*

### Corregido

**Screenbug — fadeIn redundante al reanudar desde segundo plano**
- Al volver de segundo plano, `scheduleSegmentLogic()` siempre programaba `fadeInBug()` con `BUG_SHOW_DELAY` (20 s) fijo desde el momento de reanudación, ignorando cuántos ms del segmento ya habían transcurrido. Si el screenbug ya debía estar visible, aparecía 20 s tarde o hacía un fadeIn innecesario.
- Nueva variable `currentSegmentStartMs` guarda el inicio del segmento activo en tiempo de programa. `scheduleSegmentLogic` calcula `elapsed = pausedPositionMs - currentSegmentStartMs` y ajusta el delay real: `bugShowDelay = max(0, BUG_SHOW_DELAY - elapsed)`.
- Si `elapsed >= BUG_SHOW_DELAY`, el screenbug aparece inmediatamente con `setBugAlpha(1f)` sin animación redundante.
- El delay de `fadeOutBug()` también se ajusta con `elapsed` para mantener coherencia temporal.

---


## [2004.3.4.0.40] — Beta · Era 2004 · Sub-rama 3.4.x — 2026-06-10

> *Primera beta de la sub-rama 3.4.x. Habilita R8 en el build type debug para paridad de compilación con Release, manteniendo los logs activos. Incluye todos los cambios de la Release 3.3.1.*

### Agregado

**R8 habilitado en el build type debug**
- El bloque `debug` en `build.gradle` ahora tiene `minifyEnabled true` y `shrinkResources true`, idéntico al bloque `release`.
- Se agrega `proguard-debug.pro` como tercer archivo de reglas en el tipo debug. Esta regla hace `-keep class android.util.Log { *; }` y `-keep class com.keyler.discoverykidschannel.** { *; }`, preservando todos los llamados a `Log.d/i/w/e` y los nombres de clase propios del proyecto en stack traces.
- `applicationIdSuffix ".beta"` y `versionNameSuffix ".beta"` permiten instalar el APK de beta junto al de release en el mismo dispositivo sin conflictos.
- `signingConfig signingConfigs.release` aplicado al build debug para que el APK beta pueda instalarse sin necesidad de una clave de debug separada.

### Corregido / Modificado

*Mismos cambios que la Release 2004.3.3.1 — ver entrada correspondiente.*

---


## [2004.3.3.1] — Release · Era 2004 · Sub-rama 3.3.x — 2026-06-10

> *Bug fix de la Era 2004. Corrige el bug de pausa/reanudación presente desde la 2.4.0, el asset `comercial1.mp4` y cambia el intervalo de cortes comerciales a aleatorio entre 3 y 9 minutos.*

### Corregido

**onPause() / onResume() — estrategia diferenciada por tipo de ítem (bug desde 2.4.0)**

Diagnóstico de por qué todas las versiones anteriores fallaban:
- `cancelAllTasks()` en `onPause()` destruía los listeners configurados por `playUriWithTransition()`. Al volver, el video reanudaba pero al terminar el clip el canal se congelaba porque no había `onCompletionListener` activo.
- `seekTo()` directo en `onResume()` fallaba silenciosamente cuando Android liberó el surface del `VideoView` en segundo plano.
- El tracker corría desde `onCreate()` antes de que hubiera video, sobreescribiendo posiciones válidas con 0.

Nueva estrategia:
- **PROGRAMA** → `beginProgramSegment(uri, pausedPositionMs, isFirstPlay=false)`. El `seekTo` ocurre dentro de `onPrepared`, cuando el MediaPlayer está garantizadamente listo.
- **NO-PROGRAMA** (bumper, enseguida, comercial, ya_regresa, continuamos) → `advance()` reinicia el ítem desde el principio. Los listeners se reconfiguran desde cero.
- **COMERCIAL EN CURSO** → retoma el programa en `commercialResumeMs` (salta el comercial).
- Se eliminaron `wasPlayingBeforePause`, `lastVideoPositionMs` y el alias. Nueva variable `pausedByLifecycle`.

**`comercial1.mp4` — clip de ya_regresa embebido al inicio**
- El archivo presentaba un defecto de edición: los primeros segundos contenían imágenes y audio de un clip de `ya_regresa`. Asset reemplazado por el usuario.

### Modificado

**Intervalo de cortes comerciales — de fijo a aleatorio entre 3 y 9 minutos**
- `BREAK_INTERVAL_MS` reemplazada por `BREAK_INTERVAL_MIN_MS = 3 min` y `BREAK_INTERVAL_MAX_MS = 9 min`.
- `calcBreaks()` elige un intervalo aleatorio para cada corte usando `Math.random()`.

---


## [2004.3.3.0] — Release · Era 2004 · Sub-rama 3.3.x — 2026-06-08

> *Primera release de la Era 2004. Unifica todos los FadeOut de cambio de video a 500 ms, eliminando las constantes diferenciadas por tipo de clip de la 3.2.x. Incorpora tres reemplazos de assets: `bumper2.mp4` con material original, `enseguida1.mp4` actualizado por cambios de parrilla, y el par `ya_regresa4`/`continuamos4` corregido por un defecto de edición.*

### Modificado

**FadeOut unificado a 500 ms para todos los clips**
- `TRANSITION_FADE_OUT_MS` establecido en `500L`. Todos los cambios de video del canal (enseguida, bumper, comercial standalone, ya_regresa, comercial del bloque, continuamos) usan ahora la misma duración de FadeOut de salida.
- Constantes `ENSEGUIDA_FADE_OUT_MS`, `BUMPER_FADE_OUT_MS`, `YA_REGRESA_FADE_OUT_MS` y `CONTINUAMOS_FADE_OUT_MS` eliminadas del `companion object`.
- Todos los callers de `playUriWithTransition()` usan el valor por defecto; el parámetro `fadeOutMs` se conserva en la firma para posibles ajustes futuros.

### Assets reemplazados

**`bumper2.mp4` reemplazado por material original Era 2004**
- El archivo `bumper2.mp4` fue reemplazado por una versión con material original correspondiente a la Era 2004.
- No hay cambios en la lógica de reproducción.

**`enseguida1.mp4` actualizado — programa descontinuado en 2004**
- El archivo `enseguida1.mp4` fue reemplazado. El avance que contenía anunciaba un programa que dejó de transmitirse en Discovery Kids Latin America en 2004, resultando anacrónico dentro de la era simulada.
- La nueva versión es coherente con la parrilla de la Era 2004.

**`ya_regresa4.mp4` y `continuamos4.mp4` reemplazados — defecto de edición corregido**
- La versión anterior de `ya_regresa4.mp4` presentaba un defecto de edición: el inicio del clip estaba mezclado con audio e imagen del programa de origen.
- Ambos archivos del par (`ya_regresa4` / `continuamos4`) fueron reemplazados por versiones limpias sin artefactos.

---


## [2003.3.2.0] — Release · Era 2003 · Sub-rama 3.2.x — 2026-06-05

> *Consolida las betas `3.2.0.20`, `3.2.0.21` y `3.2.0.22`. Introduce un sistema de transiciones profesionales aplicado uniformemente a toda la secuencia de canal, corrige el timing del FadeOut para que se dispare antes del fin del video, implementa la asignación de `ya_regresa` por programa mediante shuffled pool sin repetición, y establece duraciones de FadeOut diferenciadas por tipo de clip.*

### Agregado

**Sistema de transiciones profesionales — `playUriWithTransition()` (Beta 3.2.0.20)**
- Nuevo helper `playUriWithTransition()` que centraliza la lógica de transición para todos los cambios de video del canal.
- Ejecuta un **FadeOut de 2 segundos** sobre el `VideoView` antes de cada cambio, seguido de un **FadeIn de 1 segundo** al arrancar el nuevo clip.
- Se aplica de forma uniforme a **todos** los eventos de cambio de video: enseguida post-programa, bumper, comercial standalone, `ya_regresa`, comercial del bloque publicitario, `continuamos` y retoma del programa tras el bloque comercial.
- `playUri()` se conserva sin cambios para usos internos que no requieren transición.
- Constantes `TRANSITION_FADE_OUT_MS = 2_000L` y `TRANSITION_FADE_IN_MS = 1_000L` agregadas al `companion object`.

**Sistema de ya_regresa por programa — Shuffled Pool (Beta 3.2.0.21)**
- Reemplaza la selección simple de anti-repetición por un mecanismo de **shuffled pool** de 4 slots.
- Antes de cada ciclo de 4 programas, `ENSEGUIDAS_PRE_COMERCIAL` se mezcla aleatoriamente y cada programa consume un slot distinto. Ningún `ya_regresa` se repite dentro del mismo ciclo.
- Al agotarse los 4 slots el pool se regenera con un nuevo shuffle, garantizando variedad entre ciclos consecutivos.
- Variables añadidas: `yaRegresaPool: MutableList<Int>` e `yaRegresaPoolIndex: Int`.

### Corregido

**BUG FIX: FadeOut de transición disparado al final del video en lugar de 2 segundos antes (Beta 3.2.0.21)**
- En la Beta 3.2.0.20 el FadeOut se ejecutaba en `onCompletion`, cuando el video ya había terminado, produciendo un corte visual abrupto.
- **Fix:** `playUriWithTransition()` schedula el FadeOut mediante `post(duration - fadeOutMs)` desde `onPrepared`. La animación comienza exactamente `fadeOutMs` ms antes del fin real del clip.
- `onCompletionListener` se conserva como fallback para clips más cortos que `fadeOutMs`. Una guardia `transitionCompleted` evita doble ejecución de `onComplete()`.

**Distribución de cortes comerciales — intervalo fijo de 9 minutos (Beta 3.2.0.22)**
- La versión anterior de `calcBreaks()` distribuía los breaks de forma equidistante (`durationMs / (numBreaks + 1)`), lo que podía adelantar el primer corte a menos de 9 minutos.
- **Fix:** `calcBreaks()` ahora coloca breaks exactamente en `[9 min, 18 min, 27 min, ...]` mediante un `while (breakPos < durationMs)` con incremento de `BREAK_INTERVAL_MS`. El primer corte ocurre **siempre a los 9 minutos exactos**.

### Modificado

**FadeIn del programa unificado a 1 segundo (Beta 3.2.0.20)**
- El FadeIn de `beginProgramSegment` fue actualizado de 500 ms → 1 000 ms (`TRANSITION_FADE_IN_MS`), alineándolo con el estándar de todos los demás clips.

**FadeOut del bloque comercial actualizado de 500 ms a 2 segundos (Beta 3.2.0.20)**
- El FadeOut de 500 ms de `playCommercial()` introducido en la versión 2.4.1 fue reemplazado por `TRANSITION_FADE_OUT_MS` (2 s).

**FadeOut diferenciado por tipo de clip (Beta 3.2.0.22)**
- `playUriWithTransition()` acepta el parámetro `fadeOutMs: Long = TRANSITION_FADE_OUT_MS` para configurar la duración del FadeOut por tipo de clip.
- Nuevas constantes:
  - `ENSEGUIDA_FADE_OUT_MS = 1_000L`
  - `BUMPER_FADE_OUT_MS = 700L`
  - `YA_REGRESA_FADE_OUT_MS = 500L`
  - `CONTINUAMOS_FADE_OUT_MS = 500L`

| Evento | FadeOut salida | FadeIn entrada |
|--------|---------------|----------------|
| Enseguida post-programa | 1 000 ms | 1 000 ms |
| Bumper | 700 ms | 1 000 ms |
| ya_regresa (pre-comercial) | 500 ms | 1 000 ms |
| continuamos (post-comercial) | 500 ms | 1 000 ms |
| Comercial standalone | 2 000 ms | 1 000 ms |
| Comercial del bloque publicitario | 2 000 ms | 1 000 ms |

---


## [2003.3.2.0.22-beta] — Beta · Era 2003 · Sub-rama 3.2.x

> *Tercera beta de la sub-rama 3.2.x. Corrige la distribución de cortes comerciales para que el primero ocurra exactamente a los 9 minutos, e introduce duraciones de FadeOut diferenciadas por tipo de clip.*

### Corregido

**Distribución de cortes comerciales — intervalo fijo de 9 minutos**
- La versión anterior de `calcBreaks()` distribuía los breaks de forma **equidistante** (`durationMs / (numBreaks + 1)`), lo que podía adelantar el primer corte a menos de 9 minutos dependiendo de la duración del programa.
- **Fix:** `calcBreaks()` ahora coloca breaks exactamente en `[9 min, 18 min, 27 min, ...]` mediante un `while (breakPos < durationMs)` con incremento de `BREAK_INTERVAL_MS` por iteración. El primer corte ocurre **siempre a los 9 minutos exactos**; cada corte subsiguiente, cada 9 minutos adicionales.

### Modificado

**FadeOut diferenciado por tipo de clip**
- `playUriWithTransition()` ahora acepta el parámetro `fadeOutMs: Long = TRANSITION_FADE_OUT_MS` para configurar la duración del FadeOut de salida por tipo de clip sin alterar la constante global.
- Nuevas constantes en `companion object`:
  - `ENSEGUIDA_FADE_OUT_MS = 1_000L` — transición más corta para mantener el ritmo entre programas.
  - `BUMPER_FADE_OUT_MS = 700L` — transición ágil para clips de identidad de canal.
  - `YA_REGRESA_FADE_OUT_MS = 500L` — salida rápida del ya_regresa hacia el comercial.
  - `CONTINUAMOS_FADE_OUT_MS = 500L` — salida rápida del continuamos al retomar el programa.
- `playEnseguida()` pasa `fadeOutMs = ENSEGUIDA_FADE_OUT_MS` (1 s).
- `playBumper()` pasa `fadeOutMs = BUMPER_FADE_OUT_MS` (700 ms).
- `playCommercial()` pasa `fadeOutMs = YA_REGRESA_FADE_OUT_MS` (500 ms) al lanzar el comercial, y `fadeOutMs = CONTINUAMOS_FADE_OUT_MS` (500 ms) al lanzar el continuamos.
- `playStandaloneCommercial` y el comercial del bloque publicitario continúan usando el valor por defecto de 2 s.
- El FadeOut del clip **entrante** (schedulado en `onPrepared`) también usa `fadeOutMs` para mantener simetría de duración en ambos extremos del clip.

---


## [2003.3.2.0.21-beta] — Beta · Era 2003 · Sub-rama 3.2.x

> *Segunda beta de la sub-rama 3.2.x. Corrige el timing del FadeOut de transición e introduce un sistema de selección de ya_regresa por programa basado en shuffled pool, garantizando variedad sin repetición en cada ciclo de 4 programas.*

### Corregido

**BUG FIX: FadeOut de transición disparado al final del video en lugar de 2 segundos antes**
- En la Beta 3.2.0.20 el FadeOut se ejecutaba en `onCompletion`, es decir, cuando el video **ya había terminado**. El resultado era un corte visual abrupto visible entre clips.
- **Fix:** `playUriWithTransition()` ahora schedula el FadeOut mediante `post(duration - TRANSITION_FADE_OUT_MS)` justo cuando el video es preparado (`onPrepared`). La animación de 2 segundos comienza exactamente 2 segundos antes del fin real del clip.
- `onCompletionListener` se conserva como **fallback** para clips más cortos que `TRANSITION_FADE_OUT_MS`. Una guardia `transitionCompleted` evita doble ejecución de `onComplete()`.

### Agregado

**Sistema de ya_regresa por programa — Shuffled Pool (Beta 3.2.0.21)**
- Reemplaza la selección simple de anti-repetición por un mecanismo de **shuffled pool** de 4 slots.
- Antes de cada ciclo de 4 programas se mezcla aleatoriamente la lista `ENSEGUIDAS_PRE_COMERCIAL` y se asigna un `ya_regresa` distinto a cada programa. Ningún slot se repite dentro del mismo ciclo.
- Al agotarse los 4 slots (`yaRegresaPoolIndex >= 4`) el pool se regenera con un nuevo shuffle, garantizando variedad entre ciclos consecutivos.
- Variables añadidas: `yaRegresaPool: MutableList<Int>` y `yaRegresaPoolIndex: Int`.
- `lastEnseguidaPreComercialRes` se conserva por compatibilidad con `saveChannelState`.

---


## [2003.3.2.0.20-beta] — Beta · Era 2003 · Sub-rama 3.2.x

> *Primera beta de la sub-rama 3.2.x. Eleva la calidad visual del canal con un sistema de transiciones profesionales aplicado de forma uniforme a cada cambio de video en toda la secuencia de programación.*

### Agregado

**Sistema de transiciones profesionales — `playUriWithTransition()`**
- Nuevo helper `playUriWithTransition()` que centraliza la lógica de transición para todos los cambios de video del canal.
- Ejecuta un **FadeOut de 2 segundos** sobre el `VideoView` antes de cada cambio, seguido de un **FadeIn de 1 segundo** al arrancar el nuevo clip.
- Se aplica de forma uniforme a **todos** los eventos de cambio de video:
  - Inicio de enseguida post-programa (`playEnseguida`)
  - Inicio de bumper (`playBumper`)
  - Inicio de comercial standalone (`playStandaloneCommercial`)
  - Inicio de `ya_regresa` (pre-comercial) en `playCommercial`
  - Inicio del comercial dentro del bloque publicitario
  - Inicio del `continuamos` (post-comercial)
  - Retoma del programa tras el bloque comercial (`beginProgramSegment`, FadeIn 1 s)
- `playUri()` se conserva sin cambios para usos internos que no requieren transición.
- Las constantes `TRANSITION_FADE_OUT_MS = 2000L` y `TRANSITION_FADE_IN_MS = 1000L` se agregan al `companion object` para control centralizado de duraciones.

### Modificado

**FadeIn del programa unificado a 1 segundo**
- El FadeIn de `beginProgramSegment` (arranque de programa y retoma tras comercial) fue actualizado de **500 ms → 1000 ms** (`TRANSITION_FADE_IN_MS`), alineándolo con el estándar de todos los demás clips del canal.

**FadeOut del bloque comercial actualizado de 500 ms a 2 segundos**
- El FadeOut de 500 ms de `playCommercial()` introducido en la versión 2.4.1 fue reemplazado por el FadeOut estándar de `TRANSITION_FADE_OUT_MS` (2 s), consistente con el resto de las transiciones.
- La corrección del BUG FIX 2001.2.5.0.52 (`videoView.alpha = 1f`) queda implícita en `playUriWithTransition()`, que siempre establece `alpha = 0f` antes del FadeIn.

---

## [2003.3.1.0] — Release · Era 2003 · Fase 2 — Parte 2 · Era Arcoiris completa

> *Segunda y última parte de la Gran Update de la Era Arcoiris. Consolida todos los cambios validados durante las betas `3.1.0.10` a `3.1.0.11`. Completa el sistema de cortinillas Discovery Kids con 4 pares de transición comercial y 4 enseguidas post-programa, cerrando definitivamente la sub-rama 3.1.x y la Era Arcoiris.*

### Agregado

**`ya_regresa3.mp4` + `continuamos3.mp4` — Cortinilla Discovery Kids Rosa**
- Tercer par de transición comercial con la cortinilla de color rosa.
- `ya_regresa3` se suma a la lista `YA_REGRESA` para selección aleatoria anti-repetición.
- `continuamos3` queda mapeado a `ya_regresa3` en `YA_REGRESA_CONTINUAMOS_MAP`.
- La lista de pares disponibles pasa de 2 a 3.

**`ya_regresa4.mp4` + `continuamos4.mp4` — Cortinilla Discovery Kids Verde**
- Cuarto par de transición comercial con la cortinilla en color verde.
- `ya_regresa4` se suma a la lista `YA_REGRESA`; `continuamos4` queda mapeado en `YA_REGRESA_CONTINUAMOS_MAP`.
- La lista de pares de transición pasa de 3 a 4 elementos.

**`enseguida3.mp4` — Cortinilla Discovery Kids Azul**
- Tercera enseguida post-programa con la cortinilla de color azul.
- Se suma a `ENSEGUIDAS_POST_PROGRAMA` para selección aleatoria anti-repetición.
- Con 3 enseguidas, la selección es verdaderamente aleatoria (siempre hay 2 candidatos).

**`enseguida4.mp4` — Cortinilla Discovery Kids Amarillo**
- Cuarta enseguida post-programa con la cortinilla en color amarillo.
- Se suma a `ENSEGUIDAS_POST_PROGRAMA`. Con 4 elementos, la selección aleatoria anti-repetición tiene siempre 3 candidatos disponibles.

### Modificado

**Assets actualizados a la Era 2003**
- `bumper.mp4` reemplazado por versión con footage original del año 2003.
- `comercial1.mp4` a `comercial4.mp4` actualizados a la estética del año 2003.

**`enseguida2.mp4` — Cortinilla Verde actualizada a mejor resolución**
- La cortinilla verde existente fue reemplazada por una versión de mayor calidad visual.
- Sin cambios en código; solo reemplazo del archivo en `res/raw/`.

### Nota técnica

> ⚠️ Los videos de programas (`pro1–4.mp4`) deben estar en **resolución 480p o inferior**. Resoluciones superiores (720p+) causan que el `VideoView` use aceleración de hardware (`SurfaceView`) que renderiza en una capa separada y tapa el ScreenBug. Esto es una limitación conocida del sistema actual; se resolverá en una versión futura con `TextureView`.

---

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
| 2006.4.1.0           | 2026-06-22 | 🚀 Release | Configuración (5 opciones, Forzar 4:3, Screenbug, comerciales); CrtOverlayView Era 2006; comercial3/4; Screenbug 10 semanas |
| 2006.4.1.0.12-preview| 2026-06-21 | 🧪 Preview | Configuración rediseñada a lista simple (sin modos, sin debug); Brillo CRT → Efecto CRT on/off; nuevo: duración Screenbug, intervalo comerciales, Forzar 4:3 |
| 2006.4.1.0.11-preview| 2026-06-20 | 🧪 Preview | SettingsActivity nueva (modos Completa/Profesional: música, debug, brillo CRT); comercial3/4 agregados a rotación Era 2006 |
| 2006.4.1.0.10-preview| 2026-06-19 | 🧪 Preview | CrtOverlayView evolucionado a Era 2006 (scanlines/vignette/flicker/borde reducidos); comercial1/2 a Era 2006; Screenbug 10 semanas planeado para 22/06 |
| 2005.4.0.1           | 2026-06-17 | 🚀 Release | BUG FIX: ya_regresa/continuamos ahora determinístico por programa (antes shuffled pool); zona de protección de 3 min sin cortes al final del programa |
| 2005.4.0.0           | 2026-06-16 | 🚀 Release | Primera release Era Doki 1.0: bumpers1–8, logo/comerciales fase 3.0, enseguidas/ya_regresa/continuamos Era Doki, Discovery Kids LA, intervalo comerciales aleatorio 3–9 min |
| 2005.4.0.0.4         | 2026-06-16 | 🔧 Beta    | ya_regresa1-4 y continuamos1-4 actualizados a Era Doki; intervalo comerciales aleatorio 3–9 min |
| 2005.3.4.2           | 2026-06-16 | 🚀 Release | BUG FIX: intervalo comerciales fijo 9 min → aleatorio 3–9 min; bumper6 reemplazado por aviso Era Doki |
| 2005.4.0.0.3         | 2026-06-15 | 🔧 Beta    | enseguida3/4 eliminadas; enseguida1/2 Era Doki; nombre app → Discovery Kids LA; FadeIn 1 s (experimental) |
| 2005.4.0.0.2         | 2026-06-14 | 🔧 Beta    | 4 comerciales fase 3.0 (2005); bg_music reducido a 10:02 min; logo fase 3.0 |
| 2005.4.0.0.1         | 2026-06-13 | 🔧 Beta    | 8 bumpers Era Doki 1.0 (2005–2009)                                      |
| 2004.3.4.0           | 2026-06-12 | 🚀 Release | BUG FIX: screenbug counter, fadeIn videoView; Prev/Next navegan por bloque completo |
| 2004.3.4.0.41        | 2026-06-11 | 🔧 Beta    | BUG FIX: screenbug fadeIn redundante al reanudar desde segundo plano    |
| 2004.3.3.2           | 2026-06-11 | 🚀 Release | BUG FIX: screenbug fadeIn redundante al reanudar desde segundo plano    |
| 2004.3.4.0.40        | 2026-06-10 | 🔧 Beta    | R8 en debug; paridad con release; applicationIdSuffix .beta             |
| 2004.3.3.1           | 2026-06-10 | 🚀 Release | BUG FIX pausa/reanudación; comercial1.mp4 corregido; cortes aleatorios 3-9 min |
| 2004.3.3.0           | 2026-06-08 | 🚀 Release | FadeOut unificado a 500 ms; bumper2.mp4, enseguida1.mp4, ya_regresa4/continuamos4 reemplazados (Era 2004) |
| 2003.3.2.0           | 2026-06-05 | 🚀 Release | Transiciones profesionales, FadeOut antes del fin, ya_regresa por shuffled pool, cortes a los 9 min, FadeOut diferenciado por clip |
| 2003.3.2.0.22-beta   | 2026-06-04 | 🔧 Beta    | Cortes comerciales en intervalo fijo de 9 min; FadeOut diferenciado: enseguida 1 s, bumper 700 ms, ya_regresa 500 ms, continuamos 500 ms |
| 2003.3.2.0.21-beta   | 2026-06-03 | 🔧 Beta    | BUG FIX: FadeOut disparado 2 s antes del fin del video; ya_regresa por shuffled pool sin repetición por ciclo |
| 2003.3.2.0.20-beta   | —          | 🔧 Beta    | Transiciones profesionales FadeOut 2s / FadeIn 1s en todos los cambios de video |
| 2003.3.1.0           | —          | 🚀 Release | Era Arcoiris completa (Fase 2 — Parte 2): 4 pares ya_regresa/continuamos, 4 enseguidas; assets Era 2003; enseguida2 mejorada |
| 2003.3.1.0.11-beta   | —          | 🔧 Beta    | ya_regresa4/continuamos4 (verde), enseguida4 (amarillo), enseguida2 mejorada |
| 2003.3.1.0.10-beta   | —          | 🔧 Beta    | ya_regresa3/continuamos3 (rosa), enseguida3 (azul), assets Era 2003 |
| 2002.3.0.1           | —          | 🚀 Release | Bug fix: screenbug invisible tras eliminación de Tallas en 3.0.0 |
| 2002.3.0.0           | —          | 🚀 Release | StandaloneCommercial; Tallas eliminadas; renombrado ya_volvemos→continuamos1/2, enseguida3/4→ya_regresa1/2; enseguidas aleatorias; assets Era 2002 |
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
