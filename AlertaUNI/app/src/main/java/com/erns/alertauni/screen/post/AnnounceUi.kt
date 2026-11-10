package com.erns.alertauni.screen.post

import androidx.compose.ui.graphics.Color
import com.erns.alertauni.data.model.CatalogCourseEntity
import com.erns.alertauni.data.model.DraftEntity
import com.erns.alertauni.data.model.PostEntity
import com.erns.alertauni.data.model.StudentEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Nivel de prioridad de un anuncio. El color se acompana siempre del texto. */
enum class Priority(val label: String, val color: Color) {
    BAJA("Baja", Color(0xFF64748B)),
    MEDIA("Media", Color(0xFFD97706)),
    ALTA("Alta", Color(0xFFB3261E));

    companion object {
        fun from(value: String?): Priority = entries.firstOrNull { it.name == value } ?: MEDIA
    }
}

/** Curso tal como lo ve el docente: acronimo corto y nombre completo. */
data class CourseUi(
    val catalogId: String,
    val code: String,
    val acronym: String,
    val name: String
)

/** Anuncio ya preparado para dibujarse en el muro. */
data class AnnounceUi(
    val id: Int,
    val course: CourseUi,
    val isPrivate: Boolean,
    val recipientName: String?,
    val title: String,
    val content: String,
    val priority: Priority,
    val commentCount: Int,
    val commentsOpen: Boolean,
    val publishedLabel: String,
    val archived: Boolean,
    /** Fecha "yyyy-MM-dd HH:mm" para ordenar del mas reciente al mas antiguo. */
    val sortKey: String = ""
)

/** Borrador local (Room) listo para dibujarse en el muro o abrirse para editar. */
data class DraftUi(
    val id: Long,
    val course: CourseUi?,
    val isPrivate: Boolean,
    val recipientId: String?,
    val recipientName: String?,
    val priority: Priority,
    val title: String,
    val content: String,
    val editedLabel: String
)

fun DraftUi.matches(query: String): Boolean {
    val q = query.normalized()
    return q.isBlank() ||
        title.normalized().contains(q) ||
        content.normalized().contains(q) ||
        (recipientName?.normalized()?.contains(q) ?: false)
}

fun DraftEntity.toUi(courses: List<CourseUi>): DraftUi = DraftUi(
    id = id,
    course = courses.firstOrNull { it.catalogId == courseCatalogId },
    isPrivate = isPrivate,
    recipientId = recipientId,
    recipientName = recipientName,
    priority = Priority.from(priority),
    title = title,
    content = content,
    editedLabel = shortDate(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(updatedAt), ZoneId.systemDefault()).format(SOURCE)
    )
)

/** Acronimo a partir del id del catalogo (IDNP-2026B-A -> IDNP) o de las iniciales del nombre. */
fun CatalogCourseEntity.toCourseUi(): CourseUi {
    val fromId = id.substringBefore('-').takeIf { it.isNotBlank() && it != id }
    val initials = courseName.split(' ')
        .filter { it.length > 2 && it.first().isUpperCase() }
        .joinToString("") { it.first().toString() }
    return CourseUi(id, courseCode, fromId ?: initials.ifBlank { courseCode }, courseName)
}

fun StudentEntity.fullName(): String = "$firstname $surname".trim()

/** Busca por nombre, apellido o codigo, sin importar tildes ni mayusculas. */
fun StudentEntity.matches(query: String): Boolean {
    val q = query.normalized()
    return q.isBlank() || fullName().normalized().contains(q) || codigo.normalized().contains(q)
}

fun AnnounceUi.matches(query: String): Boolean {
    val q = query.normalized()
    return q.isBlank() ||
        title.normalized().contains(q) ||
        content.normalized().contains(q) ||
        course.acronym.normalized().contains(q) ||
        (recipientName?.normalized()?.contains(q) ?: false)
}

fun String.normalized(): String =
    java.text.Normalizer.normalize(lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .trim()

private val ES = Locale("es", "PE")
private val SOURCE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", ES)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", ES)

/**
 * "2026-10-01 01:28" -> "Hoy 01:28", "Ayer 01:28", "1 oct 01:28" o "1 oct 2025".
 * El ano solo aparece cuando no es el actual; los segundos nunca.
 */
fun shortDate(published: String, today: LocalDate = LocalDate.now()): String {
    val date = runCatching { LocalDateTime.parse(published.take(16).replace('T', ' '), SOURCE) }
        .getOrNull() ?: return published
    val day = date.toLocalDate()
    return when {
        day == today -> "Hoy ${date.format(TIME)}"
        day == today.minusDays(1) -> "Ayer ${date.format(TIME)}"
        day.year == today.year -> "${date.format(DAY_MONTH).replace(".", "")} ${date.format(TIME)}"
        else -> date.format(DAY_MONTH_YEAR).replace(".", "")
    }
}

fun PostEntity.toUi(
    courses: Map<String, CourseUi>,
    recipients: Map<String, String>,
    priority: Priority,
    commentsClosed: Boolean,
    archived: Boolean
): AnnounceUi {
    val course = courses[courseCode] ?: CourseUi("", courseCode, courseCode, courseName.orEmpty())
    return AnnounceUi(
        id = id,
        course = course,
        isPrivate = isPrivate,
        recipientName = receiverPrivate?.takeIf { isPrivate && it.isNotBlank() }
            ?.let { recipients[it] ?: "Un estudiante" },
        title = title,
        content = content,
        priority = priority,
        commentCount = countComments,
        commentsOpen = allowComments && !commentsClosed,
        publishedLabel = shortDate(date),
        archived = archived,
        sortKey = date.take(16).replace('T', ' ')
    )
}
