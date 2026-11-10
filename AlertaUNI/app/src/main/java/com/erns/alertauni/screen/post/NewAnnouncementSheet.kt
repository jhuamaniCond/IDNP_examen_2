package com.erns.alertauni.screen.post

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erns.alertauni.data.model.StudentEntity
import com.erns.alertauni.ui.theme.MyForegroundColor
import com.erns.alertauni.ui.theme.MyMutedForegroundColor
import com.erns.alertauni.ui.theme.MyPrimaryColor
import com.erns.alertauni.ui.theme.MyPrimaryForegroundColor
import com.erns.alertauni.ui.theme.MySecondaryColor
import com.erns.alertauni.ui.theme.MySurfaceColor

private val Sans = FontFamily.SansSerif
private const val MAX_CONTENT = 280

/** Destinatario del anuncio: todo el curso o un solo alumno. */
private enum class Audience(val label: String, val icon: ImageVector) {
    CURSO("Todo el curso", Icons.Filled.Groups),
    ESTUDIANTE("Un estudiante", Icons.Filled.Lock)
}

/** Alumno elegido junto con el curso desde el que se le escribe. */
private data class Candidate(val student: StudentEntity, val course: CourseUi)

/** Un alumno en la lista de busqueda, una sola vez aunque este en varios cursos. */
private data class StudentOption(val name: StudentEntity, val byCourse: List<Candidate>)

/**
 * El contenido de la hoja se desplaza, pero el arrastre que sobra al llegar arriba no debe
 * mover la hoja: si no, al intentar desplazar el formulario la hoja empieza a cerrarse.
 */
private object BlockSheetDrag : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewAnnouncementSheet(
    courses: List<CourseUi>,
    students: Map<String, List<StudentEntity>>,
    sending: Boolean,
    initialCourseId: String?,
    /** Borrador que se esta retomando; nulo para un anuncio nuevo. */
    draft: DraftUi? = null,
    onDismiss: () -> Unit,
    onSend: (courseId: String, student: StudentEntity?, Priority, title: String, content: String) -> Unit,
    onSaveDraft: (courseId: String?, student: StudentEntity?, isPrivate: Boolean, Priority, title: String, content: String) -> Unit
) {
    var audience by remember { mutableStateOf(if (draft?.isPrivate == true) Audience.ESTUDIANTE else Audience.CURSO) }
    // Con un solo curso no hay nada que elegir: se selecciona solo
    var courseId by remember {
        mutableStateOf(draft?.course?.catalogId ?: initialCourseId ?: courses.singleOrNull()?.catalogId)
    }
    var studentQuery by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf(draft?.let { recipientFromDraft(it, students) }) }
    // Alumno que esta en varios cursos: falta decidir desde cual se le escribe
    var pendingStudent by remember { mutableStateOf<StudentOption?>(null) }
    var priority by remember { mutableStateOf(draft?.priority ?: Priority.MEDIA) }
    var title by remember { mutableStateOf(draft?.title.orEmpty()) }
    var content by remember { mutableStateOf(draft?.content.orEmpty()) }

    val course = courses.firstOrNull { it.catalogId == courseId }
    val ready = course != null && title.isNotBlank() && content.isNotBlank() &&
        (audience == Audience.CURSO || recipient != null)
    val canSaveDraft = title.isNotBlank() || content.isNotBlank()

    // Cualquier cambio (alumno, curso, prioridad o texto) se pierde al cerrar: se pregunta antes
    val initial = remember { Snapshot(audience, courseId, recipient?.student?.student_id, priority, title, content) }
    val dirty = Snapshot(audience, courseId, recipient?.student?.student_id, priority, title, content) != initial
    val dirtyState = rememberUpdatedState(dirty)
    var askExit by remember { mutableStateOf(false) }

    val saveDraft: () -> Unit = {
        onSaveDraft(courseId, recipient?.student, audience == Audience.ESTUDIANTE, priority, title.trim(), content.trim())
    }
    val requestClose: () -> Unit = { if (dirty) askExit = true else onDismiss() }
    val pick: (Candidate) -> Unit = {
        recipient = it
        courseId = it.course.catalogId
        pendingStudent = null
        studentQuery = ""
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            // Deslizar hacia abajo, tocar fuera o "atras" con cambios: no cerrar, preguntar
            if (value == SheetValue.Hidden && dirtyState.value) {
                askExit = true
                false
            } else true
        }
    )

    if (askExit) {
        if (canSaveDraft) {
            AlertDialog(
                onDismissRequest = { askExit = false },
                icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                title = { Text("¿Guardar como borrador?", fontFamily = Sans) },
                text = {
                    Text("Tu anuncio aún no se ha publicado. Guárdalo como borrador para terminarlo después.", fontFamily = Sans)
                },
                confirmButton = { TextButton(onClick = { askExit = false; saveDraft() }) { Text("Guardar borrador") } },
                dismissButton = { TextButton(onClick = { askExit = false; onDismiss() }) { Text("Descartar") } }
            )
        } else {
            // Sin titulo ni mensaje no hay borrador que guardar, pero si algo elegido que se perderia
            AlertDialog(
                onDismissRequest = { askExit = false },
                title = { Text("¿Descartar el anuncio?", fontFamily = Sans) },
                text = { Text("Se perderá lo que elegiste.", fontFamily = Sans) },
                confirmButton = { TextButton(onClick = { askExit = false; onDismiss() }) { Text("Descartar") } },
                dismissButton = { TextButton(onClick = { askExit = false }) { Text("Seguir editando") } }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MySurfaceColor,
        // "Atras" cierra la hoja sin pasar por confirmValueChange: se desactiva aqui y se
        // maneja con BackHandler para preguntar antes de perder lo elegido
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        BackHandler(onBack = requestClose)
        // imePadding en todo el contenido: la barra de botones queda siempre sobre el teclado
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 20.dp, end = 8.dp)
            ) {
                Text(
                    if (draft != null) "Editar borrador" else "Nuevo anuncio",
                    fontFamily = Sans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = MyForegroundColor,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = requestClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar sin publicar")
                }
            }

            // Formulario desplazable entre la cabecera y la barra fija de botones
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .nestedScroll(BlockSheetDrag)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (draft != null) {
                    LocalOnlyNote("Borrador guardado en este teléfono · editado ${draft.editedLabel}")
                }

                // 1. ¿Para quien? Es lo primero que se decide y cambia el resto del formulario
                Section("¿Para quién es?") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        Audience.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = audience == option,
                                onClick = {
                                    audience = option
                                    if (option == Audience.CURSO) {
                                        recipient = null
                                        pendingStudent = null
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, Audience.entries.size),
                                icon = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MySecondaryColor,
                                    activeContentColor = MyPrimaryColor
                                )
                            ) { Text(option.label, fontFamily = Sans) }
                        }
                    }
                }

                // 2. Curso
                Section(if (audience == Audience.CURSO) "Curso" else "Curso del alumno") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        courses.forEach { option ->
                            FilterChip(
                                selected = courseId == option.catalogId,
                                onClick = {
                                    courseId = option.catalogId
                                    if (recipient?.course?.catalogId != option.catalogId) recipient = null
                                },
                                label = { Text(option.acronym, fontFamily = Sans, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MyPrimaryColor,
                                    selectedLabelColor = MyPrimaryForegroundColor
                                )
                            )
                        }
                    }
                    Text(
                        text = course?.name ?: if (audience == Audience.CURSO) "Elige el curso que recibirá el anuncio."
                        else "Opcional: si no eliges curso, se busca en todos.",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        color = MyMutedForegroundColor
                    )
                }

                // 3. Alumno (solo en privado): se busca escribiendo, sin recorrer listas
                if (audience == Audience.ESTUDIANTE) {
                    Section("Alumno") {
                        val chosen = recipient
                        val pending = pendingStudent
                        when {
                            chosen != null -> RecipientChip(chosen) { recipient = null }
                            pending != null -> ChooseCourse(pending, onPick = pick, onCancel = { pendingStudent = null })
                            else -> StudentSearch(
                                query = studentQuery,
                                onQueryChange = { studentQuery = it },
                                options = studentOptions(courses, students, courseId, studentQuery),
                                onPick = { option ->
                                    // Si ya hay curso elegido o el alumno esta en uno solo, se completa solo
                                    val match = option.byCourse.firstOrNull { it.course.catalogId == courseId }
                                        ?: option.byCourse.singleOrNull()
                                    if (match != null) pick(match) else pendingStudent = option
                                }
                            )
                        }
                    }
                }

                // 4. Prioridad
                Section("Prioridad") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        Priority.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = priority == option,
                                onClick = { priority = option },
                                shape = SegmentedButtonDefaults.itemShape(index, Priority.entries.size),
                                icon = {
                                    Icon(
                                        Icons.Filled.Circle,
                                        contentDescription = null,
                                        tint = option.color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MySecondaryColor,
                                    activeContentColor = MyForegroundColor
                                )
                            ) { Text(option.label, fontFamily = Sans) }
                        }
                    }
                    Text(
                        text = priorityHelp(priority, audience),
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        color = MyMutedForegroundColor
                    )
                }

                // 5. Mensaje
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Título", fontFamily = Sans) },
                    placeholder = { Text("Ej: Cambio de aula para el examen", fontFamily = Sans) },
                    singleLine = true,
                    // "Siguiente" en el teclado pasa directo al mensaje
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    textStyle = TextStyle(fontFamily = Sans, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it.take(MAX_CONTENT) },
                    label = { Text("Mensaje", fontFamily = Sans) },
                    minLines = 3,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    supportingText = {
                        Text("${content.length}/$MAX_CONTENT", fontFamily = Sans, modifier = Modifier.fillMaxWidth())
                    },
                    textStyle = TextStyle(fontFamily = Sans, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Barra fija: el destino y los botones se ven siempre, aunque el teclado este abierto
            HorizontalDivider(color = MySecondaryColor)
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AudienceSummary(audience, course, recipient, students)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Guardar sin publicar: no exige tener todo completo, basta con algo escrito
                    OutlinedButton(
                        onClick = saveDraft,
                        enabled = canSaveDraft && !sending,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar borrador", fontFamily = Sans, fontWeight = FontWeight.Medium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = {
                            val c = course ?: return@Button
                            onSend(c.catalogId, recipient?.student, priority, title.trim(), content.trim())
                        },
                        enabled = ready && !sending,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MyPrimaryColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                color = MyPrimaryForegroundColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = sendLabel(audience, course, recipient),
                                fontFamily = Sans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Ayuda de la prioridad; no habla de "curso" cuando el anuncio es para un solo alumno. */
private fun priorityHelp(priority: Priority, audience: Audience): String = when (priority) {
    Priority.ALTA -> "Se destaca en rojo en el muro: urgente."
    Priority.MEDIA -> if (audience == Audience.CURSO) "Aviso normal para el curso." else "Aviso normal para el estudiante."
    Priority.BAJA -> "Información de consulta, sin urgencia."
}

/** Estado del formulario para saber si cambio desde que se abrio. */
private data class Snapshot(
    val audience: Audience,
    val courseId: String?,
    val recipientId: String?,
    val priority: Priority,
    val title: String,
    val content: String
)

/**
 * Reconstruye el alumno de un borrador. Si sus datos ya se cargaron se usa el alumno real;
 * si no, uno minimo con el nombre guardado (basta para mostrarlo y enviar por su id).
 */
private fun recipientFromDraft(draft: DraftUi, students: Map<String, List<StudentEntity>>): Candidate? {
    val id = draft.recipientId ?: return null
    val course = draft.course ?: return null
    val real = students[course.catalogId]?.firstOrNull { it.student_id == id }
    if (real != null) return Candidate(real, course)
    val name = draft.recipientName.orEmpty()
    return Candidate(
        StudentEntity(
            course_catalog_id = course.catalogId,
            student_id = id,
            codigo = "",
            firstname = name.substringBefore(' '),
            surname = name.substringAfter(' ', ""),
            email = ""
        ),
        course
    )
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MyMutedForegroundColor
        )
        content()
    }
}

/** Alumnos que coinciden con lo escrito; cada uno aparece una vez con todos sus cursos. */
private fun studentOptions(
    courses: List<CourseUi>,
    students: Map<String, List<StudentEntity>>,
    courseId: String?,
    query: String
): List<StudentOption> =
    courses
        .filter { courseId == null || it.catalogId == courseId }
        .flatMap { course -> students[course.catalogId].orEmpty().map { Candidate(it, course) } }
        .filter { it.student.matches(query) }
        .groupBy { it.student.student_id }
        .map { (_, byCourse) -> StudentOption(byCourse.first().student, byCourse) }
        .sortedBy { it.name.surname }
        .take(6)

@Composable
private fun StudentSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    options: List<StudentOption>,
    onPick: (StudentOption) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Escribe el nombre o código", fontFamily = Sans) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        textStyle = TextStyle(fontFamily = Sans, fontSize = 16.sp),
        modifier = Modifier.fillMaxWidth()
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MySecondaryColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
    ) {
        if (options.isEmpty()) {
            Text(
                "Ningún alumno coincide con «$query».",
                fontFamily = Sans,
                fontSize = 14.sp,
                color = MyMutedForegroundColor,
                modifier = Modifier.padding(12.dp)
            )
        }
        options.forEachIndexed { index, option ->
            if (index > 0) HorizontalDivider(color = MySurfaceColor)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Initials(option.name)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        option.name.fullName(),
                        fontFamily = Sans,
                        fontSize = 15.sp,
                        color = MyForegroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (option.name.codigo.isNotBlank()) {
                        Text(option.name.codigo, fontFamily = Sans, fontSize = 12.sp, color = MyMutedForegroundColor)
                    }
                }
                Text(
                    option.byCourse.joinToString(" · ") { it.course.acronym },
                    fontFamily = Sans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyPrimaryColor
                )
            }
        }
    }
}

/** El alumno esta en varios cursos y no hay curso elegido: se pregunta desde cual escribirle. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseCourse(option: StudentOption, onPick: (Candidate) -> Unit, onCancel: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MySecondaryColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            "${option.name.fullName()} está en varios de tus cursos. ¿Desde cuál le escribes?",
            fontFamily = Sans,
            fontSize = 14.sp,
            color = MyForegroundColor
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            option.byCourse.forEach { candidate ->
                FilterChip(
                    selected = false,
                    onClick = { onPick(candidate) },
                    label = { Text(candidate.course.acronym, fontFamily = Sans, fontWeight = FontWeight.Medium) }
                )
            }
        }
        TextButton(onClick = onCancel) { Text("Elegir otro alumno", fontFamily = Sans) }
    }
}

@Composable
private fun RecipientChip(candidate: Candidate, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MySecondaryColor, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Initials(candidate.student)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                candidate.student.fullName(),
                fontFamily = Sans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MyForegroundColor
            )
            Text(candidate.course.acronym + " · " + candidate.course.name, fontFamily = Sans, fontSize = 12.sp,
                color = MyMutedForegroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClear) {
            Icon(Icons.Filled.Close, contentDescription = "Cambiar de alumno")
        }
    }
}

@Composable
private fun Initials(student: StudentEntity) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .background(MyPrimaryColor, CircleShape)
    ) {
        Text(
            (student.firstname.take(1) + student.surname.take(1)).uppercase(),
            color = MyPrimaryForegroundColor,
            fontFamily = Sans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AudienceSummary(
    audience: Audience,
    course: CourseUi?,
    recipient: Candidate?,
    students: Map<String, List<StudentEntity>>
) {
    val text = when {
        audience == Audience.ESTUDIANTE && recipient != null ->
            "Solo lo verá ${recipient.student.fullName()} (${recipient.course.acronym})."
        audience == Audience.ESTUDIANTE -> "Elige al alumno que recibirá el anuncio."
        course == null -> "Elige el curso que recibirá el anuncio."
        else -> {
            val count = students[course.catalogId]?.size
            if (count == null) "Lo verán todos los estudiantes de ${course.acronym}."
            else "Lo verán los $count estudiantes de ${course.acronym}."
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MySecondaryColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(audience.icon, contentDescription = null, tint = MyPrimaryColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = Sans, fontSize = 14.sp, color = MyPrimaryColor, fontWeight = FontWeight.Medium)
    }
}

private fun sendLabel(audience: Audience, course: CourseUi?, recipient: Candidate?): String = when {
    audience == Audience.ESTUDIANTE && recipient != null -> "Enviar a ${recipient.student.firstname}"
    audience == Audience.CURSO && course != null -> "Publicar en ${course.acronym}"
    else -> "Publicar"
}
