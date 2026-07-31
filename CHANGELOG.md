# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
y este proyecto sigue el estándar de [Versionado Semántico](https://semver.org/lang/es/).


## [2011.5.6.0] — 🚀 Release · Era Doki 1.0 · Era 2011 · "Parque Imaginario" — 2026-07-29

> *2 bug fixes de investigación a fondo sobre NextProgram: un solo CrtOverlayView compartido (antes había dos animaciones corriendo en paralelo, afectando el rendimiento de VideoView) y el ancho real del recuadro corregido con precisión de píxel (se derivaba forzando 4:3, pero el recuadro no es 4:3). BUG FIX de arquitectura: "los programas" (cantidad + video) y los ScreenBugs de eventos vuelven al Launcher — Configuración de Programa pasa a ser solo las opciones de UN programa puntual. "NextProgram" renombrado a "A continuación". Nuevo: "Activar comerciales" por programa.*

### 🐛 BUG FIX — CrtOverlayView duplicado ("otro CRT... afecta a VideoView")

Desde la 5.5.0 había **dos** instancias de `CrtOverlayView` corriendo en paralelo — una sobre el video (`crtOverlay`, dentro de `videoContainer`) y otra exclusiva para el marco de NextProgram (`nextProgramCrtOverlay`, agregada porque el marco vive en un contenedor hermano que la primera no cubría). Cada instancia tiene su propio loop de dibujo por cuadro (`postInvalidateOnAnimation()`), así que tener dos corriendo a la vez duplicaba la carga de renderizado sin necesidad — en hardware limitado (boxes de Android TV con poca RAM), esto podía degradar el rendimiento del propio VideoView.

Se eliminaron ambas instancias nesteadas y se agregó una **única** `CrtOverlayView`, en su propio `AspectRatioFrameLayout` hermano — geométricamente idéntico (mismo criterio 4:3 centrado) a `videoContainer` y al marco de NextProgram — dibujado por encima de los dos. Mismo efecto visual, una sola animación.

### 🐛 BUG FIX — Posición del video en el recuadro NextProgram (causa raíz real)

`showVideoInBox()` (la función que achica y reposiciona `videoContainer` para que el video quede dentro del recuadro decorativo de NextProgram) calculaba el ancho del recuadro forzando una proporción 4:3 sobre su altura — pero `AspectRatioFrameLayout.onMeasure()` ya fuerza 4:3 SIEMPRE, sin importar el `layoutParams` que se le asigne, así que el ancho real que terminaba usando el video **ignoraba por completo** cualquier intento de ajustarlo. El recuadro real del marco NextProgram no es 4:3 — mide aproximadamente 1.4:1, medido con precisión de píxel sobre una imagen de referencia — así que el video quedaba consistentemente ~22px más angosto de lo que debía, corrido de posición respecto al borde amarillo real.

Se agregó `AspectRatioFrameLayout.forceAspectRatio` (desactivable), y `showVideoInBox()` ahora calcula el ancho real del recuadro a partir de una fracción `RIGHT` medida explícitamente (antes no existía — el ancho se derivaba, nunca se medía), en vez de forzar 4:3 sobre la altura.

### 🐛 BUG FIX (arquitectura) — "Los programas" y los ScreenBugs de eventos vuelven al Launcher

La 5.5.0 había movido ENTERA la sección "Programas" (cantidad + video de cada uno) y los ScreenBugs de eventos (globales) a `ProgramConfigActivity`, junto con las opciones de cada programa — un malentendido. La separación correcta:

- **Launcher** (`DiscoveryKidsLauncherActivity`): los **programas** en sí (cantidad, y qué video es cada uno) y los **ScreenBugs de eventos** (Navidad, Año Nuevo, Pascua, Día de la Tierra — globales, no pertenecen a un programa en particular).
- **Configuración de Programa** (`ProgramConfigActivity`): ahora recibe un `programIndex` por Intent y muestra **solo las opciones de ESE programa puntual** — ya_regresa, continuamos, Intro, Créditos, A continuación personalizado, activar comerciales. Se abre con un botón "⚙️ Opciones" en la fila de cada programa, dentro del Launcher. Cada programa guarda su configuración de forma completamente independiente — cambiarla para el Programa 1 nunca afecta al Programa 2 ni a ningún otro.

### 🏷️ Renombrado — "NextProgram" → "A continuación"

El nombre visible en la UI (switch "NextProgram personalizado", selector de imagen/GIF, mensajes de validación) pasa a decir "A continuación" — coincide con lo que efectivamente se ve en pantalla durante la transmisión. Los identificadores internos del código (nombres de función, de variable, claves de `SharedPreferences`) no se tocaron — es un cambio de rótulo visible, no un refactor de nombres internos.

### ➕ Nuevo — Activar comerciales, por programa

Switch nuevo en Configuración de Programa (Predeterminado: activado). Desactivado, ese programa puntual se reproduce de punta a punta sin ningún corte comercial — el resto de los programas no se ve afectado.

### ⚠️ Alcance

> Cambios de código en `activity_main.xml` (CrtOverlayView único, compartido), `AspectRatioFrameLayout.kt` (`forceAspectRatio` desactivable), `LiveDiscoveryKids.kt` (`showVideoInBox()`/`restoreVideoFullScreen()` con ancho real medido, `breakQueue` condicionada a "Activar comerciales"), `SettingsManager.kt` (nuevo `KEY_COMMERCIALS_ENABLED_PREFIX`), `DiscoveryKidsLauncherActivity.kt` (reescrita — Programas + ScreenBugs de eventos de vuelta acá), `ProgramConfigActivity.kt` (reescrita — un solo programa por pantalla, vía `EXTRA_PROGRAM_INDEX`), `activity_launcher.xml`, `activity_program_config.xml`, nuevo `item_program_row.xml` (reemplaza a `item_program_config.xml`). `build.gradle`: `versionName` a `2011.5.6.0` (promovida de Preview a Release).

---

## [2011.5.5.0] — 🚀 Release · Era Doki 1.0 · Era 2011 · "Parque Imaginario" — 2026-07-27

> *Cambio de Era (2010→2011). Nueva Activity Configuración de Programa (extraída del Launcher), NextProgram personalizado, 4 ScreenBugs de eventos (Navidad ahora también configurable, + Año Nuevo, Pascua, Día de la Tierra), y 4 correcciones: ActionBar tapando contenido, ScreenBug repitiéndose entre Intro/Programa/Créditos, CRT ausente en NextProgram, y el ajuste fino de la posición del recuadro.*

### 🆕 Nueva Activity: Configuración de Programa

Se extrajo la sección "Programas" (cantidad, video de cada uno, ya_regresa/continuamos/Intro/Créditos personalizados) de `DiscoveryKidsLauncherActivity` a su propia Activity, `ProgramConfigActivity` — accesible desde un botón en el Launcher. Mismo comportamiento y persistencia (SAF, permisos de lectura) que antes, solo organizado en su propia pantalla.

- Nuevo `activity_program_config.xml`, mismo criterio visual (Material3, `MaterialCardView`) que ya tenía esta sección dentro del Launcher.
- `DiscoveryKidsLauncherActivity` se achica a lo esencial: redirección automática si Experimental está desactivado, botón "Iniciar canal" (+ validación previa), botón "Configuración de Programa", y el menú de Configuración general.

### 🎬 NextProgram personalizado por programa

Nueva fila en Configuración de Programa: activar + elegir una imagen o GIF propio para el NextProgram de cada programa, en vez del `nextprogramN.gif` de fábrica. A diferencia de Intro/Créditos, sí tiene un valor por defecto (los 4 de fábrica) — mismo patrón "personalizado" que ya_regresa/continuamos.

> ⚠️ El archivo personalizado se decodifica en el momento en que NextProgram aparece (no se precarga en un hilo aparte como los 4 de fábrica) — si el usuario elige un archivo muy pesado, podría notarse un pequeño tranco justo en ese instante. Alcance acotado a propósito para esta Release; si se nota en la práctica, se resuelve precargándolo también en segundo plano.

### 🎄 ScreenBugs de eventos: Año Nuevo, Pascua, Día de la Tierra (+ Navidad ahora configurable)

Tres ScreenBugs de evento nuevos, todos reemplazando SOLO la fase 2 (`screenbug.png`, el PNG estático) — las fases 1/3 (`screenbug_start`/`screenbug_end`) siguen siendo siempre las normales:

- **Año Nuevo** (`screenbug_year.png`): 25 de diciembre al 7 de enero.
- **Pascua** (`screenbug_easteregg.png`): Domingo de Pascua — fechas 2026-2030 precalculadas (algoritmo de Computus) y confirmadas a mano: 5 abr 2026, 28 mar 2027, 16 abr 2028, 1 abr 2029, 21 abr 2030.
- **Día de la Tierra** (`screenbug_tierra.png`): 22 de abril, todos los años.

Los 4 (incluida Navidad, que ya existía desde la 2010.5.3.0 pero sin forma de desactivarla) ahora son configurables individualmente desde Configuración de Programa → "ScreenBugs de eventos" — todos activados por defecto (mismo comportamiento que ya tenía Navidad).

### 🐛 BUG FIX — ActionBar tapando el layout

Causa más probable: `DiscoveryKidsLauncherActivity.onCreate()` tenía un `setTheme(R.style.LauncherTheme)` redundante ANTES de `setContentView()` — el manifiesto ya declara ese tema para la Activity. Reaplicar el tema en runtime, después de que la ventana ya se creó con el tema del manifiesto, es un problema conocido de AppCompat que puede alterar cómo se calcula el inset de contenido bajo la ActionBar. Se sacó por completo.

### 🐛 BUG FIX — ScreenBug se reinicia entre Intro/Programa/Créditos

*"El screenbug se reinicia y vuelve a mostrar screenbug de inicio cuando cambia de intro a programa sabiendo de que ya se mostró en el intro, lo mismo pasa en créditos."*

La 5.4.1 hacía correr la fase 1/2 completa de nuevo en el primer segmento del Programa aunque ya hubiera corrido en la Intro (para garantizar que apareciera al menos una vez) — pero eso es justo lo que se veía como "se reinicia": el GIF de aparición se mostraba de nuevo, en vez de continuar en la fase que le tocaba.

- **Intro → Programa**: se volvió al enfoque de carry-over (`lastIntroDurationMs`): el Programa usa la duración real de la Intro para decidir qué fase RESTAURAR (mid, si la Intro ya la mostró y ocultó el start) o cuánto falta del delay original — nunca vuelve a agendar el start desde cero si ya se mostró. Combinado con el clamp de duración corta ya arreglado en la 5.4.1, no reintroduce el bug de "nunca aparece" en Intros cortas.
- **Programa → Créditos**: nuevo flag `screenBugMidVisible` — si la fase 2 (mid) estaba visible justo antes del corte a Créditos, se restaura de inmediato ahí (en vez de quedar oculta hasta que le toque a la fase 3).

### 🐛 BUG FIX — NextProgram sin efecto CRT

El `CrtOverlayView` original vive dentro de `videoContainer` — desde la Release 5.4.1, el marco NextProgram vive en su PROPIO `AspectRatioFrameLayout` hermano (para que el logo no se achique junto con el video del recuadro), así que nunca tenía el efecto CRT encima. Se agregó una segunda instancia de `CrtOverlayView` en ese mismo contenedor, configurada junto con la original en `applySettings()`.

### 🎯 Ajuste fino de la posición del recuadro

Remedido sobre la nueva imagen de referencia (con el video ya mostrándose adentro, a diferencia de la anterior): `NEXTPROGRAM_BOX_LEFT_FRACTION` 0.46→0.466, `TOP` 0.09→0.083, `BOTTOM` 0.53→0.525.

### ⚠️ Alcance

> Nuevo: `ProgramConfigActivity.kt`, `activity_program_config.xml`. Modificados: `LiveDiscoveryKids.kt` (eventos estacionales, NextProgram personalizado, carry-over de ScreenBug, segundo CRT, fracciones del recuadro), `SettingsManager.kt` (keys de NextProgram/eventos), `DiscoveryKidsLauncherActivity.kt` (extracción), `activity_launcher.xml`, `item_program_config.xml`, `activity_main.xml`, `AndroidManifest.xml`. `build.gradle`: `versionName` a `2011.5.5.0`.

---

## [2010.5.4.1] — 🐛 Bug Fix · Era Doki 1.0 · Era 2010 · "Parque Imaginario" — 2026-07-25

> *3 correcciones sobre la 5.4.0: el ScreenBug de inicio/final directamente no aparecía en Intro/Créditos (bug real, no de paciencia — la cuenta de 20s/46s nunca entraba en clips cortos y no se agendaba nada), y NextProgram tenía mal la idea de qué va en el recuadro: es el VIDEO del programa, no otro GIF.*

### 🐛 BUG FIX — ScreenBug no aparecía en Intro ni en Créditos

Causa raíz real: `SCREENBUG_START_DELAY_MS` (20s) y `SCREENBUG_END_SHOW_BEFORE_MS` (46s) se usaban tal cual sobre la duración real de la Intro/Créditos — clips que en la práctica suelen ser bastante más cortos que eso (créditos de 10-15s, por ejemplo). El cálculo daba negativo y la condición de guarda (`segmentDuration > X`) hacía que directamente **nunca se agendara nada** — no es que apareciera tarde o hubiera que tener paciencia, no aparecía nunca.

- `scheduleMultipleScreenbugs()` ahora hace clamp de `startShowAt`/`endShowAt` a la duración real del clip (`coerceAtMost`/`coerceAtLeast`), en vez de usar los 20s/46s "a ciegas". En clips largos (cualquier Programa normal) el resultado es idéntico a antes — el clamp no cambia nada cuando sobra tiempo de sobra. En clips cortos, aparece lo antes posible para GARANTIZAR que se llegue a mostrar.
- Se descartó el diseño original de "suprimir la fase 1/2 en el Programa si hubo Intro" (asumiendo que la Intro ya la dejaba mostrada): `beginProgramSegment()` siempre resetea el ScreenBug a oculto al arrancar cualquier segmento nuevo, así que esa supresión causaba que lo que se llegó a mostrar en la Intro se ocultara de golpe justo al cortar a Programa y **nunca volviera a aparecer**. Ahora la fase 1/2 corre en la Intro (si existe) Y SIEMPRE en el Programa también — en el peor caso se ve dos veces, pero nunca deja de aparecer.
- `scheduleNextProgramBug()` tenía el mismo bug (con `NEXTPROGRAM_SHOW_BEFORE_MS`, 31s) — mismo fix.

### 🎯 BUG FIX — El recuadro de NextProgram mostraba el marco mal entendido

Corrección de diseño, no solo de código: lo que va DENTRO del recuadro (la zona que la Release 5.4.0 dejó transparente en el GIF `nextprogram*.gif`) es el **VideoView del programa en curso**, mostrado sin estirar ni deformar — NO otro GIF ni el mismo `nextprogram*.gif` reposicionado, como se había interpretado en la 5.4.0.

- El GIF `nextprogram*.gif` vuelve a ser el marco decorativo de pantalla completa (logo, texto, borde amarillo del recuadro), con el recuadro TRANSPARENTE en el arte.
- Mientras el marco está visible, `videoContainer` (el contenedor real del video, `AspectRatioFrameLayout`) se achica y reposiciona con `LiveDiscoveryKids.showVideoInBox()` para caer exactamente dentro del recuadro — conserva su proporción real en todo momento (`AspectRatioFrameLayout` siempre deriva el ancho como alto×4/3, nunca estira). Al terminar, `restoreVideoFullScreen()` lo vuelve a pantalla completa.
- BUG FIX adicional encontrado armando esto: `nextProgramBug` (el marco) vivía DENTRO de `videoContainer` en la 5.4.0 — si se hubiera dejado así, el logo/texto del marco se habría achicado JUNTO con el video al reposicionarlo en el recuadro, arruinando el diseño. Se movió a su propio `AspectRatioFrameLayout` hermano en `activity_main.xml`, con el mismo criterio de tamaño (alto de pantalla → ancho×4/3 centrado) para que quede perfectamente alineado con el marco del video esté achicado o no, sin importar la proporción real de la pantalla del dispositivo.

### ⚠️ Alcance

> Cambios de código en `LiveDiscoveryKids.kt` (`scheduleMultipleScreenbugs()`/`scheduleNextProgramBug()` con clamp interno, `showVideoInBox()`/`restoreVideoFullScreen()` nuevas, `playIntro()`/`scheduleCreditosOverlays()` simplificadas). `activity_main.xml` (`nextProgramBug` movido a su propio `AspectRatioFrameLayout`). `build.gradle`: `versionName` a `2010.5.4.1`.

---

## [2010.5.4.0] — 🚀 Release · Era Doki 1.0 · Era 2010 · "Parque Imaginario" — 2026-07-24

> *Consolida la Preview 2010.5.4.0.40 (NextProgram, ScreenBug final a 46s) como Release estable, y agrega lo pedido para el 24-07-26: 2 bug fixes de arranque (ANR, cantidad de programas), reposicionamiento real de NextProgram, eliminación de StandaloneCommercial, e Intro/Créditos personalizados por programa — con ScreenBug/NextProgram atados a ellos sin reiniciar la cuenta.*

### 🐛 BUG FIX — ANR "Discovery Kids no responde" al abrir la app

Causa raíz: `preloadScreenBugAssets()` y `preloadNextProgramGifs()` decodificaban hasta 8 GIFs (`Movie.decodeStream()`) de forma **sincrónica en el hilo principal**, dentro de `onCreate()` — la Preview 2010.5.4.0.40 sumó 4 GIFs más al mismo hilo (los de NextProgram), empujando el tiempo total por encima de lo que tolera Android antes de mostrar el diálogo "no responde". `GifMovieDrawable` no depende del hilo desde el que se construye (solo crea un `Handler` apuntando al Looper principal, válido desde cualquier hilo) — ahora ambas funciones decodifican en un hilo aparte y solo postean la asignación final a los campos de la Activity con `runOnUiThread()`.

### 🐛 BUG FIX — NextProgram no se ubicaba en el recuadro ni animaba

La Preview 2010.5.4.0.40 había agregado el `ImageView` de NextProgram a pantalla completa (`match_parent`), asumiendo que el GIF final traería compuesto todo el frame — confirmado en dispositivo que estaba mal. Ahora se ubica con 4 guías porcentuales de `ConstraintLayout` (~46%–94% horizontal, ~9%–53% vertical del marco 4:3), calculadas sobre la imagen de referencia que envió Keyler, con `scaleType="fitXY"`. La animación de aparición (fade-in, 500 ms) no tenía ningún problema de código — el `ImageView` fuera de lugar hacía parecer que "no animaba".

### 🐛 BUG FIX — LiveDiscoveryKids seguía mostrando 4 programas con menos elegidos

No se encontró un bug estático en `totalProgramCount()`/`buildPlaylist()` en sí (leen `SettingsManager.getProgramCount()` correctamente) — el camino más probable, dado que `DiscoveryKidsLauncherActivity` no cierra su propia Activity al iniciar el canal (`startActivity()` sin `finish()`): si `LiveDiscoveryKids` queda vivo en la pila de tareas y el usuario cambia la cantidad de programas y vuelve a esa misma instancia (por Recientes, sin pasar por `onCreate()` de nuevo), el `playlist` construido con la cantidad VIEJA nunca se actualizaba. `onResume()` ahora reconstruye `playlist` si la cantidad cambió desde la última construcción (`playlistBuiltForCount`), sin interrumpir el clip que esté sonando en ese momento.
> Si el bug persiste después de este fix con otra causa, Keyler: pasame el logcat del momento exacto en que pasa — con el fix de arriba debería quedar resuelto para el escenario más probable, pero si hay otro camino que lo dispara, con logs lo encuentro más rápido que adivinando en el código.

### ⏭️ Eliminado: StandaloneCommercial

Ya no existe un ítem propio del playlist para los comerciales sueltos entre Bumper y Programa. El ciclo pasa de `Bumper → StandaloneCommercial → Programa` a `Bumper → [Intro] → Programa → [Créditos]` — los comerciales ahora **solo** aparecen interrumpiendo un Programa en curso (`playCommercial`, sin cambios en esa lógica).

### 🎬 Nuevo: Intro y Créditos personalizados por programa

Nueva sección en Discovery Kids Launcher → Configuración de Programa (por cada programa, junto a ya_regresa/continuamos personalizados): un switch de activar + un selector de archivo (SAF), sin video predeterminado incluido en la app.

- **Intro**: aparece después del Bumper y antes del Programa. Solo se quita al terminar su propio video.
- **Créditos**: aparecen al terminar el Programa. Solo se quitan al terminar su propio video.
- Ambos son opcionales: si el usuario no los activó, o los activó pero no eligió un archivo, no se agregan al playlist (`hasValidIntro()`/`hasValidCreditos()`) — y el Launcher ahora **bloquea "Iniciar canal"** avisando cuáles faltan (ver bug fix de validación más abajo), en vez de saltearlos en silencio como antes hacía un programa sin `.mp4`.

### 🔗 ScreenBug / NextProgram atados a Intro/Créditos, sin reiniciar la cuenta

Pedido explícito: *"el screenbug de inicio debería aparecer en el Intro, y el screenbug final en los Créditos — y que la Intro, el Programa y los Créditos sumen, sean una sola duración, sin que la cuenta se reinicie ni se detenga al cambiar de clip."*

- **ScreenBug inicio (fase 1/2, `screenbug_start`/`screenbug`)**: si hay Intro válida, la cuenta de 20s arranca ahí. Si la Intro dura menos de 20s, al pasar al Programa la cuenta CONTINÚA exactamente donde quedó (usa la duración real de la Intro, `lastIntroDurationMs`, no vuelve a 0) — nunca se reinicia ni se detiene, aunque la Intro sea muy corta.
- **ScreenBug final (fase 3, `screenbug_end`) + NextProgram**: si hay Créditos válidos, NO corren en el Programa — se difieren a los Créditos, agendados con la duración REAL de los créditos (no la del programa) apenas se conoce (su propio `onPrepared`).
- Sin Intro ni Créditos: comportamiento 100% igual que antes (cero riesgo de regresión para el caso clásico).
- Implementación: no fue necesario precalcular la duración total del bloque de antemano — cada clip nuevo RECALCULA cuánto falta de cada cuenta usando la duración real del clip anterior, en vez de depender de que un mismo timer sobreviva el cambio de clip (eso sí hubiera reintroducido el bug ya solucionado antes de "el ScreenBug se reinicia" al pasar a segundo plano, porque los timers de Handler no se pausan cuando el video sí).
- El resume desde segundo plano (`onResume()`) también quedó cubierto para Créditos: si la app vuelve mientras estaban sonando, se reagenda la fase 3 + NextProgram con la posición y duración correctas.

### ✅ Nuevo: validación antes de iniciar el canal

`DiscoveryKidsLauncherActivity` ahora revisa, antes de arrancar `LiveDiscoveryKids`, que todo lo activado en Configuración de Programa tenga un video real: Programas (con el mismo chequeo de archivo/MediaStore de `pro{N}.mp4` que usa el canal), Intro, Créditos, ya_regresa y continuamos personalizados. Si falta algo, un diálogo lista exactamente qué programa y qué campo — y no deja iniciar el canal hasta corregirlo.

### 🕐 Cambios de timing

- ScreenBug: 5s → 4,9s (`SCREENBUG_START_ESTIMATED_DURATION_MS` y `SCREENBUG_END_VISIBLE_DURATION_MS`) — evita el salto visible de loop del GIF al inicio.

### 📝 Otras notas

- **"Se ve 1ms del clip anterior tras la pantalla negra al cambiar de video":** no se tocó — coincidimos con la sospecha de Keyler de que es una particularidad de `VideoView`/el decoder de Android en el cambio de superficie, no algo controlable desde la lógica de la app. Si en algún momento se vuelve más notorio o hay pistas de que es evitable, avisame y lo investigo a fondo.
- Se quitó del README la advertencia de usar programas en 480p o inferior (pedido explícito).

### ⚠️ Alcance

> Cambios de código en `LiveDiscoveryKids.kt` (preload en background, NextProgram reposicionado, `PlayItem.StandaloneCommercial` eliminado, `PlayItem.Intro`/`PlayItem.Creditos` nuevos, `playIntro()`/`playCreditos()`, `scheduleMultipleScreenbugs()`/`scheduleSegmentLogic()` reescritas para el timing de bloque, `playlistBuiltForCount` + reconstrucción en `onResume()`), `SettingsManager.kt` (keys de Intro/Créditos), `DiscoveryKidsLauncherActivity.kt` (filas de Intro/Créditos, `validateChannelSetup()`), `item_program_config.xml` (filas nuevas), `activity_main.xml` (NextProgram con `ConstraintLayout`). `build.gradle`: `versionName` a `2010.5.4.0` (Release estable, sin número de build).

---

## [2010.5.4.0.40] — 🧪 Preview · Era Doki 1.0 · Era 2010 · "Parque Imaginario" — 2026-07-22

> *Reemplazo del "enseguida" post-programa por NextProgram: un overlay GIF que se superpone al programa mismo, no un clip aparte. El ScreenBug final ahora empieza 46s antes del final del programa (antes 20s), dejando lugar a que NextProgram aparezca 15s después, a los 31s antes del final. De paso, investigación a fondo destapó un bug de compilación arrastrado desde la 4.6.0: los 11 archivos de extensión que esa Release decía haber eliminado en realidad seguían en el proyecto, duplicando cada función de `LiveDiscoveryKids.kt` y rompiendo la build.*

### ⏭️ Reemplazo de Enseguida → NextProgram

El clip "enseguida" post-programa (`PlayItem.Enseguida`) desaparece del playlist por completo: el ciclo pasa de `Enseguida → Bumper → StandaloneCommercial → Programa` a `Bumper → StandaloneCommercial → Programa`. En su lugar, un nuevo overlay **NextProgram** (GIF) se superpone directamente sobre el programa que está terminando, `NEXTPROGRAM_SHOW_BEFORE_MS` (31 segundos) antes de su final real, con una animación de entrada (fade-in) de `NEXTPROGRAM_ANIM_MS` (500 ms).

- `nextprogram1.gif`–`nextprogram4.gif`: uno por programa, misma asignación determinística por índice que `ya_regresaN`/`continuamosN` (`currentProgramIndex % NEXTPROGRAMS.size`).
- Solo se agenda en el **último segmento real** del programa (`breakQueue` vacío al momento de programarlo) — nunca aparece antes de un corte comercial a mitad de programa.
- Se oculta de golpe junto con el corte al terminar el programa (sin fadeOut propio), en los mismos puntos donde ya se reseteaba el ScreenBug: inicio de cualquier segmento, Prev/Next, entrada a comercial/bumper/standalone, y ambos caminos de fin de programa (normal y fallback).
- Nueva función `scheduleNextProgramBug()`, análoga a `scheduleMultipleScreenbugs()`: si al restaurar sesión el punto de aparición ya pasó, se muestra de inmediato sin animación en vez de esperar un timer vencido.
- Caché de los 4 `GifMovieDrawable` vía `preloadNextProgramGifs()`, llamada en `onCreate()` junto a `preloadScreenBugAssets()` — mismo motivo (evitar lag al decodificar el GIF la primera vez que se muestra).

> ⚠️ Este build **no incluye los archivos de arte** de NextProgram — hay que agregar `nextprogram1.gif`, `nextprogram2.gif`, `nextprogram3.gif` y `nextprogram4.gif` a `res/drawable/` antes de compilar. El `ImageView` `nextProgramBug` se agregó full-frame (`match_parent`) igual que `screenBug`, asumiendo que cada GIF ya trae compuesto todo el contenido visible (recuadro de vista previa + logo + texto), tal como se ve en la imagen de referencia que enviaste. Si el asset final termina siendo más chico o necesita posicionamiento específico dentro del frame, avisame para ajustar el layout.

### 🕐 ScreenBug final: 20s → 46s antes del final

`SCREENBUG_END_SHOW_BEFORE_MS` pasa de 20 000 ms a 46 000 ms, para que el ScreenBug final (logo) ya esté en pantalla cuando aparece NextProgram 15s después (46s − 31s = 15s de diferencia entre ambos).

### 🐛 BUG FIX (investigación a fondo) — 11 archivos duplicados impedían compilar

Al tocar `ChannelScreenBug.kt` para este cambio, aparecieron **dos** declaraciones de `LiveDiscoveryKids.fadeInBug()` en el proyecto — una en `ChannelScreenBug.kt` y otra, más completa, ya reunificada en `LiveDiscoveryKids.kt`. Investigando la causa raíz: la Release 4.6.0 (REUNIFICACIÓN) documentó en un comentario que los 11 archivos de extensión (`ChannelPlaylist.kt`, `ChannelProgramPlayback.kt`, `ChannelCommercialBlock.kt`, `ChannelVideoTransitions.kt`, `ChannelMediaResolver.kt`, `ChannelBackgroundMusic.kt`, `ChannelSessionState.kt`, `ChannelPositionTracker.kt`, `ChannelScreenBug.kt`, `ChannelUiHelpers.kt`, `ChannelDebugOverlay.kt`) "dejan de existir como archivos separados" — pero en la práctica **nunca se borraron del disco**. Seguían presentes, con el contenido idéntico letra por letra al ya reunificado en `LiveDiscoveryKids.kt`, lo que hace que el proyecto no compile (error de redeclaración) desde esa Release. Se borraron los 11 archivos.

### ⚠️ Alcance

> Cambios de código en `LiveDiscoveryKids.kt` (`PlayItem.Enseguida` eliminado, `playEnseguida()` eliminada, `buildPlaylist()` sin Enseguida, `NEXTPROGRAMS`/`NEXTPROGRAM_SHOW_BEFORE_MS`/`NEXTPROGRAM_ANIM_MS` nuevas, `SCREENBUG_END_SHOW_BEFORE_MS` a 46 000, nuevas `scheduleNextProgramBug()`/`fadeInNextProgramBug()`/`showNextProgramResource()`/`preloadNextProgramGifs()`/`setNextProgramBugAlpha()`). `activity_main.xml` (nuevo `ImageView` `nextProgramBug`). Se eliminaron los 11 archivos de extensión duplicados listados arriba. `build.gradle`: `versionName` a `2010.5.4.0.40`.

---

## [2010.5.3.0] — 🚀 Release · Era Doki 1.0 · Era 2010 · "Parque Imaginario" — 2026-07-20

> *Cambio de Era (2009→2010). BUG FIX definitivo del fadeOut/fadeIn de programas — causa raíz real esta vez: el fix de la 2009.5.2.1 estaba incompleto, no contemplaba una reanudación (tras un corte comercial, volver de Configuración, etc.). Nuevo ScreenBug de Navidad (1–24 de diciembre), con el mismo comportamiento de 3 fases que el normal.*

### 🐛 BUG FIX (definitivo) — FadeOut/FadeIn de programas

El fix de la 2009.5.2.1 agregó el fadeOut preventivo a `beginProgramSegment()`, pero el cálculo de CUÁNDO dispararlo tenía un bug: usaba `programDuration - fadeOutMs` como si el video arrancara siempre desde el segundo 0, **sin restar `startOffsetMs`** (el punto real donde arranca esta vez). Esto es incorrecto en cualquier reanudación (`isNewSegment=false`, o retomar tras un corte comercial) — el timer quedaba programado para un momento que, en la práctica, ya había quedado en el pasado (el video, arrancando adelantado, llega a su fin real antes de que el timer mal calculado dispare), así que el programa terminaba por el mecanismo de respaldo (corte abrupto, sin fadeOut) en vez del programado — y como esa transición al siguiente clip arrancaba desde un corte abrupto en vez de un `withEndAction` ya asentado, el fadeIn del siguiente clip también quedaba roto. Como prácticamente cualquier programa con al menos un corte comercial pasa por una reanudación así, el bug se manifestaba de forma casi constante.

Corregido con el mismo patrón que ya usa correctamente `resumeUriWithSeek()`: `remaining = duration - startOffsetMs`, y el fadeOut se programa sobre ese tiempo restante real, no sobre la duración completa.

### 🎄 Nuevo ScreenBug de Navidad

Del 1 al 24 de diciembre (inclusive, cualquier año), el ScreenBug usa un set alternativo con temática navideña — mismo comportamiento de 3 fases que el normal (entrada animada, estático, salida animada), seleccionado automáticamente por fecha del sistema. Fuera de esas fechas, se usa el set normal sin ningún cambio.

> ⚠️ Este build **no incluye los archivos de arte** del ScreenBug de Navidad — hay que agregar `screenbug_navidad.png`, `screenbug_start_navidad.gif` y `screenbug_end_navidad.gif` a `res/drawable/` (mismos nombres, mismo formato que sus equivalentes normales) antes de compilar.

### 🎨 Cambio de Era

> Era 2009 → Era 2010. Sin cambios de contenido asociados a la Era en esta Release (el ScreenBug de Navidad es estacional, no de Era — convive con el set normal el resto del año).

### ⚠️ Alcance

> Cambios de código en `LiveDiscoveryKids.kt` (`beginProgramSegment()` con cálculo de fadeOut corregido, `scheduleMultipleScreenbugs()` con selección de set normal/Navidad, nueva `isChristmasScreenBugActive()`, `preloadScreenBugAssets()`/`showScreenBugResource()` con las variantes de Navidad). `build.gradle`: `versionName` a `2010.5.3.0` (cambio de Era 2009→2010).

---

## [2009.5.2.1] — 🐛 Bug Fix · Era Doki 1.0 · Era 2009 · "Parque Imaginario" — 2026-07-18

> *TextureView eliminado por completo (motor de video, switch "Recortar 4:3" y AlertDialog de 720p+), 2 bugs de arrastre con causa raíz encontrada — el contenedor de video ya no cambia de forma con "Forzar 4:3" (siempre 4:3), y el programa ahora sí hace fadeOut al terminar — y rediseño: la ActionBar de Configuración deja de ser un header hecho a mano, y el logo del Launcher pasa de la ActionBar al cuerpo de la pantalla.*

### 🗑️ Eliminado — TextureView por completo

Se sacó del proyecto el motor de video alternativo basado en TextureView (introducido en la 2009.5.0.0) junto con todo lo que dependía de él: la clase `TextureVideoView`, el switch "Recortar 4:3" (ex "Usar TextureView") de Configuración, `SettingsManager.isTextureViewEnabled()`/`setTextureViewEnabled()`, y el `AlertDialog` que avisaba sobre programas de 720p o superior. `DkVideoView` vuelve a ser una sola clase concreta (ya no hace falta la separación abstracta con dos motores) — un envoltorio de `VideoView` clásico.

### 🐛 BUG FIX — "Forzar 4:3" seguía roto (causa raíz real, esta vez sí)

Las dos releases anteriores tenían el diseño invertido: el contenedor (`AspectRatioFrameLayout`) dejaba de estar en 4:3 (pasaba a ocupar toda la pantalla, forma 16:9) cuando "Forzar 4:3" estaba desactivado. Eso está mal — el contenedor **tiene que estar siempre en 4:3**, sin excepción; lo que decide "Forzar 4:3" es qué pasa **dentro** de esa caja: si el video se estira para llenarla exacto (activado) o si se ajusta preservando su proporción real sin estirarse (desactivado) — un video 16:9, por ejemplo, encaja con franjas arriba/abajo en vez de deformarse. `AspectRatioFrameLayout` ya no tiene ningún toggle — vuelve a forzar 4:3 siempre, sin condición — y `DkVideoView` es el único que responde al switch, con la lógica de fit real que ya existía.

### 🐛 BUG FIX — El programa no hacía fadeOut al terminar (y por eso el siguiente tampoco fadeIn)

A diferencia de TODOS los demás tipos de clip (bumper, enseguida, comercial — que ya usaban un fadeOut programado antes de su final real), el fin de un programa no tenía ningún fadeOut: el video cortaba en seco y `advance()` arrancaba el siguiente clip directamente desde ese corte abrupto, en vez de disparar la transición desde el cierre de una animación ya asentada — rompiendo también el fadeIn del clip siguiente. Ahora `beginProgramSegment()` programa el mismo fadeOut preventivo que ya usan todos los demás clips, y la transición al siguiente arranca desde su `withEndAction` — mismo patrón que ya funcionaba en el resto de la app.

### 🎨 Diseño — ActionBar de Configuración y logo del Launcher

- **Configuración**: se eliminó el header hecho a mano (ImageButton "Atrás" + TextView "Configuración" simulando una barra) y pasa a usar la ActionBar real de Android (`Theme.AppCompat` en vez de `.NoActionBar`), con navegación "Up" nativa.
- **Launcher**: la ActionBar ahora solo tiene el título — el logo se sacó de ahí (antes vivía como ícono de la ActionBar) y pasa al cuerpo de la pantalla, entre la ActionBar y el botón "Iniciar canal".

### ⚠️ Alcance

> Cambios de código en `DkVideoView.kt` (reescrito — una sola clase, sin TextureView), `AspectRatioFrameLayout.kt` (sin toggle, siempre 4:3), `LiveDiscoveryKids.kt` (`beginProgramSegment()` con fadeOut real, `applySettings()` corregido, `checkVideoResolutionAndWarn()` eliminada), `SettingsActivity.kt`/`SettingsManager.kt` (switch "Recortar 4:3" eliminado, ActionBar real), `DiscoveryKidsLauncherActivity.kt` (logo fuera de la ActionBar), `activity_settings.xml`/`activity_launcher.xml`/`themes.xml`. `build.gradle`: `versionName` a `2009.5.2.1`.

---

## [2009.5.2.0] — 🚀 Release · Era Doki 1.0 · Era 2009 · "Parque Imaginario" — 2026-07-17

> *Release de investigación a fondo: 2 bugs de arrastre finalmente resueltos con causa raíz identificada — el ScreenBug "reiniciándose" y el video estirado a 16:9 con "Forzar 4:3" desactivado. Además, el ítem de Configuración pasa al menú de overflow original de Android, y "Usar TextureView" se renombra a "Recortar 4:3" con deshabilitado condicional. Nuevo Screenbug Julio 2009 – 2011.*

### 🐛 BUG FIX — El ScreenBug "se reinicia" (causa raíz encontrada)

`beginProgramSegment()` llama `setBugAlpha(0f)` **siempre** al arrancar — incluso en un simple resume dentro del mismo proceso (abrir Configuración y volver, o un ratito en segundo plano, `isNewSegment=false`). `scheduleMultipleScreenbugs()` solo programaba eventos **futuros** de show/hide según `elapsed`, pero nunca restauraba de inmediato la fase que ya debería estar visible en ese punto del programa — así que el ScreenBug quedaba oculto hasta el próximo evento programado, varios segundos después, y reaparecía mostrando otra fase, fuera de lugar. Eso es lo que se percibía como "se reinicia". Ahora, si `elapsed` cae dentro de la ventana visible de alguna fase, esa fase se restaura de inmediato — sin reiniciar el GIF al frame 0 (`resetAnimation=false`), para que no se note un salto.

### 🐛 BUG FIX — Video estirado a 16:9 con "Forzar 4:3" desactivado (causa raíz encontrada)

El fix de la 2009.5.1.0 para "Forzar 4:3" dejó un efecto secundario: `applySettings()` forzaba el `layoutParams.width` de `videoView` a `MATCH_PARENT` **siempre**, pisando el `WRAP_CONTENT` + `gravity=CENTER` original (`onCreate()`) que era justo lo que le permitía calcular su propio tamaño según la proporción real del video. Además, `LegacyVideoView` (motor VideoView clásico) nunca tuvo lógica propia de ajuste de aspecto — dependía enteramente, y de forma poco confiable, del comportamiento interno heredado de `VideoView`. Resultado: con "Forzar 4:3" desactivado, el video se estiraba sin más a la forma del contenedor (16:9 en pantalla completa), distorsionado, sin importar su proporción real.

Se reescribió `DkVideoView.kt`: el fit de aspecto ahora vive en la clase base, compartido por los dos motores (`LegacyVideoView` y `TextureVideoView`) — `forceAspectRatio` (sincronizado desde `applySettings()`, igual que en `AspectRatioFrameLayout`) y `videoAspect` (la proporción real del video, capturada de `MediaPlayer.videoWidth`/`videoHeight` apenas se conoce). Con el forzado desactivado, el video ahora se ajusta preservando su proporción real dentro del espacio disponible (pillarbox/letterbox), sin estirarse nunca.

### 🎨 Discovery Kids Launcher — ActionBar

- El ítem "Configuración" de la ActionBar pasa de ícono fijo (`showAsAction="always"`) al **menú de overflow original de Android** (los 3 puntos, `showAsAction="never"`) — el patrón estándar del sistema, en vez de un ícono agregado por la app.

### ⚙️ Configuración — "Recortar 4:3" (antes "Usar TextureView")

- El switch se renombra de "Usar TextureView" a **"Recortar 4:3"** (sigue siendo el mismo switch — motor de video TextureView vs. clásico, `SettingsManager.isTextureViewEnabled()` sin cambios).
- Ahora se **deshabilita** (grisado, no clickeable) mientras "Forzar 4:3" esté activado — en ese caso el video ya se recorta a 4:3 de todas formas, así que esta opción no cambia nada. Se habilita de nuevo apenas se desactiva "Forzar 4:3".

### 🎨 Contenido

- Nuevo Screenbug (Julio 2009 – 2011).

### ⚠️ Alcance

> Cambios de código en `DkVideoView.kt` (reescrito — fit de aspecto real compartido entre motores), `LiveDiscoveryKids.kt` (`scheduleMultipleScreenbugs()` restaura fase visible de inmediato, `fadeInBugWithResource()`/`showScreenBugResource()` con `resetAnimation`, `applySettings()` sin pisar `layoutParams`), `SettingsActivity.kt` (`updateCropSwitchEnabledState()`), `activity_settings.xml` (rename + ids), `menu_launcher.xml` (`showAsAction="never"`). `build.gradle`: `versionName` a `2009.5.2.0`.

---

## [2009.5.1.0] — 🚀 Release · Era Doki 1.0 · Era 2009 · "Parque Imaginario" — 2026-07-15

> *Release enfocada en pulir la 2009.5.0.0: Discovery Kids Launcher rediseñado a Material Design 3 puro (esquema claro/oscuro automático, ActionBar original de Android en vez del MenuBar hecho a mano), 3 bug fixes de la Release anterior (Forzar 4:3, recorte de video con TextureView, ScreenBug reiniciándose al cambiar de Activity o volver de segundo plano), ajustes de timing y reproducción del ScreenBug de 3 fases, y nuevo Screenbug de Mayo–Julio 2009.*

### 🎨 Discovery Kids Launcher — rediseño a Material Design 3

- **Reemplazo completo del MenuBar custom por la ActionBar original de Android.** El `LinearLayout` con degradado azul→cian, logo e ícono de ajustes hecho a mano en la 2009.5.0.0 se eliminó. `LauncherTheme` pasa de `Theme.AppCompat.NoActionBar` a `Theme.Material3.DayNight` (con ActionBar real, la que trae el sistema por defecto). El título y el ícono de la app se fijan por código (`DiscoveryKidsLauncherActivity.onCreate()`), y el botón de Configuración pasa a ser un ítem del menú de opciones (`menu_launcher.xml`, `onCreateOptionsMenu()`/`onOptionsItemSelected()`) en vez de un `ImageButton` propio.
- **Esquema de color Material 3 completo, con versión clara y oscura automática (DayNight).** Semilla azul (misma familia que el degradado que tenía el MenuBar anterior, `#1565C0`/`#0061A4`), con todos los roles de color MD3 (`primary`, `primaryContainer`, `secondary`, `secondaryContainer`, `surface`, `surfaceVariant`, `outline`, etc.) — ver `values/colors.xml` (claro) y el nuevo `values-night/colors.xml` (oscuro, mismos nombres). El sistema elige automáticamente según el tema del dispositivo, sin código adicional. `android:windowLightStatusBar` también se resuelve por tema vía un `bool` calificado (`values/bools.xml` / `values-night/bools.xml`).
- **Componentes Material 3 reales.** "Iniciar canal" ahora es un `MaterialButton` relleno; la fila "Cantidad de programas" vive dentro de un `MaterialCardView` con elevación estándar M3. `item_program_config.xml` (la fila de cada programa) migró sus colores fijos (`dk_text_primary`, `dk_text_secondary`, `dk_accent`, `dk_stroke`) a atributos de tema (`?attr/colorOnSurface`, `?attr/colorOnSurfaceVariant`, `?attr/colorPrimary`, `?attr/colorOutlineVariant`), así que también sigue el esquema claro/oscuro.

### 🐛 Bug fixes de la 2009.5.0.0

- **BUG FIX — "Forzar 4:3" no respetaba el switch (forzaba siempre, activado o desactivado).** Causa raíz: `AspectRatioFrameLayout.onMeasure()` forzaba el contenedor **padre** a proporción 4:3 incondicionalmente; `applySettings()` intentaba controlarlo cambiando el ancho del `videoView` **hijo**, lo cual no tenía ningún efecto real porque el padre ya venía recortado de antemano. Ahora `AspectRatioFrameLayout` expone `forceAspectRatio: Boolean`, sincronizado con `SettingsManager.isForceAspectRatioEnabled()` en `applySettings()` (con `requestLayout()`); desactivado, el contenedor ocupa el espacio completo sin recorte.
- **BUG FIX — recorte de video a 4:3 con TextureView en resoluciones altas.** Mismo bug y mismo fix que el anterior: al desactivar "Forzar 4:3" el video ya no se recorta, sea cual sea el motor de video (`VideoView` o `TextureView`) — antes el recorte ocurría siempre, sin importar el motor.
- **BUG FIX — "volvió" el problema del ScreenBug (logo) reiniciándose al cambiar de Activity o volver de segundo plano.** Causa raíz: `resumeSavedState()` (restauración de sesión tras recreación de proceso — común en boxes de Android TV con poca RAM) llamaba a `beginProgramSegment()` sin pasar `isNewSegment`, así que usaba el valor por defecto (`true`). Esto reseteaba `currentSegmentStartMs` al punto de reanudación y el `elapsed` se recalculaba en `0`, haciendo que el ciclo completo de 3 fases del ScreenBug arrancara de cero como si el segmento recién empezara ahí, en vez de continuar donde correspondía. Ahora `currentSegmentStartMs` se persiste (`PREF_SEGMENT_START_MS`) y se restaura antes de llamar a `beginProgramSegment(..., isNewSegment = false)`.

### 🎬 ScreenBug de 3 fases — timing y reproducción de GIF

- **BUG FIX (compilación)**: `scheduleMultipleScreenbugs()` referenciaba constantes del `companion object` (`SCREENBUG_START_DELAY_MS`, `TAG`, etc.) sin calificar con `LiveDiscoveryKids.`, al ser una función de extensión top-level fuera de la clase — el proyecto no compilaba (`Unresolved reference`). Corregido calificando todas las referencias.
- **Timing ajustado**: `screenbug_start` ahora se oculta 15 s después de aparecer (antes 5 s); `screenbug` (PNG) aparece inmediatamente al ocultarse `screenbug_start` (antes esperaba 15 s más); `screenbug_end` ahora se oculta 5 s después de mostrarse (antes quedaba visible los 20 s completos de la ventana final del segmento — como es un GIF más corto que loopea, se veía repetirse en bucle todo ese tiempo).
- **Se eliminaron las animaciones de fade in/out del ScreenBug.** Las 3 fases ahora aparecen y desaparecen de forma instantánea (`setBugAlpha()`), sin transición de opacidad.
- **Reproducción de GIF migrada a `GifMovieDrawable.kt` (nuevo), basado en `android.graphics.Movie`** — API nativa del SDK, sin dependencias externas. Se probaron dos librerías de terceros antes de llegar a esta solución:
  - Glide: los GIFs sí animaban, pero con latencia perceptible por el overhead de su pipeline de decode async genérico.
  - `android-gif-drawable`: más liviano en tiempo de ejecución, pero su jar "runtime" (variante multi-release Java 9+) hacía crashear a D8/R8 con un `NullPointerException` interno, tanto en debug (`mergeExtDexDebug`) como en release — un bug del propio toolchain de dexing, irresoluble por configuración (se probaron `packagingOptions`, reglas de proguard y desactivar minificación, sin éxito).
  - `GifMovieDrawable` decodifica el GIF una sola vez (`preloadScreenBugAssets()`, llamado en `onCreate()`) y lo cachea; cada aparición solo reinicia la reproducción (`seekToStart()` + `start()`) sin volver a decodificar. El PNG estático sigue usando `setImageResource()` directo, sin pasar por ningún decoder de GIF.

### 🎨 Contenido

- Nuevo Screenbug (Mayo–Julio 2009).

### ⚠️ Alcance

> Cambios de código en `AspectRatioFrameLayout.kt` (forzado de 4:3 condicional), `DiscoveryKidsLauncherActivity.kt` (ActionBar real, menú de opciones), `LiveDiscoveryKids.kt` (persistencia de `currentSegmentStartMs`, `resumeSavedState()` con `isNewSegment` correcto, timing y reproducción del ScreenBug, `videoContainer` como campo de la Activity), nuevo `GifMovieDrawable.kt`, `activity_launcher.xml` reescrito (sin MenuBar custom), `item_program_config.xml` (colores por atributo de tema), nuevo `menu_launcher.xml`, `themes.xml` (`LauncherTheme` → Material 3 DayNight), `colors.xml` + nuevo `values-night/colors.xml` (esquema MD3 claro/oscuro), nuevos `values/bools.xml` + `values-night/bools.xml`. `build.gradle`: `versionName` a `2009.5.1.0`.

---

## [2009.5.0.0] — 🚀 Release · Era Doki 1.0 · Era 2009 · "Parque Imaginario" — 2026-07-13

> *Release del 13 de julio de 2026. Primera versión de la rama 5.x — arranca la Fase 4 del proyecto ("Parque Imaginario"). Cambio más grande hasta la fecha: Discovery Kids Launcher pasa de ser una pantalla secundaria a ser la Activity de inicio real de la app, con selector de video por programa (SAF, sin renombrar/copiar nada), cantidad de programas configurable (hasta 24) y ya_regresa/continuamos personalizados por programa — todo detrás de un interruptor "Experimental" nuevo en Configuración, desactivado por defecto. Fuera de Experimental: AlertDialog de aviso para videos de 720p+, un motor de video alternativo basado en TextureView para resolver ese problema, y aviso automático de actualización al entrar a la app.*

### 🧪 Experimental — Discovery Kids Launcher como pantalla de inicio

**Nueva sección "Experimental" en Configuración**

- Switch maestro "Habilitar funciones experimentales" (desactivado por defecto). Al cambiarlo se guarda al toque y se muestra un AlertDialog ofreciendo reiniciar la app ahora ("Reiniciar ahora" relanza desde Discovery Kids Launcher y mata el proceso con `Runtime.exit()`) o más tarde (el cambio ya quedó guardado, se aplica la próxima vez que se abra la app).
- Con Experimental **desactivado** (comportamiento por defecto, idéntico al de antes de esta Release): abrir la app pasa directo al canal (4 programas fijos, pro1–pro4.mp4 en Movies), exactamente como siempre.
- Con Experimental **activado**: la app abre en el Discovery Kids Launcher rediseñado en vez de ir directo al canal.

**Discovery Kids Launcher — rediseño completo, ahora es la Activity de inicio real**

- El intent-filter `MAIN`/`LAUNCHER` se mueve de `LiveDiscoveryKids` a `DiscoveryKidsLauncherActivity` en `AndroidManifest.xml`. Con Experimental desactivado, `onCreate()` redirige de inmediato a `LiveDiscoveryKids` y hace `finish()` sin mostrar nada — la Activity es transparente para quien no activó Experimental.
- Diseño nuevo: MenuBar superior fijo con degradado azul→cian (`bg_launcher_menubar.xml`) y el logo (`icon.webp`), fuente `dk_font` en vez de `googlesans` (aplica también dentro de `LiveDiscoveryKids`, pero **no** en `SettingsActivity` ni en `UpdateActivity`, que se quedan en `googlesans`). Ícono de ajustes en la esquina en vez de texto.
- Botón "Iniciar canal" → arranca `LiveDiscoveryKids`. Ícono de ajustes en el MenuBar → `SettingsActivity` (ya accesible desde acá, no solo desde el canal).
- Sección "Programas": ítem "Cantidad de programas" (diálogo numérico, 1–24, predeterminado 4 — `SettingsManager.getProgramCount()`), y una fila por programa (`item_program_config.xml`, inflada en código porque puede haber hasta 24 filas).
- Cada fila permite: elegir el video del programa vía selector de archivos del sistema (Storage Access Framework, `ACTION_OPEN_DOCUMENT`) — **ya no hace falta renombrarlo `pro{N}.mp4` ni copiarlo a la carpeta Movies**; y, para el ya_regresa y el continuamos de ESE programa, un switch "Personalizado" (desactivado por defecto = usa el que trae la app) que al activarse muestra un botón para elegir un video propio.
- Toda Uri elegida se persiste con `ContentResolver.takePersistableUriPermission()` para seguir siendo válida entre reinicios de la app (sin esto, el permiso de lectura de SAF expira al cerrar la app).

**Configuración avanzada de programas — cambios en `LiveDiscoveryKids.kt`**

- El `playlist` fijo de 4 ciclos (Enseguida→Bumper→Comercial→Programa × 4) pasa de `val` a `var`, armado por la nueva `buildPlaylist()` en `onCreate()`: con Experimental desactivado sigue siendo 4 programas; con Experimental activado, se arma un ciclo por cada uno de los N programas elegidos (`SettingsManager.getProgramCount()`, 1–24). `findAvailableProgramIndex()` (Prev/Next) y `totalProgramCount()` se actualizaron para usar esta cantidad dinámica en vez del `4` fijo de antes.
- `resolveProgram()` ahora, con Experimental activado, revisa primero si hay una Uri elegida por el usuario para ese índice (`SettingsManager.getProgramUri()`) antes de caer al comportamiento clásico de buscar `pro{N}.mp4`.
- Nuevas `resolveYaRegresaUri()` / `resolveContinuamosUri()`: devuelven el video personalizado del programa si el usuario activó "Personalizado" para él, o si no, el comportamiento clásico (`ENSEGUIDAS_PRE_COMERCIAL`/`ENSEGUIDA_YA_VOLVEMOS_MAP` indexado por programa). `playCommercial()`, `playCommercialStepPreComercial()` y `resumeCommercialBlock()` se actualizaron para trabajar con `Uri` en vez de un resource id `Int` fijo (`commercialChosenPreComercial`/`commercialChosenYaVolvemos` cambian de tipo).
- **BUG FIX defensivo**: si la cantidad de programas cambia entre sesiones (posible con Experimental activado) y el estado guardado de sesión anterior (`playlistIndex`/`currentProgramIndex`) ya no encaja en el playlist actual, `startChannel()` descarta ese estado y arranca desde cero en vez de arriesgar un índice fuera de rango.

### 🖥️ Compatibilidad de video (NO experimental)

**Motor de video alternativo — TextureView**

- Nuevo `DkVideoView.kt`: capa de abstracción (`FrameLayout` abstracto) con la misma API que ya usaba todo el código contra `videoView` (`setVideoURI`, `start`, `pause`, `stopPlayback`, `seekTo`, `isPlaying`, `currentPosition`, `setOnPreparedListener`, `setOnCompletionListener`) — cero cambios en los call-sites existentes de `LiveDiscoveryKids.kt`.
  - `LegacyVideoView`: envuelve un `VideoView` clásico. Sigue siendo el comportamiento por defecto.
  - `TextureVideoView`: `MediaPlayer` + `TextureView` manejados a mano, con `onMeasure()` calculando el ancho a partir del alto y la relación de aspecto real del video (reproduce el efecto de `layout_width="wrap_content"` que ya tenía `VideoView`, porque `TextureView` por sí sola simplemente estira su contenido).
- Nuevo switch "Usar TextureView" en Configuración → "Compatibilidad de video" (desactivado por defecto). Al cambiarlo se muestra el mismo diálogo de reiniciar ahora/más tarde que Experimental — el tipo de superficie de video se fija una sola vez al crear la Activity, no se puede intercambiar en caliente.
- `activity_main.xml`: el `<VideoView>` fijo se reemplaza por un `<FrameLayout android:id="@+id/videoViewContainer">` vacío; `LiveDiscoveryKids.onCreate()` instancia el `DkVideoView` correcto (`DkVideoView.create()`) y lo agrega ahí en código.

**AlertDialog de resolución alta (720p+)**

- Nueva `checkVideoResolutionAndWarn()`, llamada desde el `onPrepared` de `beginProgramSegment()`: si el programa recién preparado mide 720p o más de alto y todavía se está usando `VideoView` clásico (no TextureView), muestra un AlertDialog explicando que la transmisión podría no funcionar correctamente (el ScreenBug puede quedar oculto detrás del video) y recomendando activar "Usar TextureView". Se muestra una sola vez por sesión.

### 🔔 Aviso de actualización al entrar a la app (NO experimental)

- Nueva `checkForUpdateOnLaunch()` en `LiveDiscoveryKids.onCreate()`: usa el mismo `AppUpdater.checkForUpdate()` que ya usaba Configuración → "Buscar actualizaciones" (respeta el switch "Habilitar versiones Preview"). Si hay una versión más nueva, muestra un AlertDialog propio — **fuera** de Configuración y de `UpdateActivity` — ofreciendo ir al Actualizador o posponerlo. Si no hay novedad o falla la consulta (sin internet, etc.), no muestra nada: nunca interrumpe la reproducción con un error.

### ⚙️ Configuración — reorganización de Configuración

- **ELIMINADO**: la sección "Programación" / ítem "Elegir programas" de `SettingsActivity`. Discovery Kids Launcher ya no se abre desde Configuración — es la Activity de inicio de la app (ver arriba), así que el atajo quedaba duplicado.

### 🎬 Contenido — assets actualizados

- `enseguida1`/`enseguida2` actualizados; `enseguida2` vuelve a la rotación aleatoria de `ENSEGUIDAS_POST_PROGRAMA` (antes solo estaba `enseguida1` en el código, aunque el comentario ya mencionaba a los dos). `enseguida3`/`enseguida4` quedan eliminados.
- Los 4 comerciales (`comercial1`–`comercial4`) y `ya_regresa1`/`ya_regresa2`/`continuamos1`/`continuamos2` actualizados de contenido para la Era 2009/Fase 4 (sin cambios de código — la rotación y el mapeo ya excluían 3/4 desde antes).
- Nuevo logo de la app (`icon.webp`).

> **Alcance:** cambios de código en `AndroidManifest.xml` (intent-filter MAIN/LAUNCHER movido a `DiscoveryKidsLauncherActivity`), `DiscoveryKidsLauncherActivity.kt` (reescritura completa), `SettingsActivity.kt`/`activity_settings.xml` (nuevas secciones Experimental y Compatibilidad de video, eliminada Programación), `SettingsManager.kt` (nuevas keys: Experimental, cantidad de programas, Uri por programa, ya_regresa/continuamos personalizado por programa, TextureView), `LiveDiscoveryKids.kt` (playlist dinámico, `resolveProgram`/`resolveYaRegresaUri`/`resolveContinuamosUri`, chequeo de resolución, aviso de actualización al iniciar, guard de sesión guardada inválida), nuevo `DkVideoView.kt`, nuevo layout `item_program_config.xml`, `activity_launcher.xml` reescrito, `activity_main.xml` (VideoView → placeholder), nuevos drawables (`bg_launcher_menubar.xml`, `ic_play.xml`, `ic_video_pick.xml`) y colores (`dk_launcher_gradient_top/bottom`). `build.gradle`: `versionName` a `2009.5.0.0`.

---


## [2009.4.6.1] — 🚀 Release · Era Doki 1.0 · Era 2009 — 2026-07-11

> *Release del 11 de julio de 2026. Cambio de Era (2008→2009) e implementación del sistema de 3 Screenbug secuenciales. Además, 2 bug fixes críticos: Prev/Next ahora navega en orden real del playlist, y el Actualizador ya no cree que hay versión nueva cuando actualizas de una preview a la release final de la misma versión.*

### 🔧 Bug Fixes Críticos

**AppUpdater — versión con BUILD segment**

- **BUG**: cuando instalabas una preview (versionName ej. `2008.4.6.0.60`) y después sacaba la release final de esa misma versión (versionName `2008.4.6.0.01`), el Actualizador creía que había versión más nueva (4.6.0) < instalada (4.6.0.60), así que no te dejaba actualizar. 
- **CAUSA**: `currentVersionName()` devolvía los 4 segmentos (MAJOR.MINOR.PATCH.BUILD), y el segmento BUILD (60 en preview vs 01 en release) hacía que se comparara como "preview es mayor que release de la misma versión".
- **CORRECCIÓN**: `currentVersionName()` ahora devuelve solo los primeros 3 segmentos (MAJOR.MINOR.PATCH), ignorando BUILD completamente. Así, preview y release de la misma 4.6.0 ambas devuelven "4.6.0", y compareVersions() funciona correctamente.

**LiveDiscoveryKids — Prev/Next navegación en orden del playlist**

- **BUG**: al presionar Prev/Next para cambiar de programa, a veces saltaba al programa equivocado o no respetaba el orden real de reproducción del playlist.
- **CAUSA**: `goToAdjacentProgram()` usaba `indexOfFirst` para buscar el próximo programa en todo el playlist desde el inicio, ignorando la dirección de navegación (Prev o Next). Si estabas reproduciendo Program(3) y presionabas Next, buscaba el primer `PlayItem.Program(0)` en el playlist (índice 3) en vez de continuar hacia adelante, wrapeando si es necesario.
- **CORRECCIÓN**: ahora busca a partir de `playlistIndex` actual en la dirección de la navegación (1 para Next, -1 para Prev), wrapeando correctamente. Prev/Next ahora avanzan/retroceden en orden lógico a través de la lista, no saltando de forma impredecible.

### 🎬 Sistema de 3 Screenbug Secuenciales

**Release 2009.4.6.1 — NUEVO: 3 fases de Screenbug durante el programa**

Implementación de un sistema de 3 imágenes/GIFs diferentes que se muestran en momentos específicos durante la reproducción del programa, reemplazando la lógica simple anterior:

- **Fase 1 — `screenbug_start.gif` (GIF):** Mostrar 20s después de iniciar el programa, ocultar 5s después (duración asumida del GIF). Propósito: animación de "entrada" dinámica del Screenbug.
- **Fase 2 — `screenbug.png` (PNG):** Mostrar 15s después de que `screenbug_start` se oculta (40s totales después de iniciar), ocultar cuando aparece `screenbug_end`. La imagen estática principal del Screenbug, visible la mayor parte del programa.
- **Fase 3 — `screenbug_end.gif` (GIF):** Mostrar 20s antes de que termine el programa, ocultar al terminar. Propósito: animación de "salida" dinámica, dando dinamismo al final.

Timings configurables vía constantes en `LiveDiscoveryKids.Companion` (en ms):
- `SCREENBUG_START_DELAY_MS` = 20.000 (20s)
- `SCREENBUG_START_ESTIMATED_DURATION_MS` = 5.000 (5s, duración asumida del GIF start)
- `SCREENBUG_MID_DELAY_AFTER_START_MS` = 15.000 (15s, delay antes de mostrar el PNG)
- `SCREENBUG_END_SHOW_BEFORE_MS` = 20.000 (20s antes del final)

Nueva función `scheduleMultipleScreenbugs()` que maneja toda la lógica de timings, llamada automáticamente desde `scheduleSegmentLogic()`. Los timings se ajustan si la app se pausa/reanuda, igual que la lógica anterior — no reinician desde cero.

### 🎉Nueva Fase 4 está cerca 5.0.0
- Se agregó **bumper6** como indicando que esta cerca la fase 4.

### 🎨 Cambio de Era

- **Era 2008 → Era 2009:** el segmento de Era en `versionName` cambia de 2008 a 2009 (ej. `2009.4.6.1`). Esto afecta a `versionName`, tags de GitHub, versionCode para Play Store, etc. Los 3 Screenbug comienzan a usarse en esta Era (como mencionó Keyler, "la fase 4 está cerca").

> **Alcance:** cambios de código en `LiveDiscoveryKids.kt` (nueva función `scheduleMultipleScreenbugs()`, nueva función `fadeInBugWithResource()`, constantes de timings del screenbug) y `AppUpdater.kt` (fix de `currentVersionName()` para ignorar BUILD). Los 3 archivos de screenbug (`screenbug_start.gif`, `screenbug.png`, `screenbug_end.gif`) deben estar en `src/main/res/drawable/` o importarse desde `src/main/res/raw/` — ver comentarios en `scheduleMultipleScreenbugs()` para la migración de asset paths. Sin cambios en Activities ni layouts. Cambio de Era: actualizar `versionName` en `build.gradle` a `2009.4.6.1` y `versionCode` según correspondza.

---


## [2008.4.6.0] — 🚀 Release · Era Doki 1.0 · Era 2008 — 2026-07-10

> *Release del 10 de julio de 2026. Dos cambios grandes: `LiveDiscoveryKids.kt` se reunifica en un solo archivo (reversión de la reorganización de la 4.1.0.21), y debuta el Discovery Kids Launcher, una pantalla nueva para elegir qué programas salen al aire.*

### Cambiado

**`LiveDiscoveryKids.kt` — reunificación de los 11 archivos de extensión**

- La Reorganización 4.1.0.21 había repartido el flujo del canal (playlist driver, reproducción de programas, bloque comercial, transiciones de video, resolución de URIs, música de fondo, persistencia de sesión, position tracker, Screenbug, helpers de UI y overlay de debug) en 11 archivos de extensión aparte: `ChannelPlaylist.kt`, `ChannelProgramPlayback.kt`, `ChannelCommercialBlock.kt`, `ChannelVideoTransitions.kt`, `ChannelMediaResolver.kt`, `ChannelBackgroundMusic.kt`, `ChannelSessionState.kt`, `ChannelPositionTracker.kt`, `ChannelScreenBug.kt`, `ChannelUiHelpers.kt` y `ChannelDebugOverlay.kt`.
- Esta Release revierte esa reorganización: los 11 archivos se eliminan del proyecto y todo su código vuelve a vivir en `LiveDiscoveryKids.kt`, cada bloque bajo el comentario de encabezado original de su archivo de origen (a modo de separador de sección).
- Copiado tal cual, **sin cambios de comportamiento** — mismas funciones, misma lógica, mismas visibilidades (`internal`). El único cambio es dónde vive el código.

**Discovery Kids Launcher — nueva pantalla para elegir programas**

- Nueva `DiscoveryKidsLauncherActivity` (`activity_launcher.xml`), con el mismo diseño de lista simple de `SettingsActivity`/`activity_settings.xml`: header con botón Atrás, rótulo de sección gris, e ítems con switch a la derecha.
- Un switch por programa (`pro1.mp4`–`pro4.mp4`), todos activados por defecto. Cada ítem muestra además si el archivo se encontró realmente en la carpeta Movies (misma resolución que `ChannelMediaResolver.resolveProgram()`, duplicada en `checkProgramFileExists()` porque esta Activity no es una instancia de `LiveDiscoveryKids`).
- Se accede desde Configuración → nueva sección "Programación" → ítem "Elegir programas" (mismo patrón que "Buscar actualizaciones" → `UpdateActivity`: `startActivity` simple, sin pasar datos).
- El estado de cada switch se persiste al toque, sin botón "Guardar", vía dos funciones nuevas en `SettingsManager`: `isProgramEnabled(context, index)` / `setProgramEnabled(context, index, enabled)`.
- `LiveDiscoveryKids.playProgram()` ahora consulta `SettingsManager.isProgramEnabled()` antes de reproducir un programa: si está desactivado, lo saltea exactamente por el mismo camino que ya usaba para un archivo `.mp4` faltante (`playlistIndex++` + `advance()`).
- `findAvailableProgramIndex()` (navegación Prev/Next) también respeta el estado activado/desactivado, además de la existencia del archivo — Prev/Next nunca aterriza en un programa desactivado.
- Desactivar los 4 programas a la vez no rompe nada: el canal sigue el mismo fallback que ya existía si faltaban los 4 archivos — repite Enseguida → Bumper → Comercial en loop sin encontrar programa disponible.

> **Alcance:** cambios de código en `LiveDiscoveryKids.kt` (reunificado, sin los 11 archivos de extensión), `DiscoveryKidsLauncherActivity.kt` (nuevo), `activity_launcher.xml` (nuevo), `SettingsManager.kt` (nuevas funciones de per-programa), `SettingsActivity.kt`/`activity_settings.xml` (nueva sección "Programación") y `AndroidManifest.xml` (nueva Activity). Sin cambios en `AppUpdater.kt` ni en `UpdateActivity.kt`. Sin cambio de contenido ni de Era.

---


## [2008.4.5.0.50-preview] — 🧪 Preview · Era Doki 1.0 · Era 2008 — 2026-07-07

> *Preview para el 7 de julio de 2026. Segundo rediseño de `UpdateActivity`: ahora calca la pantalla nativa de Android "Configuración → Sistema → Actualización del sistema" en vez del diseño tipo diálogo centrado del release anterior.*

### Cambiado

**`UpdateActivity` / `activity_update.xml` — rediseño visual 2, calco de la pantalla nativa de Android**

- El bloque de estado (ícono, título, subtítulos) deja de estar centrado: ahora queda alineado a la izquierda con el mismo padding de 20dp que usan los ítems de `activity_settings.xml` — igual que "Tu sistema está actualizado" en la captura de referencia de Android.
- El `ProgressBar` indeterminado grande (`progressIndeterminate`) y la `ProgressBar` gruesa de descarga (`progressDownload`, dentro de `groupDownloadProgress`) se eliminan. En su lugar hay una única barra fina de 3dp (`progressThin`) pegada debajo del título, visible solo en los estados CHECKING (indeterminada) y DOWNLOADING (determinada, 0–100%) — el mismo efecto visual que la línea celeste fina que muestra Android nativo mientras busca actualizaciones.
- `txtUpdateMessage` se reemplaza por dos líneas de subtítulo (`txtUpdateSubtitle1`/`txtUpdateSubtitle2`): la primera siempre muestra la versión instalada ("Versión instalada: X", equivalente a "Versión de Android: Q" de la captura), la segunda es contextual según el estado (mensaje de progreso, versión nueva disponible, o el error).
- Los dos `Button` planos (`btnUpdatePrimary`/`btnUpdateSecondary`, estilo diálogo) se reemplazan por renglones de lista clickeables (`itemUpdatePrimary`/`itemUpdateSecondary`) con una flecha ">" a la izquierda (`ic_chevron_right.xml`, nuevo), idénticos en estructura a un ítem de `activity_settings.xml` — así se ve "Comprobar actualizaciones" en la captura de referencia.
- Durante CHECKING y DOWNLOADING no se muestra ningún renglón de acción (igual que la pantalla nativa, que oculta "Comprobar actualizaciones" mientras la comprobación está en curso).
- `UpdateActivity.kt` se actualizó para poblar estas vistas nuevas; el enum `UpdateScreenState` y el flujo de estados (CHECKING → UP_TO_DATE / AVAILABLE → DOWNLOADING → INSTALLING, o ERROR en cualquier paso) no cambiaron — solo qué vistas se muestran y cómo. Sin cambios en `AppUpdater.kt`.

> **Alcance:** cambios de código en `UpdateActivity.kt`, `activity_update.xml` y el nuevo `ic_chevron_right.xml`. Sin cambios en `AppUpdater.kt`, `SettingsActivity.kt`, `LiveDiscoveryKids.kt` ni en el resto del canal. Al ser una Preview de solo UI, no hay cambio de Era ni de contenido.

---


## [2008.4.5.0] — 🚀 Release · Era Doki 1.0 · Era 2008 — 2026-07-06

> *Release del 6 de julio de 2026. Cambio de Era — los 4 comerciales standalone y el par ya_regresa4/continuamos4 evolucionan a la Era 2008.*

### Cambio de Era — 2007 → 2008

**4 comerciales actualizados a la Era 2008**
- `comercial1.mp4` a `comercial4.mp4` se reemplazaron por versiones de la Era 2008.

**`ya_regresa4.mp4` y `continuamos4.mp4` actualizados a la Era 2008**
- El cuarto par de transición comercial (pre/post) se actualizó a la estética de la Era 2008. Los pares 1–3 (`ya_regresa1–3`/`continuamos1–3`) no se modificaron en esta Release.
- Sin cambios en el mapeo `ya_regresa → continuamos` ni en la lógica de asignación determinística por programa (`ENSEGUIDAS_PRE_COMERCIAL[currentProgramIndex % size]`, ver 2005.4.0.1).

- El segmento de Era del `versionName` pasa de `2007` a `2008` a partir de esta Release (`2008.4.5.0`). Sin impacto en el Actualizador: `AppUpdater.currentVersionName()` descarta el primer segmento del `versionName` sin importar su valor.

> **Alcance:** cambios de contenido únicamente — 4 comerciales, `ya_regresa4` y `continuamos4`. Sin cambios de código en `ChannelPlaylist.kt`, `ChannelCommercialBlock.kt`, `AppUpdater.kt` ni en el resto del canal. El Screenbug, los bumpers y los demás pares ya_regresa/continuamos no se modificaron.

---


## [2007.4.4.0] — 🚀 Release · Era Doki 1.0 · Era 2007 — 2026-07-03

> *Release del 3 de julio de 2026. El Actualizador migra su descarga de DownloadManager a OkHttp — detecta con certeza cuándo termina la descarga y expone bytes descargados/totales — y `UpdateActivity` se rediseña para coincidir con el lenguaje visual de SettingsTheme. Además, el Screenbug pasa a la variante de septiembre de 2007.*

### Cambiado

**Descarga del Actualizador — de `DownloadManager` a OkHttp**

- `AppUpdater.downloadAndInstall()` ya no encola la descarga en `DownloadManager`: ahora abre el `.apk` con OkHttp y lee el `ResponseBody` en un loop manual (bloques de 8 KB) escribiendo directo a un `FileOutputStream` en `getExternalFilesDir(DIRECTORY_DOWNLOADS)`.
- Antes, saber cuándo terminaba la descarga dependía de dos mecanismos separados: un `Thread` sondeando `DownloadManager.Query` cada 300 ms para el progreso, y un `BroadcastReceiver` de `ACTION_DOWNLOAD_COMPLETE` para el resultado final — con posibilidad de que ambos quedaran desincronizados. Con OkHttp, todo corre en el mismo hilo: el progreso se reporta en cada bloque leído, y en cuanto el loop de lectura termina sin excepción, la descarga está garantizada completa — sin receiver aparte, sin condición de carrera.
- `onProgress` cambia de firma: pasa de `(percent: Int) -> Unit` a `(percent: Int, bytesDownloaded: Long, bytesTotal: Long) -> Unit`, para que la UI pueda mostrar también el tamaño descargado/total, no solo el porcentaje.
- `trackDownloadProgress()` y `registerInstallReceiver()` fueron eliminadas — ya no hace falta sondear ni escuchar broadcasts, OkHttp resuelve ambos casos en el mismo flujo secuencial.
- Requiere agregar la dependencia `com.squareup.okhttp3:okhttp` en `build.gradle` (no incluida en el paquete de fuentes de la app).

**`UpdateActivity` / `activity_update.xml` — rediseño visual, mismo lenguaje que Configuración**

- El layout de `UpdateActivity` se rediseñó para coincidir con el estilo de `SettingsTheme`/`activity_settings.xml` (lista simple neutra estilo Android Settings) en vez del diseño genérico anterior: mismo header (botón Atrás + título 24sp), ícono de actualización (`ic_update`, nuevo) centrado sobre el título/mensaje.
- La pantalla de descarga ahora se parece a la nativa de Android para "Actualización del sistema": porcentaje grande (30sp) arriba de la barra, tamaño descargado/total en formato "X MB de Y MB" debajo, barra horizontal con `progressTint`/`progressBackgroundTint` en los colores de la paleta (`dk_accent`/`dk_stroke`) en vez del estilo Holo por defecto.
- Los dos `Button` de ancho completo del diseño anterior se reemplazaron por botones planos alineados a la derecha (`borderlessButtonStyle`), imitando los botones de diálogo nativos de Android — secundario en `dk_text_secondary`, primario en `dk_accent` y negrita.
- Si el servidor no envía `Content-Length` (`bytesTotal <= 0`), la barra pasa a modo indeterminado en vez de mostrar un porcentaje inválido.

### Contenido — Screenbug actualizado a la variante de septiembre de 2007

- El Screenbug se reemplazó por la variante correspondiente a septiembre de 2007, dentro de la misma Era 2007 iniciada en la Release 4.3.0.
- Es un reemplazo directo del archivo de imagen (`res/drawable/screenbug.webp`) — sin cambios de lógica en `ChannelScreenBug.kt` ni en `ChannelProgramPlayback.kt` (el cálculo de cuándo mostrarlo/ocultarlo no se modificó).

> **Alcance:** cambios de código en `AppUpdater.kt`, `UpdateActivity.kt`, `activity_update.xml` y el nuevo `ic_update.xml`. Cambio de contenido: Screenbug. Sin cambios en `LiveDiscoveryKids.kt`, `SettingsActivity.kt` ni en el resto del canal. Pendiente agregar la dependencia de OkHttp a `build.gradle` si el proyecto todavía no la tiene.

---


## [2007.4.3.1] — 🐛 Release Fixer · Era Doki 1.0 · Era 2007 — 2026-07-01

> *Release Fixer del 1 de julio de 2026. Corrige el bug reportado en 4.3.0 y anterior: Prev/Next saltaba al programa equivocado si se tocaba antes de que cualquier programa hubiera arrancado en la sesión.*

### Corregido

**Prev/Next saltaba al programa equivocado si se tocaba antes de que arrancara cualquier programa en la sesión**

**Causa raíz**

`goToAdjacentProgram()` calculaba el programa destino a partir de `currentProgramIndex`, que arranca en `0` por defecto al iniciar la Activity. Si Keyler tocaba Prev o Next durante la Enseguida/Bumper/Comercial **inicial** — es decir, antes de que `Program(0)` (`pro1.mp4`) hubiera salido al aire por primera vez en la sesión — `findAvailableProgramIndex()` no tenía forma de distinguir "todavía no arrancó ningún programa" de "el programa 0 ya se reprodujo", y trataba ambos casos igual. Resultado: **Next** saltaba directo al programa 1 (saltándose el 0), y **Prev** caía en el programa 3 en vez de ir al 0 — en ambos casos, no era el siguiente/anterior real en el orden del `PlayItem`.

**Solución**

Se agregó `hasPlayedAnyProgram`, un flag que solo pasa a `true` dentro de `playProgram()`, en el momento exacto en que un programa realmente arranca. Mientras siga en `false`, `goToAdjacentProgram()` usa un punto de partida "virtual" para el wraparound de `findAvailableProgramIndex()` — distinto según la dirección, ya que no es simétrico: `-1` para Next (así el primer candidato evaluado es el programa 0) y `0` para Prev (así el primer candidato evaluado, retrocediendo, es el programa 3). El guard que evita el no-op (`target == currentProgramIndex`) también se ajustó para no dispararse falsamente en este estado inicial. El flag se persiste en la sesión guardada (`PREF_HAS_PLAYED_PROGRAM`), con un valor por defecto retrocompatible para sesiones guardadas por versiones anteriores a 4.3.1 (se infiere `true` si el ítem guardado era `"program"` o `"commercial"`).

| Antes del fix | Después del fix |
|---|---|
| `currentProgramIndex = 0` (valor por defecto, ningún programa reprodujo aún) | `hasPlayedAnyProgram = false` hasta que `playProgram()` arranca un programa real |
| Next antes de Program(0) → salta al programa 1 (salteando el 0) | Next antes de Program(0) → va correctamente al programa 0 |
| Prev antes de Program(0) → cae en el programa 3 sin razón aparente | Prev antes de Program(0) → va correctamente al programa 3 (último, por wraparound) |

> **Alcance:** este fix solo afecta `ChannelPlaylist.kt` (`goToAdjacentProgram()`, `findAvailableProgramIndex()` sin cambios de firma), `ChannelProgramPlayback.kt` (`playProgram()`) y `ChannelSessionState.kt` (persistencia/restauración del nuevo flag). No hay cambios de contenido ni en el resto de la lógica del canal (`advance()`, bloque comercial, Screenbug).

---


## [2007.4.3.0] — 🚀 Release · Era Doki 1.0 · Era 2007 — 2026-06-29

> *Cambio de Era — los 4 comerciales, los clips ya_regresa/continuamos y el Screenbug evolucionan a la Era 2007. El Actualizador estrena `UpdateActivity`, una pantalla dedicada con barra de progreso en vivo, reemplazando los diálogos de siempre.*

### Agregado

**`UpdateActivity` — pantalla dedicada para el Actualizador, reemplaza los AlertDialog**
- "Buscar actualizaciones" en Configuración ya no consulta a `AppUpdater` ni muestra diálogos desde `SettingsActivity`: ahora solo hace `startActivity(UpdateActivity::class.java)`. Es `UpdateActivity` quien hace la consulta, pide confirmación, descarga y muestra el resultado.
- Seis estados manejados con visibilidad de vistas (sin `ViewFlipper`, sin diálogos): `CHECKING`, `UP_TO_DATE`, `AVAILABLE`, `DOWNLOADING`, `INSTALLING`, `ERROR`. Cada uno con su propio título, mensaje y botones.
- `AppUpdater.downloadAndInstall()` ahora acepta `onProgress: (percent: Int) -> Unit`, además de `onStarted`/`onCompleted`/`onFailed`. Nueva función privada `trackDownloadProgress()`: hilo en segundo plano que sondea `DownloadManager.Query` cada 300 ms (`COLUMN_BYTES_DOWNLOADED_SO_FAR` / `COLUMN_TOTAL_SIZE_BYTES`) y reporta el porcentaje a la UI vía un `Handler` sobre el main looper, hasta detectar `STATUS_SUCCESSFUL` o `STATUS_FAILED`.
- `registerInstallReceiver()` ahora también recibe `onCompleted`/`onFailed` para informarle a `UpdateActivity` el resultado final, además de abrir el instalador como siempre.
- Nuevo layout `activity_update.xml`: barra superior con botón "Volver", título, mensaje, `ProgressBar` indeterminada (chequeo) y determinada (descarga + porcentaje), y dos botones (`btnUpdatePrimary`/`btnUpdateSecondary`) que cambian de texto/acción según el estado.
- La instalación sigue abriéndose automáticamente al completar la descarga (mismo mecanismo de `FileProvider` + `Intent.ACTION_VIEW`). Es el máximo de automatización posible: desde Android 8, el sistema exige confirmación manual del usuario para instalar un APK, ese paso queda fuera del control de la app por diseño de la plataforma.

### Eliminado

**`AppUpdater.showUpdateAvailableDialog()` y `showInfoDialog()`**
- Removidos junto con el import de `AlertDialog` en `AppUpdater.kt` — esa responsabilidad de UI pasó por completo a `UpdateActivity`. `SettingsActivity` perdió también `isCheckingUpdate` y el `CheckCallback` que manejaba inline; ya no hace falta, todo vive en la nueva Activity.

### Cambio de Era — 2006 → 2007

**Comerciales, ya_regresa, continuamos y Screenbug actualizados a la Era 2007**
- Los 4 comerciales standalone (`comercial1`–`comercial4`), los 2 clips `ya_regresa` y los 2 `continuamos` se reemplazaron por versiones de la Era 2007.
- El Screenbug pasó del logo de la Era 2006 al de la Era 2007.
- Todos los reemplazos son a nivel de archivo de video/imagen — sin cambios de lógica en `ChannelPlaylist.kt`, `ChannelCommercialBlock.kt` ni `ChannelScreenBug.kt`.
- El segmento de Era del `versionName` pasa de `2006` a `2007` a partir de esta Release (`2007.4.3.0`). Sin impacto en el Actualizador: `AppUpdater.currentVersionName()` descarta el primer segmento del `versionName` sin importar su valor.

> **Alcance:** cambios de código en `AppUpdater.kt`, `UpdateActivity.kt` (nuevo), `activity_update.xml` (nuevo) y `SettingsActivity.kt`. Cambios de contenido: 4 comerciales, 4 clips y el Screenbug. Sin cambios en `LiveDiscoveryKids.kt` ni en el resto del canal. Pendiente registrar `UpdateActivity` en `AndroidManifest.xml`.

---


## [2006.4.2.1] — 🐛 Release Fixer · Era Doki 1.0 · Era 2006 — 2026-06-27

> *Release Fixer del 27 de junio de 2026. Corrige un bug crítico en el Actualizador: nunca detectaba versiones nuevas sin importar el tag publicado en GitHub.*

### Corregido

**El Actualizador siempre creía estar al día, sin importar el tag publicado en GitHub**

**Causa raíz**

Keyler etiqueta los releases en GitHub con el esquema corto `MAJOR.MINOR.PATCH[.BUILD]` (ej. tag `v4.2.1`, `v4.2.0`), pero `AppUpdater.currentVersionName()` devolvía el `versionName` **completo** instalado, que incluye el segmento de Era al inicio (ej. `2006.4.2.0`). Al comparar ambos directamente en `compareVersions()`, el primer segmento del tag (`4`) se comparaba contra el primer segmento del `versionName` (`2006`) — como `4 < 2006`, el Actualizador concluía que el release remoto era más viejo en **todos los casos**, sin importar qué tag hubiera publicado Keyler en GitHub.

**Solución**

`currentVersionName()` ahora descarta el primer segmento (la Era, fija para todo el esquema de versionado del proyecto) antes de comparar, dejando el `versionName` local en el mismo formato corto `MAJOR.MINOR.PATCH[.BUILD]` que usan los tags de GitHub. `compareVersions()` queda sin cambios — ahora compara dos versiones en el mismo esquema, como siempre debió ser.

| Antes del fix | Después del fix |
|---|---|
| `local = "2006.4.2.0"` (versionName completo) | `local = "4.2.0"` (sin el segmento de Era) |
| `compareVersions("4.2.1", "2006.4.2.0")` → `4 - 2006 < 0` → "ya estás al día" (incorrecto) | `compareVersions("4.2.1", "4.2.0")` → `4 - 4 = 0`, sigue comparando, `2 - 2 = 0`, sigue, `1 - 0 > 0` → "hay actualización" (correcto) |

> **Alcance:** este fix solo afecta `AppUpdater.kt` (`currentVersionName()` y el doc-comment de `compareVersions()`). No hay cambios en `SettingsActivity`, `activity_settings.xml`, ni en ningún otro archivo del canal. Sigue dependiendo de que Keyler mantenga la convención: tag de GitHub = `versionName` sin el segmento de Era inicial.

---

## [2006.4.2.0] — 🚀 Release · Era Doki 1.0 · Era 2006 — 2026-06-26

> *Release estable del 26 de junio de 2026. Agrega "Habilitar versiones Preview" al Actualizador (desactivado por defecto), corrige el texto del valor predeterminado de "Forzar 4:3", e incluye un Actualizador integrado accesible desde Configuración. Además, reorganiza completamente `LiveDiscoveryKids.kt` (~1770 líneas) en 11 archivos separados por responsabilidad — cambio puramente organizativo verificado función por función sin alteraciones de comportamiento.*

### Agregado

**"Habilitar versiones Preview" — nuevo switch en Configuración → Actualizaciones**
- Nuevo switch en `SettingsActivity`/`activity_settings.xml`, **desactivado por defecto**. Persistido vía `SettingsManager.isPreviewUpdatesEnabled()` / `setPreviewUpdatesEnabled()`.
- Desactivado (default): `AppUpdater` solo considera el último release **estable** del repo (equivalente a `GET /releases/latest`, que GitHub ya filtra para excluir prereleases).
- Activado: `AppUpdater` consulta `GET /releases` (lista completa, la más reciente primero) y toma el primer release sin importar si es estable o Preview (`prerelease: true`), permitiendo que "Buscar actualizaciones" instale una Preview.
- `AppUpdater.checkForUpdate()` ahora lee este switch al iniciar la consulta y elige el endpoint correspondiente; el resto del flujo (comparación de versión, descarga, instalación) no cambia.

### Corregido

**Texto de "Forzar 4:3" en Configuración no coincidía con el valor predeterminado real**
- La descripción del switch decía "(Predeterminado: Activado)" cuando el valor predeterminado real en `SettingsManager.DEFAULT_FORCE_ASPECT_RATIO` siempre fue `false` (Desactivado) — desde que la opción se simplificó a un alternar directo de `layoutParams`. Corregido el texto en `activity_settings.xml` a "(Predeterminado: Desactivado)"; también se corrigió un comentario desactualizado en `applySettings()` que documentaba el comportamiento anterior (ON por defecto).

### Reorganizado

**`LiveDiscoveryKids.kt` dividido en 11 archivos por responsabilidad — sin cambios de comportamiento**

> Han pasado 10 semanas desde el primer release del proyecto; con `LiveDiscoveryKids.kt` en ~1770 líneas, era tiempo de reorganizar todo el código del canal antes de que siguiera creciendo.

- El archivo, que concentraba absolutamente todo el flujo del canal en una sola clase de ~1770 líneas, se dividió en **funciones de extensión** de `LiveDiscoveryKids` repartidas en archivos nuevos por responsabilidad. Se eligió este enfoque (en vez de clases separadas con su propio estado) porque preserva exactamente el mismo estado de instancia y el mismo grafo de llamadas — el riesgo de introducir un bug de comportamiento es, por diseño, nulo.
- `LiveDiscoveryKids.kt` ahora contiene únicamente: las propiedades de instancia, el `companion object` (constantes y listas de recursos), y los métodos de ciclo de vida de `Activity` (`onCreate`, `onPause`, `onResume`, `onStop`, `onDestroy`, `dispatchTouchEvent`, `onRequestPermissionsResult`) — quedó en ~560 líneas.
- Archivos nuevos, cada uno con las funciones que le corresponden:

| Archivo | Contenido |
|---|---|
| `ChannelPlaylist.kt` | `advance`, `playBumper`, `playEnseguida`, `playStandaloneCommercial`, `goToAdjacentProgram`, `findAvailableProgramIndex` |
| `ChannelProgramPlayback.kt` | `playProgram`, `beginProgramSegment`, `scheduleSegmentLogic`, `calcBreaks` |
| `ChannelCommercialBlock.kt` | `playCommercial`, `playCommercialStepPreComercial`, `resumeCommercialBlock` |
| `ChannelVideoTransitions.kt` | `playUri`, `playUriWithTransition`, `resumeUriWithSeek` |
| `ChannelMediaResolver.kt` | `resolveProgram`, `rawUri` |
| `ChannelBackgroundMusic.kt` | `startBgMusic`, `stopBgMusic` |
| `ChannelSessionState.kt` | `startChannel`, `saveChannelState`, `showResumeDialog`, `resumeSavedState`, `clearSavedState`, `showExitConfirmationDialog` |
| `ChannelPositionTracker.kt` | `startPositionTracker`, `stopPositionTracker`, `post`, `cancelAllTasks` |
| `ChannelScreenBug.kt` | `fadeInBug`, `fadeOutBug`, `setBugAlpha` |
| `ChannelUiHelpers.kt` | `showNavButtons`, `resetNavHideTimer`, `requestStoragePermission`, `goFullscreen` |
| `ChannelDebugOverlay.kt` | `applySettings`, `setupDebugInfo`, `startRamMonitor`, `displayInfo` |

- Las propiedades y funciones que antes eran `private` pasaron a `internal` — requisito de Kotlin para que una función de extensión en un archivo distinto pueda acceder a los miembros de la clase. Sigue sin haber ninguna API pública nueva fuera del módulo de la app.

> **Alcance:** este cambio es exclusivamente organizativo. Las 40 funciones movidas se verificaron una por una contra el código original — 39 son carácter por carácter idénticas (descontando comentarios y espaciado); la única diferencia es una anotación de tipo de retorno explícita en `rawUri()` (`: Uri`) que antes quedaba inferida, sin ningún efecto en tiempo de ejecución. El companion object completo y los 7 métodos de ciclo de vida de `Activity` también se verificaron idénticos.

---



## [2006.4.2.0.21-preview] — Preview · Era Doki 1.0 · Era 2006 — 2026-06-25

> *Preview para el 25 de junio de 2026. Agrega "Habilitar versiones Preview" al Actualizador, corrige el texto del valor predeterminado de Forzar 4:3, y reorganiza todo el código de LiveDiscoveryKids.kt en archivos separados por responsabilidad (10 semanas desde el primer release).*

### Agregado

**"Habilitar versiones Preview" — nuevo switch en Configuración → Actualizaciones**
- Nuevo switch en `SettingsActivity`/`activity_settings.xml`, **desactivado por defecto**. Persistido vía `SettingsManager.isPreviewUpdatesEnabled()` / `setPreviewUpdatesEnabled()`.
- Desactivado (default): `AppUpdater` solo considera el último release **estable** del repo (equivalente a `GET /releases/latest`, que GitHub ya filtra para excluir prereleases).
- Activado: `AppUpdater` consulta `GET /releases` (lista completa, la más reciente primero) y toma el primer release sin importar si es estable o Preview (`prerelease: true`), permitiendo que "Buscar actualizaciones" instale una Preview.
- `AppUpdater.checkForUpdate()` ahora lee este switch al iniciar la consulta y elige el endpoint correspondiente; el resto del flujo (comparación de versión, descarga, instalación) no cambia.

### Corregido

**Texto de "Forzar 4:3" en Configuración no coincidía con el valor predeterminado real**
- La descripción del switch decía "(Predeterminado: Activado)" cuando el valor predeterminado real en `SettingsManager.DEFAULT_FORCE_ASPECT_RATIO` siempre fue `false` (Desactivado) — desde que la opción se simplificó a un alternar directo de `layoutParams`. Corregido el texto en `activity_settings.xml` a "(Predeterminado: Desactivado)"; también se corrigió un comentario desactualizado en `applySettings()` que documentaba el comportamiento anterior (ON por defecto).

### Reorganizado

**`LiveDiscoveryKids.kt` dividido en 11 archivos por responsabilidad — sin cambios de comportamiento**

> Han pasado 10 semanas desde el primer release del proyecto; con `LiveDiscoveryKids.kt` en ~1770 líneas, era tiempo de reorganizar todo el código del canal antes de que siguiera creciendo.

- El archivo, que concentraba absolutamente todo el flujo del canal en una sola clase de ~1770 líneas, se dividió en **funciones de extensión** de `LiveDiscoveryKids` repartidas en archivos nuevos por responsabilidad. Se eligió este enfoque (en vez de clases separadas con su propio estado) porque preserva exactamente el mismo estado de instancia y el mismo grafo de llamadas — el riesgo de introducir un bug de comportamiento es, por diseño, nulo.
- `LiveDiscoveryKids.kt` ahora contiene únicamente: las propiedades de instancia, el `companion object` (constantes y listas de recursos), y los métodos de ciclo de vida de `Activity` (`onCreate`, `onPause`, `onResume`, `onStop`, `onDestroy`, `dispatchTouchEvent`, `onRequestPermissionsResult`) — quedó en ~560 líneas.
- Archivos nuevos, cada uno con las funciones que le corresponden:

| Archivo | Contenido |
|---|---|
| `ChannelPlaylist.kt` | `advance`, `playBumper`, `playEnseguida`, `playStandaloneCommercial`, `goToAdjacentProgram`, `findAvailableProgramIndex` |
| `ChannelProgramPlayback.kt` | `playProgram`, `beginProgramSegment`, `scheduleSegmentLogic`, `calcBreaks` |
| `ChannelCommercialBlock.kt` | `playCommercial`, `playCommercialStepPreComercial`, `resumeCommercialBlock` |
| `ChannelVideoTransitions.kt` | `playUri`, `playUriWithTransition`, `resumeUriWithSeek` |
| `ChannelMediaResolver.kt` | `resolveProgram`, `rawUri` |
| `ChannelBackgroundMusic.kt` | `startBgMusic`, `stopBgMusic` |
| `ChannelSessionState.kt` | `startChannel`, `saveChannelState`, `showResumeDialog`, `resumeSavedState`, `clearSavedState`, `showExitConfirmationDialog` |
| `ChannelPositionTracker.kt` | `startPositionTracker`, `stopPositionTracker`, `post`, `cancelAllTasks` |
| `ChannelScreenBug.kt` | `fadeInBug`, `fadeOutBug`, `setBugAlpha` |
| `ChannelUiHelpers.kt` | `showNavButtons`, `resetNavHideTimer`, `requestStoragePermission`, `goFullscreen` |
| `ChannelDebugOverlay.kt` | `applySettings`, `setupDebugInfo`, `startRamMonitor`, `displayInfo` |

- Las propiedades y funciones que antes eran `private` pasaron a `internal` — requisito de Kotlin para que una función de extensión en un archivo distinto pueda acceder a los miembros de la clase. Sigue sin haber ninguna API pública nueva fuera del módulo de la app.

> **Alcance:** este cambio es exclusivamente organizativo. Las 40 funciones movidas se verificaron una por una contra el código original — 39 son carácter por carácter idénticas (descontando comentarios y espaciado); la única diferencia es una anotación de tipo de retorno explícita en `rawUri()` (`: Uri`) que antes quedaba inferida, sin ningún efecto en tiempo de ejecución. El companion object completo y los 7 métodos de ciclo de vida de `Activity` también se verificaron idénticos.

---



## [2006.4.2.0.20-preview] — Preview · Era Doki 1.0 · Era 2006 — 2026-06-24
> *Preview para el 24 de junio de 2026. Agrega un Actualizador integrado: desde Configuración, "Buscar actualizaciones" consulta el último release de GitHub, y si hay una versión más nueva descarga el `.apk` y abre el instalador del sistema.*

### Agregado

**Actualizador (`AppUpdater.kt`) — nuevo, accesible desde Configuración**
- Nuevo `object AppUpdater`: consulta la API de GitHub Releases (`/repos/keylerperales07-commits/DiscoveryKidsChannel/releases/latest`) en un hilo en background, lee `tag_name` (la versión del release) y la URL del primer asset `.apk` adjunto (`assets[].browser_download_url`).
- `compareVersions()` compara la versión remota contra la instalada (`versionName` vía `PackageManager`) segmento por segmento de forma numérica — no alfabética, así `4.10` se reconoce mayor que `4.9` — compatible con el esquema `YYYY.MAJOR.MINOR.PATCH[.BUILD]` del proyecto. El sufijo `-preview` se descarta antes de comparar.
- Si hay una versión más nueva con `.apk` adjunto: `AlertDialog` de confirmación → `downloadAndInstall()` encola la descarga con `DownloadManager` (hacia `getExternalFilesDir(DIRECTORY_DOWNLOADS)`) → un `BroadcastReceiver` de `ACTION_DOWNLOAD_COMPLETE` dispara `openInstaller()` al terminar, que abre el instalador del sistema vía `Intent.ACTION_VIEW` + `FileProvider`.
- Si ya está actualizado, si el release no tiene `.apk` adjunto, o si falla la consulta (sin red, etc.), se informa con un `AlertDialog` simple — nunca queda la app en un estado roto ni reintenta sola.
- El Actualizador **nunca corre automáticamente**: solo se activa al tocar "Buscar actualizaciones" en Configuración, una acción explícita del usuario.

**`SettingsActivity` / `activity_settings.xml` — nueva sección "Actualizaciones"**
- Item "Buscar actualizaciones" (acción inmediata, sin switch ni diálogo de valor): delega todo el trabajo en `AppUpdater.checkForUpdate()`, se deshabilita mientras la consulta está en curso para evitar toques repetidos, y muestra el resultado con los diálogos de `AppUpdater`.

**`AndroidManifest.xml` — permisos y `FileProvider` para el Actualizador**
- Nuevos permisos: `INTERNET` (consultar GitHub) y `REQUEST_INSTALL_PACKAGES` (abrir el instalador del `.apk` descargado; en Android 8+ el sistema solicita habilitar "Instalar apps desconocidas" la primera vez).
- Nuevo `<provider>` `androidx.core.content.FileProvider` (`${applicationId}.fileprovider`), con `res/xml/file_paths.xml` exponiendo únicamente la subcarpeta `Download/` de los archivos privados de la app — necesario porque Android 7+ prohíbe pasar URIs `file://` directas entre apps.

> **Nota:** el `.apk` debe estar adjunto como asset del release de GitHub para que el Actualizador lo detecte; un release sin `.apk` adjunto se informa como error, no se asume ni se intenta otra fuente.

---


## [2006.4.1.1] — Bug Fix · Era Doki 1.0 · Era 2006 — 2026-06-23

> *Corrige dos bugs relacionados: el Screenbug reiniciaba su cuenta de aparición cada vez que la app volvía de segundo plano o de un cambio de Activity (ej. abrir Configuración), y los clips no-programa (bumper, enseguida, comercial) se reiniciaban desde el principio en la misma situación, en vez de reanudarse donde estaban.*

### Corregido

**Screenbug se reiniciaba al volver de segundo plano o cambiar de Activity**
- Causa raíz: `scheduleSegmentLogic()` calculaba `elapsed = segmentStartMs - currentSegmentStartMs` para decidir si el Screenbug ya debía estar visible, pero `currentSegmentStartMs` se sobreescribía con el mismo valor de `segmentStartMs` **antes** de hacer ese cálculo, dando siempre `elapsed = 0`. El Screenbug entonces creía que el segmento siempre acababa de empezar, sin importar cuánto tiempo real hubiera transcurrido.
- `scheduleSegmentLogic()` ahora recibe un parámetro `isNewSegment: Boolean`. Solo actualiza `currentSegmentStartMs` cuando el segmento es realmente nuevo (arranque de programa, tras un corte comercial, restauración de sesión completa). Al reanudar el mismo segmento tras backgrounding (`isNewSegment = false`), conserva el valor original y calcula `elapsed` correctamente contra él.
- `beginProgramSegment()` ahora también recibe `isNewSegment` (default `true`), propagado a `scheduleSegmentLogic()`. La única llamada que pasa `isNewSegment = false` es la de `onResume()` al reanudar un programa pausado por lifecycle.

**Bumper / Enseguida / Comercial se reiniciaban desde el principio al volver de segundo plano**
- Causa raíz: `onResume()` reanudaba correctamente un programa en curso (vía `beginProgramSegment()` con la posición guardada), pero para cualquier otro tipo de ítem activo (bumper, enseguida, StandaloneCommercial, o cualquiera de los 3 pasos del bloque comercial: ya_regresa → comercial → continuamos) simplemente llamaba `advance()`, que siempre arranca el ítem desde cero.
- El position tracker (antes exclusivo de programas) ahora corre durante **cualquier** clip en reproducción, guardando su posición en `currentClipPositionMs` cuando no se está en un segmento de programa.
- `playUriWithTransition()` (usado por bumper, enseguida, StandaloneCommercial, y los pasos comercial/continuamos del bloque comercial) ahora registra en todo momento qué Uri está sonando (`currentClipUri`) y cómo continuar el flujo al terminar (`currentClipOnComplete`).
- Nueva función `resumeUriWithSeek()`: reanuda un clip no-programa exactamente en la posición guardada usando `seekTo()` dentro de `onPrepared()` (mismo patrón ya usado para programas, necesario porque Android puede liberar el surface del `VideoView` en segundo plano), en vez de reiniciarlo desde el principio.
- El bloque comercial (`playCommercial()`) ahora trackea en qué paso está (`commercialStep`: `PRE_COMERCIAL` / `COMERCIAL` / `POST_COMERCIAL`) y qué recursos eligió cada paso (`commercialChosenPreComercial/Commercial/YaVolvemos`), promovidos de variables locales a propiedades de instancia. Nueva función `resumeCommercialBlock()` reconstruye el paso exacto donde se quedó el bloque sin volver a sortear ningún clip.
- `onResume()` ahora reanuda en este orden de prioridad: programa en curso → bloque comercial en curso (en su paso y posición real) → cualquier otro clip con estado guardado (`currentClipUri`) → solo si no hay ningún estado guardado, recién ahí `advance()` como último recurso.
- `goToAdjacentProgram()` (Prev/Next) limpia explícitamente el estado de clip no-programa al saltar, para no dejar un `currentClipUri` obsoleto que `onResume()` intente reanudar después.

> **Nota:** la restauración de sesión tras cerrar completamente la app (diálogo "¿Continuar donde estabas?", `resumeSavedState()`) no se modificó — sigue reiniciando bumper/enseguida desde el principio y saltando el comercial en curso, tal como estaba documentado. Este fix aplica únicamente al ciclo de vida `onPause()`/`onResume()` dentro de la misma sesión (segundo plano, cambio de Activity), no al guardado explícito en `SharedPreferences`.

---


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
| 2011.5.6.0           | 2026-07-29 | 🚀 Release | 2 bug fixes de NextProgram: CrtOverlayView único y compartido, ancho real del recuadro corregido con precisión de píxel; BUG FIX de arquitectura: "los programas" y ScreenBugs de eventos vuelven al Launcher, Configuración de Programa pasa a ser solo opciones de un programa puntual; "NextProgram" renombrado a "A continuación"; nuevo "Activar comerciales" por programa |
| 2011.5.5.0           | 2026-07-27 | 🚀 Release | Cambio de Era (2010→2011); nueva Activity Configuración de Programa (extraída del Launcher); NextProgram personalizado por programa; 3 ScreenBugs de eventos nuevos (Año Nuevo, Pascua, Día de la Tierra) + Navidad ahora configurable; BUG FIX: ActionBar tapando el layout, ScreenBug repitiéndose entre Intro/Programa/Créditos, NextProgram sin efecto CRT, ajuste fino de posición del recuadro |
| 2010.5.4.1           | 2026-07-25 | 🐛 Bug Fix | BUG FIX (causa raíz real): ScreenBug no aparecía en Intro ni Créditos (el cálculo de 20s/46s daba negativo en clips cortos, nunca se agendaba nada — clamp a la duración real del clip); corrección de diseño en NextProgram: el recuadro muestra el VideoView del programa en curso, no otro GIF |
| 2010.5.4.0           | 2026-07-24 | 🚀 Release | Consolida la Preview 2010.5.4.0.40 (NextProgram, ScreenBug final a 46s); BUG FIX: ANR al abrir la app (decode de GIFs sincrónico en el hilo principal, movido a hilo aparte), NextProgram no se ubicaba en el recuadro, cantidad de programas no se actualizaba al volver por Recientes; eliminación de StandaloneCommercial; Intro/Créditos personalizados por programa |
| 2010.5.3.0           | 2026-07-20 | 🚀 Release | Cambio de Era (2009→2010); BUG FIX definitivo del fadeOut/fadeIn de programas (el fix de la 2009.5.2.1 no restaba el punto de reanudación al calcular el timer, se rompía tras cualquier corte comercial); nuevo ScreenBug de Navidad (1–24 de diciembre), mismo comportamiento de 3 fases que el normal |
| 2009.5.2.1           | 2026-07-18 | 🐛 Bug Fix | TextureView eliminado por completo (motor, switch "Recortar 4:3", AlertDialog 720p+); BUG FIX (causa raíz real): contenedor de video vuelve a estar siempre en 4:3 (antes cambiaba a 16:9 con el switch desactivado), programa ahora hace fadeOut real al terminar (antes cortaba en seco y rompía el fadeIn del siguiente clip); Configuración usa ActionBar real (sin header hecho a mano); logo del Launcher fuera de la ActionBar, en el cuerpo |
| 2009.5.2.0           | 2026-07-17 | 🚀 Release | BUG FIX (causa raíz encontrada): ScreenBug "reiniciándose" al reanudar, y video estirado a 16:9 con "Forzar 4:3" desactivado (DkVideoView reescrito, fit de aspecto real compartido); ActionBar del Launcher: Configuración pasa al menú de overflow original; "Usar TextureView" renombrado a "Recortar 4:3", deshabilitado cuando "Forzar 4:3" está activo; nuevo Screenbug Julio 2009–2011 |
| 2009.5.1.0           | 2026-07-15 | 🚀 Release | Launcher rediseñado a Material Design 3 (claro/oscuro, ActionBar original); BUG FIX: Forzar 4:3 no respetaba el switch, recorte de video con TextureView, ScreenBug reiniciándose al cambiar de Activity/segundo plano; ajustes de timing del ScreenBug de 3 fases + reproducción de GIF nativa (GifMovieDrawable); nuevo Screenbug Mayo–Julio 2009 |
| 2009.5.0.0           | 2026-07-13 | 🚀 Release | "Parque Imaginario" (inicio Fase 4, rama 5.x): Discovery Kids Launcher pasa a ser la Activity de inicio (detrás de Experimental) con selector de video por programa, hasta 24 programas y ya_regresa/continuamos personalizados; AlertDialog 720p+, motor TextureView opcional, aviso de actualización al abrir la app |
| 2009.4.6.1           | 2026-07-11 | 🚀 Release | Cambio de Era 2008→2009; 3 Screenbug secuenciales (start.gif/screenbug.png/end.gif); fix AppUpdater con BUILD segment; fix Prev/Next navegación en orden del playlist |
| 2008.4.6.0           | 2026-07-10 | 🚀 Release | LiveDiscoveryKids.kt reunificado (reversión de la reorganización 4.1.0.21, sin cambios de comportamiento); nuevo Discovery Kids Launcher para elegir qué programas salen al aire |
| 2008.4.5.0.50-preview| 2026-07-07 | 🧪 Preview | UpdateActivity rediseñada 2: calca la pantalla nativa "Actualización del sistema" de Android — ícono/título/subtítulos alineados a la izquierda, barra fina de progreso, renglones de lista con flecha ">" en vez de botones |
| 2008.4.5.0           | 2026-07-06 | 🚀 Release | Cambio de Era 2007→2008: 4 comerciales standalone y par ya_regresa4/continuamos4 actualizados |
| 2007.4.4.0           | 2026-07-03 | 🚀 Release | Actualizador migra descarga de DownloadManager a OkHttp (progreso + detección de fin confiable); UpdateActivity rediseñada al estilo SettingsTheme; Screenbug actualizado a la variante de septiembre 2007 |
| 2007.4.3.1           | 2026-07-01 | 🐛 Release Fixer | Fix: Prev/Next saltaba al programa equivocado si se tocaba antes de que cualquier programa hubiera arrancado en la sesión (currentProgramIndex por defecto en 0 se confundía con "programa 0 ya visto") |
| 2007.4.3.0           | 2026-06-29 | 🚀 Release | Cambio de Era 2006→2007 (comerciales, ya_regresa/continuamos, Screenbug); UpdateActivity reemplaza los AlertDialog del Actualizador con barra de progreso en vivo |
| 2006.4.2.1           | 2026-06-27 | 🐛 Release Fixer | Fix crítico: AppUpdater siempre creía estar al día (comparaba el versionName completo contra el tag corto de GitHub, ej. "2006" vs "4") |
| 2006.4.2.0           | 2026-06-26 | 🚀 Release | "Habilitar versiones Preview" en Actualizador (default OFF); Actualizador integrado desde Configuración (consulta GitHub, descarga .apk); fix texto default Forzar 4:3; LiveDiscoveryKids.kt reorganizado en 11 archivos (sin cambios de comportamiento) |
| 2006.4.2.0.21-preview| 2026-06-25 | 🧪 Preview | "Habilitar versiones Preview" en Actualizador (default OFF); fix texto default Forzar 4:3; LiveDiscoveryKids.kt reorganizado en 11 archivos |
| 2006.4.2.0.20-preview| 2026-06-23 | 🧪 Preview | Actualizador integrado: "Buscar actualizaciones" en Configuración consulta GitHub Releases, descarga el .apk más nuevo y abre el instalador |
| 2006.4.1.1           | 2026-06-23 | 🐛 Bug Fix | Screenbug ya no reinicia su cuenta al volver de segundo plano/cambio de Activity; bumper/enseguida/comercial se reanudan en su posición real en vez de reiniciarse |
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
