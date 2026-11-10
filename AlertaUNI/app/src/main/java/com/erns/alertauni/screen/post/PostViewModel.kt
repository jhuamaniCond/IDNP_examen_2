package com.erns.alertauni.screen.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erns.alertauni.data.local.AnnounceLocalStore
import com.erns.alertauni.data.local.SessionOwner
import com.erns.alertauni.data.model.CourseCatalogRequest
import com.erns.alertauni.data.model.DraftEntity
import com.erns.alertauni.data.model.PostEntity
import com.erns.alertauni.data.model.PostPrivateRequest
import com.erns.alertauni.data.model.PostRequest
import com.erns.alertauni.data.model.StudentEntity
import com.erns.alertauni.data.repository.AnnounceRepository
import com.erns.alertauni.data.repository.ContactRepository
import com.erns.alertauni.data.repository.DraftRepository
import com.erns.alertauni.data.repository.PostRepository
import com.erns.alertauni.domain.manager.DataStoreHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Que se esta mirando en el muro. */
enum class WallMode { MURO, ARCHIVADOS, BORRADORES }

/** Filtros del muro: texto buscado, curso elegido, solo privados y la vista activa. */
data class WallFilter(
    val query: String = "",
    val courseCatalogId: String? = null,
    val onlyPrivate: Boolean = false,
    val mode: WallMode = WallMode.MURO
)

data class WallState(
    val courses: List<CourseUi> = emptyList(),
    val announcements: List<AnnounceUi> = emptyList(),
    val drafts: List<DraftUi> = emptyList(),
    val draftCount: Int = 0,
    val archivedCount: Int = 0,
    val filter: WallFilter = WallFilter(),
    val loading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val announceRepository: AnnounceRepository,
    private val contactRepository: ContactRepository,
    private val draftRepository: DraftRepository,
    private val localStore: AnnounceLocalStore,
    private val sessionOwner: SessionOwner,
    private val dataStoreHelper: DataStoreHelper
) : ViewModel() {
    private val TAG = "PostViewModel"

    private val posts = MutableStateFlow<List<PostEntity>>(emptyList())
    private val courses = MutableStateFlow<List<CourseUi>>(emptyList())
    private val loading = MutableStateFlow(true)

    /** Alumnos de cada curso del docente: catalogId -> alumnos. */
    private val _students = MutableStateFlow<Map<String, List<StudentEntity>>>(emptyMap())
    val students: StateFlow<Map<String, List<StudentEntity>>> = _students.asStateFlow()

    /** Cuenta con sesion iniciada: separa borradores y datos locales por usuario. */
    private val owner = MutableStateFlow<String?>(null)
    private var store: AnnounceLocalStore.UserStore? = null

    private val local = MutableStateFlow(LocalData())
    private val filter = MutableStateFlow(WallFilter())

    /** Borradores (Room) solo de la cuenta actual; se actualizan solos al guardar o borrar. */
    private val drafts: StateFlow<List<DraftEntity>> = owner.filterNotNull()
        .flatMapLatest { draftRepository.observe(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    /** true mientras se recargan los anuncios: hace girar el indicador de "jalar para actualizar". */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _isProfessor = MutableStateFlow(false)
    val isProfessor: StateFlow<Boolean> = _isProfessor.asStateFlow()

    private var lastDeletedDraft: DraftEntity? = null

    /** Anuncios del servidor ya convertidos para el muro, del mas reciente al mas antiguo. */
    private val sources = combine(posts, courses, _students, local) { posts, courses, students, local ->
        val recipients = students.values.flatten().associate { it.student_id to it.fullName() }
        val byCode = courses.associateBy { it.code }
        val all = posts
            .map { post ->
                post.toUi(
                    courses = byCode,
                    recipients = recipients,
                    priority = Priority.from(post.priority),
                    commentsClosed = post.id in local.closedComments,
                    archived = post.id in local.archived
                )
            }
            .sortedByDescending { it.sortKey }
        Sources(courses, all)
    }

    val state: StateFlow<WallState> = combine(sources, drafts, filter, loading) { src, drafts, filter, loading ->
        val selectedCode = src.courses.firstOrNull { it.catalogId == filter.courseCatalogId }?.code
        val draftsUi = drafts.map { it.toUi(src.courses) }
        WallState(
            courses = src.courses,
            announcements = if (filter.mode == WallMode.BORRADORES) emptyList() else src.all
                .filter { it.archived == (filter.mode == WallMode.ARCHIVADOS) }
                .filter { selectedCode == null || it.course.code == selectedCode }
                .filter { !filter.onlyPrivate || it.isPrivate }
                .filter { it.matches(filter.query) },
            drafts = if (filter.mode != WallMode.BORRADORES) emptyList() else draftsUi
                .filter { filter.courseCatalogId == null || it.course?.catalogId == filter.courseCatalogId }
                .filter { !filter.onlyPrivate || it.isPrivate }
                .filter { it.matches(filter.query) },
            draftCount = drafts.size,
            archivedCount = src.all.count { it.archived },
            filter = filter,
            loading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WallState())

    init {
        viewModelScope.launch {
            _isProfessor.value = dataStoreHelper.getUserType() == "PROFESSOR"
            val id = sessionOwner.id()
            store = localStore.forUser(id)
            local.value = readLocal()
            owner.value = id
        }
        loadCourses()
        refresh()
    }

    // ------------------------------------------------------------------ filtros
    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }
    fun onCourseSelected(catalogId: String?) = filter.update { it.copy(courseCatalogId = catalogId) }
    fun onOnlyPrivateToggle() = filter.update { it.copy(onlyPrivate = !it.onlyPrivate) }
    fun onModeChange(mode: WallMode) = filter.update { it.copy(mode = mode) }

    // ------------------------------------------------------------------ gestion (simulada, local)
    fun setArchived(postId: Int, archived: Boolean) {
        store?.setArchived(postId, archived)
        local.value = readLocal()
    }

    fun setCommentsOpen(postId: Int, open: Boolean) {
        store?.setCommentsClosed(postId, !open)
        local.value = readLocal()
        _messages.tryEmit(
            if (open) "Comentarios activados en este teléfono"
            else "Comentarios desactivados en este teléfono"
        )
    }

    // ------------------------------------------------------------------ borradores (Room)
    /**
     * Guarda el formulario como borrador. Con [draftId] nulo crea uno nuevo; si no,
     * actualiza el que se estaba editando.
     */
    fun saveDraft(
        draftId: Long?,
        courseCatalogId: String?,
        student: StudentEntity?,
        isPrivate: Boolean,
        priority: Priority,
        title: String,
        content: String,
        onSaved: () -> Unit = {}
    ) {
        val ownerId = owner.value ?: run {
            _messages.tryEmit("No se pudo identificar tu cuenta para guardar el borrador.")
            return
        }
        viewModelScope.launch {
            draftRepository.save(
                DraftEntity(
                    id = draftId ?: 0,
                    ownerId = ownerId,
                    courseCatalogId = student?.course_catalog_id?.takeIf { it.isNotBlank() } ?: courseCatalogId,
                    isPrivate = isPrivate,
                    recipientId = student?.student_id,
                    recipientName = student?.fullName(),
                    priority = priority.name,
                    title = title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _messages.tryEmit("Borrador guardado")
            onSaved()
        }
    }

    fun deleteDraft(draftId: Long) {
        val ownerId = owner.value ?: return
        lastDeletedDraft = drafts.value.firstOrNull { it.id == draftId }
        viewModelScope.launch { draftRepository.delete(draftId, ownerId) }
    }

    fun undoDeleteDraft() {
        val draft = lastDeletedDraft ?: return
        lastDeletedDraft = null
        viewModelScope.launch { draftRepository.save(draft) }
    }

    // ------------------------------------------------------------------ envio (backend)
    /**
     * Publica el anuncio. Si [student] es nulo va a todo el curso; si no, solo a ese alumno.
     * Si venia de un borrador ([draftId]) y el backend lo acepta, el borrador se elimina.
     */
    fun send(
        courseCatalogId: String,
        student: StudentEntity?,
        priority: Priority,
        title: String,
        content: String,
        draftId: Long? = null,
        onDone: (Boolean) -> Unit
    ) {
        if (_sending.value) return
        _sending.value = true
        viewModelScope.launch {
            val postId: Result<Int> = if (student == null) {
                repository.sendPost(
                    PostRequest(
                        course_catalog_id = courseCatalogId,
                        title = title,
                        content = content,
                        is_private = false,
                        allow_comments = true
                    )
                ).map { it.post_id }
            } else {
                announceRepository.addPostPrivate(
                    PostPrivateRequest(
                        courseCatalogId = courseCatalogId,
                        title = title,
                        content = content,
                        receiverPrivate = student.student_id,
                        allowComments = true
                    )
                ).map { it.post_id }
            }
            postId.onSuccess { id ->
                // La prioridad se guarda en el backend (post.priority) despues de crear el anuncio
                if (id > 0 && priority != Priority.MEDIA) {
                    announceRepository.setPriority(id, priority.name)
                        .onFailure {
                            Log.d(TAG, "Error al guardar prioridad: ${it.message}")
                            _messages.tryEmit("Se publicó, pero no se pudo guardar la prioridad")
                        }
                }
                val ownerId = owner.value
                if (draftId != null && ownerId != null) draftRepository.delete(draftId, ownerId)
                _messages.tryEmit(
                    if (student == null) "Anuncio publicado" else "Anuncio enviado a ${student.firstname}"
                )
                refresh()
            }.onFailure {
                Log.d(TAG, "Error al publicar: ${it.message}")
                _messages.tryEmit("No se pudo publicar. Puedes guardarlo como borrador e intentarlo luego.")
            }
            _sending.value = false
            onDone(postId.isSuccess)
        }
    }

    // ------------------------------------------------------------------ carga
    /** Vuelve a pedir los anuncios al servidor (al abrir, al publicar o al deslizar hacia abajo). */
    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            repository.getPosts()
                .onSuccess { posts.value = it }
                .onFailure { Log.d(TAG, "Error al cargar anuncios: ${it.message}") }
            loading.value = false
            _refreshing.value = false
        }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            repository.getCourseCatalog().onSuccess { catalog ->
                courses.value = catalog.map { it.toCourseUi() }
                // Los alumnos se cargan una vez para buscarlos al escribir un anuncio privado
                val byCourse = catalog.map { course ->
                    async {
                        course.id to contactRepository
                            .getStudentsByCourse(CourseCatalogRequest(course.id))
                            .getOrDefault(emptyList())
                    }
                }.awaitAll().toMap()
                _students.value = byCourse
            }.onFailure {
                Log.d(TAG, "Error al cargar cursos: ${it.message}")
            }
        }
    }

    private data class Sources(val courses: List<CourseUi>, val all: List<AnnounceUi>)

    private data class LocalData(
        val archived: Set<Int> = emptySet(),
        val closedComments: Set<Int> = emptySet()
    )

    private fun readLocal(): LocalData {
        val s = store ?: return LocalData()
        return LocalData(
            archived = s.archivedIds(),
            closedComments = s.closedCommentIds()
        )
    }
}
