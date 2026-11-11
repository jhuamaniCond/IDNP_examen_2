package com.erns.alertauni.screen.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CommentsDisabled
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erns.alertauni.ui.theme.MyBorderColor
import com.erns.alertauni.ui.theme.MyForegroundColor
import com.erns.alertauni.ui.theme.MyMutedColor
import com.erns.alertauni.ui.theme.MyMutedForegroundColor
import com.erns.alertauni.ui.theme.MyPrimaryColor
import com.erns.alertauni.ui.theme.MyPrimaryForegroundColor
import com.erns.alertauni.ui.theme.MySecondaryColor
import com.erns.alertauni.ui.theme.MySurfaceColor
import kotlinx.coroutines.launch

private val Sans = FontFamily.SansSerif

/** La hoja de anuncio abierta: nueva (draft = null) o retomando un borrador. */
private data class SheetRequest(val draft: DraftUi?)

@Composable
fun PostScreen(
    modifier: Modifier = Modifier,
    onClickComment: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onFabActionReady: (() -> Unit) -> Unit
) {
    val viewModel: PostViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val students by viewModel.students.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val isProfessor by viewModel.isProfessor.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    onFabActionReady {
        if (state.courses.isNotEmpty()) {
            sheet = SheetRequest(draft = null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Aún no tienes cursos para publicar anuncios.") }
        }
    }

    sheet?.let { request ->
        // key: al pasar de un borrador a otro el formulario se reinicia con sus datos
        key(request.draft?.id) {
            NewAnnouncementSheet(
                courses = state.courses,
                students = students,
                sending = sending,
                initialCourseId = state.filter.courseCatalogId,
                draft = request.draft,
                onDismiss = { sheet = null },
                onSend = { courseId, student, priority, title, content ->
                    viewModel.send(courseId, student, priority, title, content, request.draft?.id) { ok ->
                        if (ok) sheet = null
                    }
                },
                onSaveDraft = { courseId, student, isPrivate, priority, title, content ->
                    viewModel.saveDraft(request.draft?.id, courseId, student, isPrivate, priority, title, content) {
                        sheet = null
                    }
                }
            )
        }
    }

    // Archivar con deshacer: el anuncio desaparece del muro pero se puede recuperar al instante
    val onArchive: (AnnounceUi) -> Unit = { announce ->
        val archive = !announce.archived
        viewModel.setArchived(announce.id, archive)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = if (archive) "Anuncio archivado en este teléfono" else "Anuncio restaurado al muro",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.setArchived(announce.id, !archive)
        }
    }

    val onDeleteDraft: (DraftUi) -> Unit = { draft ->
        viewModel.deleteDraft(draft.id)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Borrador eliminado",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDeleteDraft()
        }
    }

    WallLayout(
        state = state,
        showDrafts = isProfessor,
        onQueryChange = viewModel::onQueryChange,
        onCourseSelected = viewModel::onCourseSelected,
        onOnlyPrivateToggle = viewModel::onOnlyPrivateToggle,
        onModeChange = viewModel::onModeChange,
        refreshing = refreshing,
        onRefresh = viewModel::refresh,
        onOpen = { onClickComment(it.id.toString()) },
        onArchive = onArchive,
        onToggleComments = { viewModel.setCommentsOpen(it.id, !it.commentsOpen) },
        onOpenDraft = { sheet = SheetRequest(draft = it) },
        onDeleteDraft = onDeleteDraft
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallLayout(
    state: WallState,
    showDrafts: Boolean,
    onQueryChange: (String) -> Unit,
    onCourseSelected: (String?) -> Unit,
    onOnlyPrivateToggle: () -> Unit,
    onModeChange: (WallMode) -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (AnnounceUi) -> Unit,
    onArchive: (AnnounceUi) -> Unit,
    onToggleComments: (AnnounceUi) -> Unit,
    onOpenDraft: (DraftUi) -> Unit,
    onDeleteDraft: (DraftUi) -> Unit
) {
    val filter = state.filter
    val draftsMode = filter.mode == WallMode.BORRADORES
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MySecondaryColor)
    ) {
        WallHeader(
            mode = filter.mode,
            archivedCount = state.archivedCount,
            query = filter.query,
            onQueryChange = onQueryChange,
            onModeChange = onModeChange
        )
        FilterRow(
            courses = state.courses,
            selectedCourseId = filter.courseCatalogId,
            onlyPrivate = filter.onlyPrivate,
            mode = filter.mode,
            draftCount = state.draftCount,
            showDrafts = showDrafts,
            onCourseSelected = onCourseSelected,
            onOnlyPrivateToggle = onOnlyPrivateToggle,
            onModeChange = onModeChange
        )
        ModeNotice(filter.mode)

        val count = if (draftsMode) state.drafts.size else state.announcements.size
        val searching = filter.query.isNotBlank() || filter.courseCatalogId != null || filter.onlyPrivate
        if (searching && count > 0) {
            Text(
                text = resultsLabel(count, draftsMode),
                fontFamily = Sans,
                fontSize = 13.sp,
                color = MyMutedForegroundColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        val listState = rememberLazyListState()
        // Al publicar, filtrar o cambiar de vista, el primer elemento cambia: se vuelve arriba
        val firstKey = if (draftsMode) state.drafts.firstOrNull()?.let { "d${it.id}" }
        else state.announcements.firstOrNull()?.let { "a${it.id}" }
        LaunchedEffect(firstKey) { if (firstKey != null) listState.animateScrollToItem(0) }

        // Jalar hacia abajo para actualizar (como en Instagram o Facebook): aparece el ⟳ y
        // se vuelven a pedir los anuncios al servidor
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // El mensaje de "sin resultados" va dentro de la lista para que tambien se pueda jalar
                if (count == 0 && (!state.loading || draftsMode)) {
                    item(key = "empty") { EmptyWall(filter) }
                } else if (draftsMode) {
                    items(state.drafts, key = { "d${it.id}" }) { draft ->
                        DraftCard(
                            draft = draft,
                            onOpen = { onOpenDraft(draft) },
                            onDelete = { onDeleteDraft(draft) }
                        )
                    }
                } else {
                    items(state.announcements, key = { "a${it.id}" }) { announce ->
                        SwipeToArchive(announce, onArchive) {
                            AnnounceCard(
                                announce = announce,
                                onOpen = { onOpen(announce) },
                                onArchive = { onArchive(announce) },
                                onToggleComments = { onToggleComments(announce) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resultsLabel(count: Int, drafts: Boolean): String {
    val (one, many) = if (drafts) "borrador" to "borradores" else "anuncio" to "anuncios"
    return if (count == 1) "1 $one" else "$count $many"
}

/** Aviso bajo los filtros cuando lo que se ve se guarda solo en el dispositivo. */
@Composable
private fun ModeNotice(mode: WallMode) {
    val modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    when (mode) {
        WallMode.BORRADORES ->
            LocalOnlyNote("Tus borradores se guardan en este teléfono hasta que los publiques.", modifier)
        WallMode.ARCHIVADOS ->
            LocalOnlyNote("Los anuncios archivados se guardan solo en este teléfono.", modifier)
        WallMode.MURO -> Unit
    }
}

// --------------------------------------------------------------------- cabecera

@Composable
private fun WallHeader(
    mode: WallMode,
    archivedCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onModeChange: (WallMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MyPrimaryColor)
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mode != WallMode.MURO) {
                IconButton(onClick = { onModeChange(WallMode.MURO) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al muro",
                        tint = MyPrimaryForegroundColor
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = when (mode) {
                    WallMode.MURO -> "Muro de anuncios"
                    WallMode.ARCHIVADOS -> "Archivados"
                    WallMode.BORRADORES -> "Borradores"
                },
                color = MyPrimaryForegroundColor,
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f)
            )
            if (mode == WallMode.MURO) {
                IconButton(onClick = { onModeChange(WallMode.ARCHIVADOS) }) {
                    BadgedBox(badge = {
                        if (archivedCount > 0) Badge { Text("$archivedCount") }
                    }) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = "Ver anuncios archivados",
                            tint = MyPrimaryForegroundColor
                        )
                    }
                }
            }
        }
        // Buscador siempre visible: no hay que descubrir la lupa ni abrir nada para filtrar
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    when (mode) {
                        WallMode.MURO -> "Buscar por título, contenido o alumno"
                        WallMode.ARCHIVADOS -> "Buscar en archivados"
                        WallMode.BORRADORES -> "Buscar en borradores"
                    },
                    fontFamily = Sans,
                    fontSize = 15.sp
                )
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Sans, fontSize = 15.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MySurfaceColor,
                unfocusedContainerColor = MySurfaceColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(52.dp)
        )
    }
}

@Composable
private fun FilterRow(
    courses: List<CourseUi>,
    selectedCourseId: String?,
    onlyPrivate: Boolean,
    mode: WallMode,
    draftCount: Int,
    showDrafts: Boolean,
    onCourseSelected: (String?) -> Unit,
    onOnlyPrivateToggle: () -> Unit,
    onModeChange: (WallMode) -> Unit
) {
    val draftsMode = mode == WallMode.BORRADORES
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        item {
            WallChip("Todos", selectedCourseId == null && !draftsMode) {
                onCourseSelected(null)
                if (draftsMode) onModeChange(WallMode.MURO)
            }
        }
        // Los borradores van al inicio: es lo que el docente tiene pendiente de publicar
        if (showDrafts && mode != WallMode.ARCHIVADOS) {
            item {
                WallChip(
                    label = if (draftCount > 0) "Borradores ($draftCount)" else "Borradores",
                    selected = draftsMode,
                    icon = Icons.Outlined.EditNote
                ) { onModeChange(if (draftsMode) WallMode.MURO else WallMode.BORRADORES) }
            }
        }
        items(courses, key = { it.catalogId }) { course ->
            WallChip(course.acronym, selectedCourseId == course.catalogId) {
                onCourseSelected(if (selectedCourseId == course.catalogId) null else course.catalogId)
            }
        }
        item {
            WallChip("Privados", onlyPrivate, icon = Icons.Filled.Lock, onClick = onOnlyPrivateToggle)
        }
    }
}

@Composable
private fun WallChip(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontFamily = Sans, fontWeight = FontWeight.Medium) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) } },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MySurfaceColor,
            selectedContainerColor = MyPrimaryColor,
            selectedLabelColor = MyPrimaryForegroundColor,
            selectedLeadingIconColor = MyPrimaryForegroundColor
        )
    )
}

// --------------------------------------------------------------------- tarjeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToArchive(
    announce: AnnounceUi,
    onArchive: (AnnounceUi) -> Unit,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) onArchive(announce)
            false // la lista se actualiza sola; la tarjeta no queda desplazada
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MyPrimaryColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (announce.archived) "Restaurar" else "Archivar",
                        color = MyPrimaryForegroundColor,
                        fontFamily = Sans
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (announce.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                        contentDescription = null,
                        tint = MyPrimaryForegroundColor
                    )
                }
            }
        }
    ) { content() }
}

@Composable
fun AnnounceCard(
    announce: AnnounceUi,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onToggleComments: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MySurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        // Franja de prioridad a la izquierda: se distingue lo urgente sin leer la tarjeta
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(announce.priority.color)
            )
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)
            ) {
                CardTopRow(announce)
                Text(
                    text = announce.title,
                    fontFamily = Sans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = MyForegroundColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp)
                )
                Text(
                    text = announce.content,
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = MyMutedForegroundColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                )
                CardBottomRow(announce, onArchive, onToggleComments)
            }
        }
    }
}

@Composable
private fun CourseTag(acronym: String) {
    Text(
        text = acronym,
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MyPrimaryColor,
        modifier = Modifier
            .background(MySecondaryColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun CardTopRow(announce: AnnounceUi) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
        CourseTag(announce.course.acronym)
        Spacer(Modifier.width(8.dp))
        // Publico = grupo de personas; privado = candado + nombre del alumno
        Icon(
            imageVector = if (announce.isPrivate) Icons.Filled.Lock else Icons.Filled.Groups,
            contentDescription = if (announce.isPrivate) "Privado" else "Público",
            tint = if (announce.isPrivate) MyPrimaryColor else MyMutedForegroundColor,
            modifier = Modifier.size(18.dp)
        )
        if (announce.isPrivate && announce.recipientName != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = announce.recipientName,
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MyPrimaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = announce.publishedLabel,
            fontFamily = Sans,
            fontSize = 12.sp,
            color = MyMutedForegroundColor,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun MenuText(title: String, hint: String) {
    Column {
        Text(title)
        Text(hint, fontSize = 11.sp, color = MyMutedForegroundColor)
    }
}

@Composable
private fun CardBottomRow(
    announce: AnnounceUi,
    onArchive: () -> Unit,
    onToggleComments: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(announce.priority.color, CircleShape)
        )
        Text(
            text = announce.priority.label,
            fontFamily = Sans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = announce.priority.color,
            modifier = Modifier.padding(start = 6.dp)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (announce.commentsOpen) Icons.Outlined.ChatBubbleOutline else Icons.Outlined.CommentsDisabled,
            contentDescription = if (announce.commentsOpen) "Comentarios" else "Comentarios desactivados",
            tint = MyMutedForegroundColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = announce.commentCount.toString(),
            fontFamily = Sans,
            fontSize = 13.sp,
            color = MyMutedForegroundColor,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = MyMutedForegroundColor)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = {
                        MenuText(
                            if (announce.commentsOpen) "Desactivar comentarios" else "Activar comentarios",
                            "Solo en este teléfono"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (announce.commentsOpen) Icons.Outlined.CommentsDisabled else Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null
                        )
                    },
                    onClick = { menu = false; onToggleComments() }
                )
                DropdownMenuItem(
                    text = {
                        MenuText(
                            if (announce.archived) "Restaurar al muro" else "Archivar",
                            "Solo en este teléfono"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (announce.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            contentDescription = null
                        )
                    },
                    onClick = { menu = false; onArchive() }
                )
            }
        }
    }
}

// --------------------------------------------------------------------- borrador

/**
 * Tarjeta de borrador: borde punteado, sin sombra, etiqueta "Borrador" y fecha de edicion
 * en lugar de fecha de publicacion, para que no se confunda con un anuncio ya publicado.
 */
@Composable
private fun DraftCard(draft: DraftUi, onOpen: () -> Unit, onDelete: () -> Unit) {
    val dash = MyMutedForegroundColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MySurfaceColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MyMutedColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MyForegroundColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("Borrador", fontFamily = Sans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MyForegroundColor)
            }
            Spacer(Modifier.width(8.dp))
            draft.course?.let {
                CourseTag(it.acronym)
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = if (draft.isPrivate) Icons.Filled.Lock else Icons.Filled.Groups,
                contentDescription = if (draft.isPrivate) "Privado" else "Público",
                tint = MyMutedForegroundColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = when {
                    draft.isPrivate && draft.recipientName != null -> draft.recipientName
                    draft.isPrivate -> "Sin alumno"
                    draft.course != null -> "Todo el curso"
                    else -> "Sin curso"
                },
                fontFamily = Sans,
                fontSize = 13.sp,
                color = MyMutedForegroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Editado ${draft.editedLabel}",
                fontFamily = Sans,
                fontSize = 12.sp,
                color = MyMutedForegroundColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = draft.title.ifBlank { "(Sin título)" },
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
            fontStyle = if (draft.title.isBlank()) FontStyle.Italic else FontStyle.Normal,
            fontSize = 17.sp,
            color = MyForegroundColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, end = 8.dp)
        )
        if (draft.content.isNotBlank()) {
            Text(
                text = draft.content,
                fontFamily = Sans,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = MyMutedForegroundColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, end = 8.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(draft.priority.color, CircleShape)
            )
            Text(
                text = draft.priority.label,
                fontFamily = Sans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = draft.priority.color,
                modifier = Modifier.padding(start = 6.dp)
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpen) {
                Text("Continuar", fontFamily = Sans, fontWeight = FontWeight.SemiBold, color = MyPrimaryColor)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar borrador", tint = MyMutedForegroundColor)
            }
        }
    }
}

@Composable
private fun EmptyWall(filter: WallFilter) {
    val (title, hint) = when {
        filter.mode == WallMode.BORRADORES && filter.query.isNotBlank() ->
            "Ningún borrador coincide con «${filter.query}»" to "Prueba con otra palabra."
        filter.mode == WallMode.BORRADORES -> "No tienes borradores" to
            "En «Nuevo anuncio», toca «Guardar como borrador» para terminarlo después."
        filter.query.isNotBlank() -> "Sin resultados para «${filter.query}»" to
            "Prueba con otra palabra o quita los filtros de curso."
        filter.mode == WallMode.ARCHIVADOS -> "No hay anuncios archivados" to
            "Desliza un anuncio a la izquierda para archivarlo."
        filter.courseCatalogId != null || filter.onlyPrivate -> "No hay anuncios con estos filtros" to
            "Toca «Todos» para ver el muro completo."
        else -> "Aún no has publicado anuncios" to "Toca «Nuevo anuncio» para escribir el primero."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (filter.mode == WallMode.BORRADORES) Icons.Outlined.EditNote else Icons.Filled.Search,
            contentDescription = null,
            tint = MyBorderColor,
            modifier = Modifier
                .size(56.dp)
                .background(MyMutedColor, CircleShape)
                .padding(12.dp)
        )
        Text(
            title,
            fontFamily = Sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MyForegroundColor,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(hint, fontFamily = Sans, fontSize = 14.sp, color = MyMutedForegroundColor)
    }
}
