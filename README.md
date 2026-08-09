# Discovery Kids Channel — Simulador de canal (Android TV)

Simulador del canal Discovery Kids Latinoamérica, recreando su experiencia de
transmisión clásica: Bumpers, cortes comerciales, Screenbug, y la estructura
de programación de distintas Eras del canal (2005–2012+).

Versión actual: **2013.6.0.0.1** — Preview de la UPDATE "La Era Planetaria".

## Estructura del ciclo de reproducción

Cada programa del playlist sigue esta secuencia (todas las transiciones:
FadeOut 500 ms / FadeIn 1 s):

```
[Intro] → Programa → [Créditos] → [Intro] → Programa → [Créditos] → ...
```

`Intro` y `Créditos` son opcionales por programa (se activan y eligen desde
Configuración de Programa, en el Discovery Kids Launcher).

### Bumpers (Preview 2013.6.0.0.1)

Los Bumpers ya **no** salen de primero, antes del programa. En su lugar, se
insertan aleatoriamente dentro de cada corte comercial que interrumpe un
programa en curso, en uno de dos puntos posibles (sorteado una vez por
corte):

- Después del pre-comercial (*ya_regresa*) y antes del comercial.
- Después del comercial y antes del post-comercial (*continuamos*).

### Corte comercial

Secuencia dentro de un programa en curso:

```
ya_regresa (pre-comercial) → [Bumper] → comercial → [Bumper] → continuamos (post-comercial) → Programa
```

(el Bumper sale en uno solo de los dos puntos posibles marcados, nunca en
ambos, dentro del mismo corte).

- *ya_regresa*/*continuamos* son determinísticos por programa (pareados).
- El comercial se elige al azar entre los disponibles, sin repetir el mismo
  dos veces seguidas.
- La reanudación en segundo plano (`onResume`/`resumeCommercialBlock`)
  reconstruye el corte exacto donde quedó, incluido el Bumper si la app se
  pausó mientras sonaba.

## Stack técnico

- Kotlin, Android TV.
- Desarrollado con AndroidIDE directamente en el dispositivo físico (no
  Android Studio en PC).
- `VideoView` + `AspectRatioFrameLayout` (contenedor fijo 4:3) para la
  reproducción; `CrtOverlayView` para el efecto CRT (scanlines, máscara de
  fósforo, viñeta, flicker).
- Los programas deben ser 480p o menos: la aceleración por hardware del
  `SurfaceView` de `VideoView` a 720p+ se renderiza por encima de los
  overlays y tapa el Screenbug.

## Documentación

- `CHANGELOG.md` — historial de cambios, formato Keep a Changelog.
- `RELEASE_NOTES_<versión>.md` — notas de cada release, una por versión.
