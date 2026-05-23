package com.onlineaq.student.ui.wrongbook

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.AddNotebookItemRequest
import com.onlineaq.student.data.model.NotebookDetail
import com.onlineaq.student.data.model.WrongNotebook
import com.onlineaq.student.data.model.WrongQuestionGroup
import com.onlineaq.student.data.model.WrongQuestionItem
import com.onlineaq.student.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WrongBookDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvSummary: TextView
    private lateinit var containerGroups: LinearLayout
    private var notebookId: String = "all"
    private var notebooks: List<WrongNotebook> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wrong_book_detail)

        notebookId = intent.getStringExtra("notebook_id") ?: "all"
        toolbar = findViewById(R.id.toolbar)
        tvSummary = findViewById(R.id.tv_detail_summary)
        containerGroups = findViewById(R.id.layout_wrong_groups)
        toolbar.title = intent.getStringExtra("notebook_title") ?: "错题本详情"
        toolbar.setNavigationOnClickListener { finish() }
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                notebooks = RetrofitClient.apiService.getWrongNotebooks().body()?.data.orEmpty()
                if (notebookId == "all") {
                    val response = RetrofitClient.apiService.getWrongQuestions()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.code == 200) {
                            renderGroups(response.body()?.data.orEmpty(), null)
                        } else {
                            Toast.makeText(this@WrongBookDetailActivity, "加载错题失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val response = RetrofitClient.apiService.getWrongNotebookDetail(notebookId.toInt())
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.code == 200) {
                            renderGroups(response.body()?.data?.groups.orEmpty(), response.body()?.data)
                        } else {
                            Toast.makeText(this@WrongBookDetailActivity, "加载错题本详情失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WrongBookDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderGroups(groups: List<WrongQuestionGroup>, detail: NotebookDetail?) {
        containerGroups.removeAllViews()
        val total = groups.sumOf { it.questions.size }
        tvSummary.text = if (detail == null) "全部错题，共 $total 道，涉及 ${groups.size} 场考试" else "${detail.notebook.notebookName}，共 $total 道错题"
        if (groups.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "暂无错题"
                textSize = 15f
                setPadding(0, 32, 0, 32)
                setTextColor(getColor(R.color.gray_500))
            }
            containerGroups.addView(emptyView)
            return
        }

        groups.forEach { group ->
            val groupView = layoutInflater.inflate(R.layout.item_wrong_group, containerGroups, false)
            groupView.findViewById<TextView>(R.id.tv_group_title).text = group.examName
            groupView.findViewById<TextView>(R.id.tv_group_subtitle).text = "共 ${group.questions.size} 道题"
            val questionContainer = groupView.findViewById<LinearLayout>(R.id.layout_group_questions)
            group.questions.forEach { item ->
                val questionView = layoutInflater.inflate(R.layout.item_wrong_question, questionContainer, false)
                bindQuestion(questionView, item)
                questionContainer.addView(questionView)
            }
            containerGroups.addView(groupView)
        }
    }

    private fun bindQuestion(view: View, item: WrongQuestionItem) {
        bindOptions(view, item)
        view.findViewById<TextView>(R.id.tv_question_type).text = when (item.questionType) {
            "single" -> "单选题"
            "judge" -> "判断题"
            "fill_blank" -> "填空题"
            "short_answer" -> "简答题"
            else -> item.questionType
        }
        view.findViewById<TextView>(R.id.tv_question_content).text = item.questionContent
        view.findViewById<TextView>(R.id.tv_student_answer).text = "你的答案：${item.studentAnswer ?: "未作答"}"
        view.findViewById<TextView>(R.id.tv_correct_answer).text = "正确答案：${item.correctAnswer ?: ""}    分值：${item.score?.toInt() ?: 0}"
        view.findViewById<TextView>(R.id.tv_submit_time).text = item.submitTime?.replace("T", " ")?.take(19) ?: ""
        val primaryButton = view.findViewById<MaterialButton>(R.id.btn_primary_action)
        if (notebookId == "all") {
            primaryButton.text = "加入错题本"
            primaryButton.setOnClickListener { chooseNotebookAndAdd(item.answerId) }
        } else {
            primaryButton.text = "移出"
            primaryButton.setOnClickListener { removeItem(item.answerId) }
        }
    }

    private fun bindOptions(view: View, item: WrongQuestionItem) {
        val optionContainer = view.findViewById<LinearLayout>(R.id.layout_option_container)
        if (item.questionType != "single" && item.questionType != "judge") {
            optionContainer.visibility = View.GONE
            return
        }
        optionContainer.visibility = View.VISIBLE

        val optionViews = listOf(
            view.findViewById<TextView>(R.id.tv_option_a),
            view.findViewById<TextView>(R.id.tv_option_b),
            view.findViewById<TextView>(R.id.tv_option_c),
            view.findViewById<TextView>(R.id.tv_option_d),
        )
        val options = listOf(
            "A" to item.optionA,
            "B" to item.optionB,
            "C" to item.optionC,
            "D" to item.optionD,
        )
        optionViews.forEachIndexed { index, textView ->
            val (letter, rawText) = options[index]
            if (rawText.isNullOrBlank()) {
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE
                textView.text = "$letter. $rawText"
                val isCorrect = item.correctAnswer?.contains(letter) == true
                textView.setBackgroundResource(if (isCorrect) R.drawable.bg_option_correct else R.drawable.bg_option_neutral)
            }
        }
    }

    private fun chooseNotebookAndAdd(answerId: Int) {
        if (notebooks.isEmpty()) {
            Toast.makeText(this, "请先创建错题本", Toast.LENGTH_SHORT).show()
            return
        }
        val names = notebooks.map { "${it.notebookName} (${it.itemCount}道)" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择错题本")
            .setItems(names) { _, which -> addItem(notebooks[which].notebookId, answerId) }
            .show()
    }

    private fun addItem(notebookId: Int, answerId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.addWrongNotebookItem(notebookId, AddNotebookItemRequest(answerId))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WrongBookDetailActivity, response.body()?.message ?: "已加入错题本", Toast.LENGTH_SHORT).show()
                    notebooks = RetrofitClient.apiService.getWrongNotebooks().body()?.data.orEmpty()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WrongBookDetailActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun removeItem(itemId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.removeWrongNotebookItem(notebookId.toInt(), itemId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        loadData()
                    } else {
                        Toast.makeText(this@WrongBookDetailActivity, response.body()?.message ?: "移除失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WrongBookDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
