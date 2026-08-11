<div align="center">

## 🧪 Preview 2013.6.0.0.2 — 2026-08-10
**Era Doki 1.0 · Era 2013 · "La Era Planetaria"**

*Continuamos con horario · "Ya regresa" eliminado · FadeOut/FadeIn acotado · GIF "próximo programa" · 2 correcciones en el Actualizador*

</div>

---

## 📋 Resumen

Esta Preview reordena por completo cómo se arma el corte comercial. El clip pre-comercial **"ya_regresa" se elimina del todo** — el corte ahora pasa directo del Programa al Bumper/comercial. El **continuamos** (post-comercial) deja de estar atado a un programa fijo y pasa a depender de la **hora real del dispositivo**. Las transiciones con FadeOut/FadeIn, que antes cubrían cualquier cambio de video, ahora quedan reservadas **exclusivamente** al límite Programa↔bloque comercial — todo lo demás corta en seco, como un cambio de canal real. Se suma un nuevo GIF, **"próximo programa"**, que aparece brevemente a mitad de cada segmento del programa. Y se corrigieron 2 problemas del Actualizador: la doble barra superior, y el bug de fondo del ActionBar comiéndose parte del layout.

---

## 🆕 Continuamos con horario

El continuamos ya no se elige emparejado con el ya_regresa de cada programa (que además se elimina, ver más abajo). Ahora depende únicamente de la hora real del dispositivo al momento del corte:

- **`continuamos_mananera1.mp4`** *(antes `continuamos1.mp4`)* — sale si el corte ocurre entre las **00:00 y las 11:59**.
- **`continuamos_tardia1.mp4`** *(antes `continuamos2.mp4`)* — sale entre las **12:00 y las 23:59**.

Si activaste "Personalizado" para el continuamos de algún programa en Configuración de Programa, no cambia nada — tu video elegido sigue teniendo prioridad sobre el predeterminado por horario.

> ⚠️ Los nombres de recurso no llevan tilde ni ñ (`mananera`, no `mañanera`) — es una restricción de Android: `res/raw` solo acepta minúsculas, números y guion bajo.

---

## 🗑️ "Ya regresa" eliminado por completo

El clip pre-comercial "ya_regresa" —el aviso fijo por programa que abría cada corte antes del comercial— desaparece del canal por completo. El corte comercial ahora arranca directo con el Bumper (si le toca a ese corte) o el comercial, sin ningún clip de aviso previo.

También se sacó la opción **"Ya regresa personalizado"** de Configuración de Programa — esa sección de la pantalla ya no existe. El continuamos sigue siendo personalizable exactamente igual que antes.

---

## 🎬 FadeOut / FadeIn acotado al límite Programa↔comercial

Antes, **todo** cambio de video del canal (bumpers, ya_regresa, comerciales, continuamos, arranque de programas) llevaba un FadeOut de 500 ms de salida y un FadeIn de 1 segundo de entrada. A partir de esta Preview, eso cambia radicalmente:

- El Programa se apaga con **FadeOut (500 ms)** justo antes de cortar a un bloque comercial.
- El Programa se enciende con **FadeIn (1 s)** al retomarse, justo después del bloque comercial.
- **Todo lo demás corta en seco:** Bumpers, Intro, Créditos, y los clips dentro del bloque comercial (comercial y continuamos) ya no tienen ninguna animación — cambian de golpe, como un corte de cámara real.
- Si un Programa termina y no hay más episodios pendientes (pasa a Créditos o al siguiente ítem de la programación), tampoco hay FadeOut — ese caso ya no es "antes de un comercial", así que corta en seco también.

---

## 🆕 GIF "próximo programa" durante los programas

Nuevo overlay `proximo_programa_screenbug.gif`, que aparece una vez por segmento a mitad del programa, sustituyendo brevemente al ScreenBug estático (`screenbug.png`):

- **Nunca** dentro del primer minuto de arrancado el segmento.
- **Nunca** de forma que quede dentro del último minuto antes de un corte comercial, o del final del programa/episodio. Si el segmento no tiene margen suficiente para respetar los dos márgenes (menos de 2 min 15 s en total), directamente no se muestra en ese segmento.
- Al mostrarse: el `screenbug.png` desaparece y aparece el GIF, **sin animación** — igual que el resto de las fases del ScreenBug.
- A los **15 segundos**, el GIF desaparece y vuelve el `screenbug.png`, también sin animación.
- Solo corre durante el Programa en sí — no aparece durante la Intro.

> ⚠️ El archivo `proximo_programa_screenbug.gif` no viene incluido en este cambio — hay que agregarlo a `res/drawable/` aparte.

---

## 🔧 Corregido

- **Actualizador con dos barras superiores.** La pantalla de "Buscar actualizaciones" mostraba una barra del sistema (con el nombre de la app) y, debajo, su propio encabezado con el botón Atrás y el título "Actualizaciones" — dos barras apiladas. Se sacó el encabezado propio y ahora el título y la navegación hacia atrás los da la barra real de Android, igual que ya funciona en Configuración.
- **ActionBar comiéndose parte del layout (investigación a fondo).** Un intento anterior de corregir el espaciado no había funcionado porque agregaba el ajuste de márgenes sin sacar el encabezado duplicado — el síntoma seguía viéndose igual. Al sacar ese encabezado y aplicar el mismo ajuste que ya funciona correctamente en las otras pantallas de la app, ambos problemas quedan resueltos de raíz.

---

## ⚠️ Alcance

> Cambios de código en `LiveDiscoveryKids.kt` (continuamos con horario, eliminación de ya_regresa, FadeOut/FadeIn acotado al límite Programa↔comercial, GIF "próximo programa"), `SettingsManager.kt` (limpieza de las claves de ya_regresa), `ProgramConfigActivity.kt` / `activity_program_config.xml` (opción de ya_regresa personalizado eliminada), `DiscoveryKidsLauncherActivity.kt` (validación de "Iniciar canal" actualizada), `UpdateActivity.kt` / `activity_update.xml` (doble menú y ActionBar). `build.gradle`: `versionName` a `2013.6.0.0.2`.

---

**Versión anterior:** [v6.0.0.0.1](https://github.com/keylerperales07-commits/DiscoveryKidsChannel/releases/tag/v6.0.0.0.1)
**Tipo:** Preview · Era Doki 1.0 · **Era 2013** · Fase 4
**Plataforma:** Android 6.0+ (API 23)

---

<p align="center">
  Hecho con ❤️ y nostalgia &nbsp;·&nbsp;
  <a href="https://github.com/keylerperales07-commits/DiscoveryKidsChannel">Ver en GitHub</a>
</p>
