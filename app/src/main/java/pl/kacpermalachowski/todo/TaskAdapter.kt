package pl.kacpermalachowski.todo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class TaskAdapter(private val taskList: MutableList<Task>) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.bind(task)
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val completedCheckBox: CheckBox = itemView.findViewById(R.id.completedCheckBox)
        private val deleteBtn: Button = itemView.findViewById(R.id.deleteBtn)
        private val editBtn: Button = itemView.findViewById(R.id.editBtn)
        private val dueDate: TextView = itemView.findViewById(R.id.dueDateTextView)
        private val priority: TextView = itemView.findViewById(R.id.priorityTextView)

        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description
            completedCheckBox.isChecked = task.isCompleted
            val year = task.dueDate.get(Calendar.YEAR)
            val month = task.dueDate.get(Calendar.MONTH)
            val dayOfMonth = task.dueDate.get(Calendar.DAY_OF_MONTH)
            dueDate.text = "Due to: $dayOfMonth.$month.$year"
            priority.text = "Priority: ${task.priority}"


            completedCheckBox.setOnCheckedChangeListener { _, isChecked ->
                task.isCompleted = isChecked

            }

            deleteBtn.setOnClickListener {
                val taskIdx = taskList.indexOf(task)
                taskList.removeAt(taskIdx)
                this@TaskAdapter.notifyItemRemoved(taskIdx)
            }

            editBtn.setOnClickListener {
                val dialogView = LayoutInflater.from(it.context).inflate(R.layout.dialog_add_task, null)
                val titleEditText: EditText = dialogView.findViewById(R.id.titleEditText)
                val descriptionEditText: EditText = dialogView.findViewById(R.id.descriptionEditText)
                val dueDatePicker: DatePicker = dialogView.findViewById(R.id.dueDatePicker)
                val priorityPicker: EditText = dialogView.findViewById(R.id.priorityEditText)

                titleEditText.setText(task.title)
                descriptionEditText.setText(task.description)
                dueDatePicker.updateDate(year, month, dayOfMonth)
                priorityPicker.setText(task.priority.toString())

                AlertDialog.Builder(it.context)
                    .setTitle("Edit ${task.title}")
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

                        val taskId = task.id
                        val newTask = Task(taskId, title, description, task.isCompleted, dueDate, priority)

                        val idx = taskList.indexOf(task)
                        taskList[idx] = newTask

                        this@TaskAdapter.notifyItemChanged(idx)

                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }
}