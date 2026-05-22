package com.onlineaq.student.ui.resultdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
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

    private var resultId: Int = 0

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
                            bindData(detail)
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

        val adapter = AnswerDetailAdapter(detail.answers)
        rvAnswers.adapter = adapter
    }
}

class AnswerDetailAdapter(
    private val answers: List<ResultAnswer>
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
        }
    }
}
