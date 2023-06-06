package pl.kacpermalachowski.todo

import java.util.Calendar

class Task(
    id: Int,
    title: String,
    description: String,
    var isCompleted: Boolean,
    val dueDate: Calendar,
    val priority: Int
) {
    val id = id
    val title = title
        get() = if (field == "") "No Title" else field
    val description = description
        get() = if (field == "") "No Description" else field
}
