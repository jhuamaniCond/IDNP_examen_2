# Resultados — Prueba de usabilidad

**Dispositivo:** emulador Android (`emulator-5554`, 1080×2400), app `com.erns.alertauni`, cuenta del docente

## Tabla del observador

| | Tarea 1: encontrar | Tarea 2: privado | Tarea 3: borrador |
|---|---|---|---|
| ¿La completó? | Sí | Sí, con un tropiezo | Sí |
| Tiempo aproximado | ~20 s | ~2 min | ~1,5 min |
| ¿Pidió ayuda? | No | No | No |
| Errores o caminos equivocados | Ninguno | 1) Al pulsar *Atrás* para bajar el teclado después de elegir a Rosa, se cerró toda la hoja **sin aviso** y se perdió la alumna elegida. 2) El teclado tapaba el campo «Mensaje»; al abrirse, la hoja se desplazó y el toque cayó otra vez en «Título». 3) Al deslizar hacia arriba para ver la X, la hoja se arrastró para cerrarse (salió el aviso de borrador, que la salvó). | Ninguno |
| En voz alta | «Busco *asesoria* sin tilde… la encontró igual.» | «*Un estudiante*, con candado: eso es lo privado.» · «*Solo lo verá Rosa Demo Flores (IDNP)*: queda clarísimo.» · «¿*La prioridad se guarda solo en este dispositivo*? No sé qué significa eso para mí.» | «Guardar como borrador, justo debajo de Publicar.» · «El chip pasó de (1) a (2).» · «Los borradores tienen borde punteado y la etiqueta *Borrador*: no se confunden con los publicados.» |

**Cosas para fijarse**
- **T1:** usó el **buscador**, con la palabra «asesoria» (sin tilde). La búsqueda ignora las tildes. Respuesta: **martes de 16:00 a 18:00, oficina 210** ✅
- **T2:** encontró «Un estudiante» enseguida y buscó a la alumna **por nombre**. Al elegirla, el curso IDNP se seleccionó solo. Leyó el resumen *«Solo lo verá…»*, y el botón cambió a **«Enviar a Rosa»**, lo que confirma el destino. Prioridad: Alta.
- **T3:** encontró «Guardar como borrador» sin necesidad de cerrar la hoja. Salió el snackbar *«Borrador guardado en este dispositivo»*. Encontró el chip «Borradores» → «Continuar» → «Editar borrador» con todo recuperado.

---

## Cuestionario final

1. **¿Completó cada tarea?** T1: Sí · T2: Con dificultad (por el teclado y el cierre con *Atrás*, no por entender a quién va) · T3: Sí.
2. **Lo más difícil:** escribir en el formulario con el teclado abierto. Tapa «Mensaje» y los botones, y la hoja se mueve sola.
3. **¿No supo dónde tocar?** Después de escribir el mensaje, el botón «Enviar» quedó oculto bajo el teclado y no era obvio que había que bajarlo.
4. **¿Tuvo claro quién recibiría el anuncio (T2)?** Sí. Hay tres señales que coinciden: el candado en «Un estudiante», el resumen *«Solo lo verá Rosa Demo Flores (IDNP)»* y el botón «Enviar a Rosa».
5. **¿Fue fácil distinguir el borrador (T3)?** Sí, en la pantalla de Borradores (borde punteado, etiqueta, «Editado…»). En el muro principal el borrador no aparece; lo único que cambia es el contador del chip. Además, hay un anuncio **publicado** llamado *«Prueba borrador privado»* que confunde por el nombre (son datos de prueba; conviene archivarlo antes de probar con personas).
6. **Innecesario o raro:**
   - *«La prioridad se guarda solo en este dispositivo (el servidor aún no la admite)»*: es un detalle técnico que al docente no le sirve y le genera desconfianza.
   - *«Los borradores se guardan solo en este dispositivo y en tu cuenta»*: «solo en este dispositivo» y «en tu cuenta» parecen contradecirse.
   - En modo «Un estudiante», la ayuda de prioridad Media dice *«Aviso normal del curso»*, aunque el anuncio no va al curso.
   - En la lista inicial de alumnos, el mismo alumno aparece dos veces si está en dos cursos (p. ej. *Luis Demo Mamani* IDNP / ISW2). Se entiende por la etiqueta del curso, pero alarga la lista.
7. **Facilidad (1–5):** **4**
8. **Qué cambiaría:**
   - Al pulsar *Atrás* con la hoja ya modificada (aunque solo sea la alumna elegida), mostrar el mismo aviso «¿Guardar como borrador?» o, por lo menos, «¿Descartar?».
   - Que el formulario haga scroll hasta el campo enfocado (`imePadding` / `bringIntoViewRequester`) y que la acción del teclado en «Título» sea *Siguiente* para pasar a «Mensaje».
   - Dejar «Enviar / Publicar» siempre visible encima del teclado.
   - Quitar las notas técnicas sobre el servidor y explicar los borradores con una sola frase clara.
   - Cambiar la ayuda de prioridad según el destino (p. ej. «Aviso normal» en lugar de «Aviso normal del curso»).

---

## Hallazgos priorizados

| # | Severidad | Hallazgo | Dónde ocurrió |
|---|---|---|---|
| 1 | Alta | *Atrás* cierra la hoja de «Nuevo anuncio» sin confirmar y se pierde lo elegido (solo pregunta si hay título o mensaje) | Tarea 2 |
| 2 | Media | El teclado tapa «Mensaje» y los botones de envío; la hoja se mueve y los toques caen en otro campo | Tarea 2 |
| 3 | Media | Deslizar hacia arriba para hacer scroll en la hoja la arrastra para cerrarla | Tarea 2 |
| 4 | Baja | Textos técnicos o contradictorios sobre dónde se guardan la prioridad y los borradores | Tareas 2 y 3 |
| 5 | Baja | La ayuda «Aviso normal del curso» sale también en anuncios privados | Tarea 2 |
| 6 | Baja | Alumnos repetidos por curso en la lista inicial del buscador | Tarea 2 |

**Lo que funcionó bien:** búsqueda que ignora tildes con contador «1 anuncio»; segmentado «Todo el curso / Un estudiante»; elegir a la alumna completa el curso solo; el resumen de destino y el botón «Enviar a Rosa»; «Guardar como borrador» bien visible y con confirmación; el aviso «¿Guardar como borrador?» al cerrar; la tarjeta de borrador con estilo propio; «Deshacer» al eliminar un borrador.
