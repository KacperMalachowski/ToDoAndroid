package pl.kacpermalachowski.todo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


fun serializeTaskListToJson(tasks: List<Task>): String {
    val gson = Gson()
    return gson.toJson(tasks)
}

fun deserializeTaskListFromJson(tasksJson: String): List<Task> {
    val gson = Gson()
    val taskListType = object : TypeToken<List<Task>>() {}.type
    return gson.fromJson(tasksJson, taskListType)
}