# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/).

## [2013.6.0.0.1] - Preview - UPDATE "La Era Planetaria"

### Cambiado
- **Reposicionamiento del Bumper dentro del corte comercial.** El Bumper ya
  no es el primer ítem del ciclo (ya no sale antes del programa). Ahora se
  inserta de forma aleatoria DENTRO de cada corte comercial, en uno de dos
  puntos posibles — sorteado una sola vez por corte:
  - Después del pre-comercial (*ya_regresa*) y antes del comercial.
  - Después del comercial y antes del post-comercial (*continuamos*).
- Se cambiaron 2 comerciales por los de la UPDATE "La Era Planetaria".

### Agregado
- Nuevos Bumpers.

### Notas técnicas (`LiveDiscoveryKids.kt`)
- `buildPlaylist()` ya no agrega `PlayItem.Bumper` al ciclo del playlist
  (el ciclo queda `[Intro] → Programa → [Créditos]`). El tipo `PlayItem.Bumper`
  y su rama en `advance()` se dejan intactos, sin uso, por si se necesitan
  más adelante.
- Nuevo campo de instancia `commercialBumperBeforeComercial` (booleano,
  sorteado en `playCommercial()` con `listOf(true, false).random()`) decide,
  para cada corte comercial, en cuál de los dos puntos posibles sale el
  Bumper de ese corte.
- Nuevos valores `BUMPER_BEFORE_COMERCIAL` y `BUMPER_AFTER_COMERCIAL` en el
  enum `CommercialStep`, para que `resumeCommercialBlock()` pueda reconstruir
  el Bumper exacto (mismo recurso, misma posición) si la app pasa a segundo
  plano justo mientras suena. Nuevo campo `commercialChosenBumperRes` guarda
  ese recurso, igual que ya se hacía con `commercialChosenCommercial` /
  `commercialChosenPreComercial` / `commercialChosenYaVolvemos`.
- `playCommercialStepPreComercial()` se dividió en tres funciones encadenadas
  para poder insertar el Bumper en cualquiera de los dos puntos sin duplicar
  lógica: `playCommercialStepPreComercial()` (paso 1, sin cambios de
  comportamiento salvo el gancho al Bumper), `playCommercialStepComercial()`
  (paso 2, nueva) y `playCommercialStepPostComercial()` (paso 3, nueva).
- Nueva función `playCommercialBumper()`: reproduce un Bumper aleatorio (sin
  repetir el último, igual que el Bumper clásico) dentro del corte comercial,
  encadenando al siguiente paso vía callback en vez de llamar `advance()`.

