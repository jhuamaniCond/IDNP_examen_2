# Pruebas de funcionamiento — Muro de Anuncios

**Fecha:** 9 de noviembre de 2026
**Dispositivo:** emulador Android (Medium Phone, API 36), cuenta de docente con los cursos IDNP-2026B-A e ISW2-2026B-B
**Datos:** 61 anuncios en el servidor (19 privados), publicados en 34 días distintos entre el 14 de agosto y el 9 de noviembre, con prioridad Alta, Media y Baja y contenidos de distinta extensión.

Las capturas de cada prueba están en esta misma carpeta, numeradas en el orden en que se hicieron.

## Resumen

| # | Prueba | Resultado |
|---|---|---|
| 1 | Preparar un anuncio privado y guardarlo como borrador | ✅ Correcto |
| 2 | Recuperar el borrador privado y continuar editándolo | ✅ Correcto |
| 3 | Publicar desde un borrador | ✅ Correcto |
| 4 | Borradores separados por cuenta | ✅ Correcto |
| 5 | Borradores después de cerrar y volver a abrir la app | ✅ Correcto |
| 6 | Búsqueda por título o contenido | ✅ Correcto |
| 7 | Búsqueda sin coincidencias y volver al muro completo | ✅ Correcto |
| 8 | Filtros por curso y por anuncios privados | ✅ Correcto |
| 9 | Archivar y restaurar (con deshacer) | ✅ Correcto |
| 10 | Funciones que ya existían: comentarios, contactos, cursos | ✅ Sin cambios, funcionan |

## Detalle

### 1. Preparar un anuncio privado y guardarlo como borrador
- **Pasos:** «Nuevo anuncio» → «Un estudiante» → escribir «Ana» → elegir *Ana Demo Torres (ISW2)* → prioridad Alta → título y mensaje → «Guardar como borrador».
- **Esperado:** el buscador encuentra a la alumna en los cursos donde está inscrita; el resumen indica quién recibirá el anuncio; el borrador aparece en «Borradores».
- **Obtenido:** la búsqueda mostró a la alumna en IDNP y en ISW2; el resumen decía *«Solo lo verá Ana Demo Torres (ISW2)»*; el chip pasó a «Borradores (2)».
- **Capturas:** `01_privado_buscar_alumno.png`, `02_privado_formulario_completo.png`, `03_borradores_lista.png`

### 2. Recuperar el borrador privado
- **Pasos:** «Borradores» → «Continuar» en el borrador privado.
- **Esperado:** se abre «Editar borrador» con la alumna, curso, prioridad, título y mensaje guardados.
- **Obtenido:** todo se recuperó, incluida la alumna seleccionada.
- **Captura:** `04_privado_reabierto.png`

### 3. Publicar desde un borrador
- **Pasos:** en el borrador reabierto, «Enviar a Ana».
- **Esperado:** se envía al servidor, el borrador desaparece y el anuncio aparece en el muro con su destinataria.
- **Obtenido:** aviso *«Anuncio enviado a Ana»*, «Borradores» bajó a 1 y el anuncio apareció primero en el muro (privado, Ana Demo Torres, Alta, «Hoy 12:51»).
- **Capturas:** `05_borrador_publicado_ya_no_esta.png`, `06_anuncio_privado_en_muro.png`

### 4. Borradores separados por cuenta
- **Cómo se probó:** no había otra cuenta de Google en el emulador, así que se compiló temporalmente una versión en la que la sesión se identificaba como otra cuenta (`cuenta-de-prueba-2`). Después se volvió a la versión normal.
- **Obtenido:**
  - Con la otra cuenta, «Borradores» estaba vacío (*«No tienes borradores»*) aunque la cuenta del docente tenía uno guardado en el mismo teléfono (`07_otra_cuenta_sin_borradores.png`).
  - La otra cuenta guardó su propio borrador (`08_otra_cuenta_su_borrador.png`).
  - Al volver a la cuenta del docente, solo aparecía su borrador y no el de la otra cuenta (`09_cuenta_original_solo_sus_borradores.png`).
  - En la base local (Room) quedaron ambos borradores, cada uno con su dueño:
    ```
    (1, '57b14a87-…', 'Borrador de prueba Room')
    (3, 'cuenta-de-prueba-2', 'Borrador de la cuenta 2')
    ```

### 5. Borradores después de cerrar la app
- **Pasos:** guardar un borrador, cerrar la app por completo (`am force-stop`), volver a abrirla e iniciar sesión.
- **Obtenido:** el borrador seguía en «Borradores» con su fecha de edición. Se comprobó también en la base de datos del teléfono.

### 6. Búsqueda por título o contenido
- **Pasos:** escribir «examen» en el buscador.
- **Obtenido:** 12 anuncios. Coinciden por título (*Cambio de aula para el examen parcial*) y por contenido (*…revisar tu examen*). Los resultados se actualizan mientras se escribe.
- **Captura:** `10_busqueda_examen.png`

### 7. Sin coincidencias y volver al muro completo
- **Pasos:** buscar «xyzabc» y luego tocar la ✕.
- **Obtenido:** mensaje *«Sin resultados para «xyzabc»»* con la sugerencia de probar otra palabra. Con la ✕ vuelve el muro completo.
- **Captura:** `11_busqueda_sin_resultados.png`

### 8. Filtros
- **Obtenido:** el chip «ISW2» muestra solo anuncios de ese curso; «Privados» muestra 17 anuncios, todos con candado y destinatario.
- **Capturas:** `12_filtro_curso_isw2.png`, `13_filtro_privados.png`

### 9. Archivar y restaurar
- **Pasos:** deslizar un anuncio a la izquierda → abrir «Archivados» → deslizarlo de nuevo para restaurarlo.
- **Obtenido:** aviso *«Anuncio archivado en este dispositivo»* con «Deshacer»; en «Archivados» se indica que es solo local; al restaurarlo vuelve al muro.
- **Capturas:** `14_archivar_con_deshacer.png`, `15_bandeja_archivados.png`

### 10. Funciones que ya existían
- **Comentarios:** se abren y muestran los comentarios guardados en el servidor (`16_comentarios_existentes.png`).
- **Contactos** y **Cursos:** cargan igual que antes (`17_contactos.png`, `18_cursos.png`).
- No se registró ningún cierre de la app durante las pruebas.

## Observaciones
- **La prioridad se guarda en el backend** (columna `post.priority`, agregada después de estas pruebas). Se verificó publicando un anuncio privado con prioridad Baja: en la base quedó `BAJA` y el muro la mostró leída del servidor; el anuncio de prueba se borró después.
- **Archivados y comentarios cerrados se guardan solo en el teléfono**, porque el backend no tiene esa columna ni el servicio. La app lo indica con el ícono de teléfono y el texto *«Solo en este teléfono»*.
- **Publicar en todo el curso desde la app fallaba** con *Internal Server Error* por un error previo en la función del servidor `service-post-add` (usaba una variable que no existía). Se corrigió y se desplegó; se verificó publicando un anuncio para ISW2 con prioridad Alta, que quedó en la base como público con `ALTA` y apareció primero en el muro. El anuncio de prueba se borró después.
- **La app pide iniciar sesión cada vez que se vuelve a abrir**, aunque se marque «Recordar cuenta». Esto ya pasaba antes de estos cambios.
- La prueba 3 publicó un anuncio privado real para Ana Demo Torres en ISW2. Se llamaba *«Prueba borrador privado»*; después de la prueba de usabilidad se renombró a *«Recordatorio: informe del laboratorio»* porque el nombre se confundía con un borrador.
