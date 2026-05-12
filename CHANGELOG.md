# Registro de Cambios

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/),
y este proyecto sigue el estándar de [Versionado Semántico](https://semver.org/lang/es/).

---

## [2000.2.3.1] — Corrección de errores · Era 2000

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

| Versión       | Fecha      | Resumen                                                    |
|---------------|------------|------------------------------------------------------------|
| 2000.2.3.0    | 2026-05-11 | Comerciales Y2K agregados; mejora de resolución en bumpers |

---
