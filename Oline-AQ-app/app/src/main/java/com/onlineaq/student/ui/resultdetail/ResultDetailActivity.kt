package com.onlineaq.student.ui.resultdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.FeedbackCreateRequest
import com.onlineaq.student.data.model.ResultAnswer
import com.onlineaq.student.data.model.ResultDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultDetailActivity : AppCompatActivity() {

    private lateinit var tvScore: TextView
    private lateinit var tvCorrect: TextView
    private lateinit var tvWrong: TextView
    private lateinit var rvAnswers: RecyclerView
    private val submittedFeedbackIds = mutableSetOf<Int>()

    private var resultId: Int = 0
    private var currentDetail: ResultDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_detail)

        resultId = intent.getIntExtra("result_id", 0)
        if (resultId == 0) {
            finish()
            return
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvScore = findViewById(R.id.tv_score)
        tvCorrect = findViewById(R.id.tv_correct)
        tvWrong = findViewById(R.id.tv_wrong)
        rvAnswers = findViewById(R.id.rv_answers)
        rvAnswers.layoutManager = LinearLayoutManager(this)

        loadDetail()
    }

    private fun loadDetail() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getResultDetail(resultId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val detail = response.body()?.data
                        if (detail != null) {
                            currentDetail = detail
                            loadSubmittedFeedbackIds(detail)
                        }
                    } else {
                        Toast.makeText(this@ResultDetailActivity, "加载成绩详情失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindData(detail: ResultDetail) {
        val result = detail.result
        tvScore.text = result.totalScore?.toInt()?.toString() ?: "0"
        tvCorrect.text = result.correctCount?.toString() ?: "0"
        tvWrong.text = result.wrongCount?.toString() ?: "0"

        val adapter = AnswerDetailAdapter(detail.answers, submittedFeedbackIds) { answer ->
            showFeedbackDialog(answer)
        }
        rvAnswers.adapter = adapter
    }

    private fun loadSubmittedFeedbackIds(detail: ResultDetail) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ids = detail.answers.joinToString(",") { it.questionId.toString() }
                val response = RetrofitClient.apiService.getMyFeedbackQuestionIds(ids)
                withContext(Dispatchers.Main) {
                    submittedFeedbackIds.clear()
                    submittedFeedbackIds.addAll(response.body()?.data.orEmpty())
                    bindData(detail)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { bindData(detail) }
            }
        }
    }

    private fun showFeedbackDialog(answer: ResultAnswer) {
        val options = arrayOf("answer_error", "content_error", "option_error", "other")
        var selectedType = options[0]
        val view = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_feedback_desc)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("题目纠错反馈")
            .setSingleChoiceItems(arrayOf("答案错误", "题干错误", "选项错误", "其他问题"), 0) { _, which ->
                selectedType = options[which]
            }
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton("提交") { _, _ ->
                val desc = etDesc.text?.toString()?.trim().orEmpty()
                if (desc.isBlank()) {
                    Toast.makeText(this, "请填写反馈说明", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                submitFeedback(answer.questionId, selectedType, desc)
            }
            .show()
    }

    private fun submitFeedback(questionId: Int, feedbackType: String, description: String) {
        val examId = currentDetail?.result?.examId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.submitFeedback(
                    FeedbackCreateRequest(
                        questionId = questionId,
                        examId = examId,
                        feedbackType = feedbackType,
                        description = description,
                    )
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        submittedFeedbackIds.add(questionId)
                        (rvAnswers.adapter as? AnswerDetailAdapter)?.notifyDataSetChanged()
                        Toast.makeText(this@ResultDetailActivity, "反馈已提交", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ResultDetailActivity, response.body()?.message ?: "反馈提交失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultDetailActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class AnswerDetailAdapter(
    private val answers: List<ResultAnswer>,
    private val submittedFeedbackIds: Set<Int>,
    private val onFeedbackClick: (ResultAnswer) -> Unit,
) : RecyclerView.Adapter<AnswerDetailAdapter.AnswerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_answer_detail, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        holder.bind(answers[position], position)
    }

    override fun getItemCount() = answers.size

    inner class AnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex = itemView.findViewById<TextView>(R.id.tv_q_index)
        private val tvResult = itemView.findViewById<TextView>(R.id.tv_q_result)
        private val tvContent = itemView.findViewById<TextView>(R.id.tv_q_content)
        private val tvYourAnswer = itemView.findViewById<TextView>(R.id.tv_your_answer)
        private val tvCorrectAnswer = itemView.findViewById<TextView>(R.id.tv_correct_answer)
        private val btnFeedback = itemView.findViewById<MaterialButton>(R.id.btn_feedback)
        private val optionContainer = itemView.findViewById<LinearLayout>(R.id.layout_option_container)
        private val optionViews = listOf(
            itemView.findViewById<TextView>(R.id.tv_option_a),
            itemView.findViewById<TextView>(R.id.tv_option_b),
            itemView.findViewById<TextView>(R.id.tv_option_c),
            itemView.findViewById<TextView>(R.id.tv_option_d),
        )

        fun bind(answer: ResultAnswer, position: Int) {
            val typeLabel = when (answer.questionType) {
                "single" -> "单选题"
                "judge" -> "判断题"
                "fill_blank" -> "填空题"
                "short_answer" -> "简答题"
                else -> answer.questionType
            }
            tvIndex.text = "第 ${position + 1} 题 ($typeLabel)"
            tvContent.text = answer.questionContent
            bindOptions(answer)

            val isCorrect = answer.isCorrect == true
            tvResult.text = if (isCorrect) "正确" else "错误"
            tvResult.setBackgroundColor(
                itemView.context.getColor(
                    if (isCorrect) R.color.ctp_green
                    else R.color.ctp_red
                )
            )

            tvYourAnswer.text = "你的答案: ${answer.studentAnswer ?: "未作答"}"
            tvCorrectAnswer.text = "正确答案: ${answer.correctAnswer ?: ""}"
            tvCorrectAnswer.visibility = if (isCorrect) View.GONE else View.VISIBLE
            if (submittedFeedbackIds.contains(answer.questionId)) {
                btnFeedback.text = "已反馈"
                btnFeedback.isEnabled = false
            } else {
                btnFeedback.text = "反馈纠错"
                btnFeedback.isEnabled = true
                btnFeedback.setOnClickListener { onFeedbackClick(answer) }
            }
        }

        private fun bindOptions(answer: ResultAnswer) {
            if (answer.questionType != "single" && answer.questionType != "judge") {
                optionContainer.visibility = View.GONE
                return
            }
            optionContainer.visibility = View.VISIBLE

            val options = listOf(
                "A" to answer.optionA,
                "B" to answer.optionB,
                "C" to answer.optionC,
                "D" to answer.optionD,
            )

            optionViews.forEachIndexed { index, textView ->
                val (letter, rawText) = options[index]
                if (rawText.isNullOrBlank()) {
                    textView.visibility = View.GONE
                } else {
                    textView.visibility = View.VISIBLE
                    textView.text = "$letter. $rawText"
                    val isCorrect = answer.correctAnswer?.contains(letter) == true
                    textView.setBackgroundResource(if (isCorrect) R.drawable.bg_option_correct else R.drawable.bg_option_neutral)
                }
            }
        }
    }
}
