package pl.kacpermalachowski.todo

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskArray = inputData.getString("task_list")?.let { deserializeTaskListFromJson(it) }
        val taskList: List<Task> = taskArray?.mapNotNull { it as? Task }!!

        val filtredTasks =  getTasksDueTodayOrOverdue(taskList)
        if (taskList != null) {
            for (task in filtredTasks) {
                val notification = createNotification(task)
                sendNotification(notification)
            }
        }

        return Result.success()
    }

    private fun getTasksDueTodayOrOverdue(taskList: List<Task>): List<Task> {
        val today = Calendar.getInstance()
        val filteredTasks = taskList.filter { task ->
            val taskDueDate = task.dueDate
            taskDueDate <= today
        }
        return filteredTasks
    }

    private fun createNotification(task: Task): Notification {
        // Build the notification using NotificationCompat.Builder
        val notificationBuilder = NotificationCompat.Builder(this.applicationContext, "TaskReminder")
            .setContentTitle(task.title)
            .setContentText(task.description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        return notificationBuilder.build()
    }

    private fun sendNotification(notification: Notification) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a unique notification ID
        val notificationId = System.currentTimeMillis().toInt()

        // Send the notification
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val TAG = "TaskReminderWorker"
        const val TASK_LIST_KEY = "task_list"
    }
}