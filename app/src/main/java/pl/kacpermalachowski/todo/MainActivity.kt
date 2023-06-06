package pl.kacpermalachowski.todo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private var taskList = mutableListOf<Task>()
    private var selectedSortCriteria: String = "Alphabetically"

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        taskAdapter = TaskAdapter(taskList)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }

        val inputData = Data.Builder()
            .putString("task_list", serializeTaskListToJson(taskList))
            .build()

        val taskReminderWorkRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInputData(inputData)
            .build()

        val onceReminder = OneTimeWorkRequestBuilder<TaskReminderWorker>().build()



        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                TaskReminderWorker.TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                taskReminderWorkRequest
            )


        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            showAddTaskDialog()
        }

        val btnExport = findViewById<Button>(R.id.btnExport)
        val btnImport = findViewById<Button>(R.id.btnImport)

        btnExport.setOnClickListener {
            val fileName = "task_list.txt"
            exportTaskListToTextFile(taskList, fileName)
            Toast.makeText(this, "Task list exported.", Toast.LENGTH_SHORT).show()
        }

        btnImport.setOnClickListener {
            val fileName = "task_list.txt"
            val importedTaskList = importTaskListFromTextFile(fileName)
            taskList.addAll(importedTaskList)
            val unique = uniqueTask(taskList)

            taskList.clear()
            taskList.addAll(unique)
            Toast.makeText(this, "Task list imported.", Toast.LENGTH_SHORT).show()
            sortTaskList()
        }

        val spinner: Spinner = findViewById(R.id.sortSpinner)
        val adapterSPin = ArrayAdapter.createFromResource(
            this,
            R.array.sortOptions,
            android.R.layout.simple_spinner_item
        )
        adapterSPin.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapterSPin

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSortCriteria = parent?.getItemAtPosition(position) as String
                sortTaskList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun sortTaskList() {
        when (selectedSortCriteria) {
            "Alphabetically" -> taskList.sortBy { it.title }
            "Due Date" -> taskList.sortBy { it.dueDate.time }
            "Priority" -> taskList.sortBy { it.priority }
            "Completion" -> taskList.sortBy { it.isCompleted }
        }

        taskAdapter.notifyDataSetChanged()
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val titleEditText: EditText = dialogView.findViewById(R.id.titleEditText)
        val descriptionEditText: EditText = dialogView.findViewById(R.id.descriptionEditText)
        val dueDatePicker: DatePicker = dialogView.findViewById(R.id.dueDatePicker)
        val priorityPicker: EditText = dialogView.findViewById(R.id.priorityEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                var priority  = 0
                if (priorityPicker.text.toString().isNotEmpty()) {
                    priority = priorityPicker.text.toString().toInt()
                }

                val dueDate = Calendar.getInstance()
                dueDate.set(
                    dueDatePicker.year,
                    dueDatePicker.month,
                    dueDatePicker.dayOfMonth
                )

                val taskId = taskList.size + 1;
                val task = Task(taskId, title, description, false, dueDate, priority)

                taskList.add(task)
                sortTaskList()

                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun exportTaskListToTextFile(taskList: List<Task>, fileName: String) {
        val text = StringBuilder()

        taskList.forEach{
            text
                .append(it.id).append(",")
                .append(it.title).append(",")
                .append(it.description).append(",")
                .append(it.isCompleted).append(",")
                .append(SimpleDateFormat("MM-dd-yyyy").format(it.dueDate.time)).append(",")
                .append(it.priority).append("\n")
        }

        try {
            val file = File(getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(text.toString().toByteArray())
            outputStream.close()
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    private fun importTaskListFromTextFile(fileName: String): List<Task> {
        val taskList = mutableListOf<Task>()

        try {
            val file = File(getExternalFilesDir(null), fileName)
            val inputStream = FileInputStream(file)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val taskData = line!!.split(",").map { it.trim() }

                Log.i("import", line!!)
                val calendar = Calendar.getInstance()
                val formatter = SimpleDateFormat("MM-dd-yyyy")
                try {
                    val date = formatter.parse(taskData[4])
                    calendar.time = date
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                var priority  = 0
                if (taskData[5].isNotEmpty()) {
                    priority = taskData[5].toInt()
                }

                val task = Task(
                    id = taskData[0].toInt(),
                    title = taskData[1],
                    description = taskData[2],
                    isCompleted = taskData[3].toBooleanStrict(),
                    dueDate = calendar,
                    priority = priority
                )

                taskList.add(task)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return taskList
    }

    private fun uniqueTask(tasks: List<Task>): List<Task> {
        val uniqueTasks = HashSet<String>()
        val result = mutableListOf<Task>()

        for (task in tasks) {
            val taskKey = "${task.title}:${task.description}"
            if (uniqueTasks.add(taskKey)) {
                result.add(task)
            }
        }

        return result

    }

}