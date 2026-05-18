package com.onlineaq.student.ui.examdetail

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.*
import com.onlineaq.student.ui.resultdetail.ResultDetailActivity
import com.onlineaq.student.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExamDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var tvTimer: TextView
    private lateinit var tvProgress: TextView
    private lateinit var rvQuestions: RecyclerView
    private lateinit var btnSubmit: com.google.android.material.button.MaterialButton

    private var examId: Int = 0
    private var questions: List<Question> = emptyList()
    private val answers = mutableMapOf<Int, String>()
    private var timer: CountDownTimer? = null
    private var remainingSeconds: Int = 0
    private var usedSeconds: Int = 0
    private var examDurationMinutes: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam_detail)

        examId = intent.getIntExtra("exam_id", 0)
        if (examId == 0) {
            finish()
            return
        }

        toolbar = findViewById(R.id.toolbar)
        tvTimer = findViewById(R.id.tv_timer)
        tvProgress = findViewById(R.id.tv_progress)
        rvQuestions = findViewById(R.id.rv_questions)
        btnSubmit = findViewById(R.id.btn_submit)

        toolbar.setNavigationOnClickListener { finish() }
        btnSubmit.setOnClickListener { attemptSubmit() }

        rvQuestions.layoutManager = LinearLayoutManager(this)

        loadExamDetail()
    }

    private fun loadExamDetail() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getExamDetail(examId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val detail = response.body()?.data
                        if (detail != null) {
                            questions = detail.questions
                            examDurationMinutes = detail.exam.duration
                            toolbar.title = detail.exam.examName
                            setupQuestions()
                            startTimer()
                        }
                    } else {
                        Toast.makeText(this@ExamDetailActivity, "加载考试失败", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExamDetailActivity, "网络错误", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun setupQuestions() {
        val adapter = QuestionAdapter(questions, answers)
        rvQuestions.adapter = adapter
        updateProgress()
    }

    private fun startTimer() {
        val totalSeconds = examDurationMinutes * 60
        remainingSeconds = totalSeconds
        usedSeconds = 0

        timer = object : CountDownTimer((totalSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                usedSeconds = totalSeconds - remainingSeconds
                updateTimerDisplay()
            }

            override fun onFinish() {
                updateTimerDisplay()
                autoSubmit()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        tvTimer.text = String.format("剩余时间: %02d:%02d", minutes, seconds)
        updateProgress()
    }

    private fun updateProgress() {
        val answered = answers.size
        val total = questions.size
        tvProgress.text = "$answered / $total 已答"
    }

    private fun attemptSubmit() {
        val unanswered = questions.size - answers.size
        if (unanswered > 0) {
            AlertDialog.Builder(this)
                .setTitle("还有 $unanswered 道题未作答")
                .setMessage("确定要提交吗？")
                .setPositiveButton("确定提交") { _, _ -> submitAnswers() }
                .setNegativeButton("继续作答", null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("确认提交")
                .setMessage("确定要提交答卷吗？")
                .setPositiveButton("确定") { _, _ -> submitAnswers() }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun autoSubmit() {
        Toast.makeText(this, "考试时间已到，自动提交", Toast.LENGTH_SHORT).show()
        submitAnswers()
    }

    private fun submitAnswers() {
        timer?.cancel()
        btnSubmit.isEnabled = false
        btnSubmit.text = "提交中..."

        val answerItems = questions.map { q ->
            AnswerItem(
                questionId = q.questionId,
                studentAnswer = answers[q.questionId] ?: ""
            )
        }

        val request = SubmitExamRequest(
            studentId = TokenManager.getUserId(),
            useTime = usedSeconds,
            answers = answerItems
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.submitExam(examId, request)
                withContext(Dispatchers.Main) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "提交答卷"
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val result = response.body()?.data
                        if (result != null) {
                            val intent = Intent(this@ExamDetailActivity, ResultDetailActivity::class.java)
                            intent.putExtra("result_id", result.resultId)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@ExamDetailActivity, response.body()?.message ?: "提交失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "提交答卷"
                    Toast.makeText(this@ExamDetailActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}

class QuestionAdapter(
    private val questions: List<Question>,
    private val answers: MutableMap<Int, String>
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position], position)
    }

    override fun getItemCount() = questions.size

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex = itemView.findViewById<TextView>(R.id.tv_question_index)
        private val tvContent = itemView.findViewById<TextView>(R.id.tv_question_content)
        private val rgOptions = itemView.findViewById<RadioGroup>(R.id.rg_options)
        private val rbA = itemView.findViewById<RadioButton>(R.id.rb_a)
        private val rbB = itemView.findViewById<RadioButton>(R.id.rb_b)
        private val rbC = itemView.findViewById<RadioButton>(R.id.rb_c)
        private val rbD = itemView.findViewById<RadioButton>(R.id.rb_d)
        private val tilAnswer = itemView.findViewById<TextInputLayout>(R.id.til_answer)
        private val etAnswer = itemView.findViewById<TextInputEditText>(R.id.et_answer)
        private val tilEssay = itemView.findViewById<TextInputLayout>(R.id.til_essay)
        private val etEssay = itemView.findViewById<TextInputEditText>(R.id.et_essay)

        private var questionId = 0

        fun bind(question: Question, position: Int) {
            questionId = question.questionId
            tvIndex.text = "第 ${position + 1} 题 (${getTypeLabel(question.questionType)})"
            tvContent.text = question.questionContent

            rgOptions.visibility = View.GONE
            tilAnswer.visibility = View.GONE
            tilEssay.visibility = View.GONE

            when (question.questionType) {
                "single", "judge" -> {
                    rgOptions.visibility = View.VISIBLE
                    rbA.text = "A: ${question.optionA ?: ""}"
                    rbB.text = "B: ${question.optionB ?: ""}"
                    rbC.text = if (question.questionType == "single") "C: ${question.optionC ?: ""}" else "C: 对"
                    rbD.text = if (question.questionType == "single") "D: ${question.optionD ?: ""}" else "D: 错"

                    rbC.visibility = if (question.questionType == "single") View.VISIBLE else View.VISIBLE
                    rbD.visibility = if (question.questionType == "single") View.VISIBLE else View.VISIBLE

                    val saved = answers[questionId]
                    if (saved != null) {
                        when (saved) {
                            "A" -> rgOptions.check(R.id.rb_a)
                            "B" -> rgOptions.check(R.id.rb_b)
                            "C" -> rgOptions.check(R.id.rb_c)
                            "D" -> rgOptions.check(R.id.rb_d)
                        }
                    } else {
                        rgOptions.clearCheck()
                    }

                    rgOptions.setOnCheckedChangeListener { _, checkedId ->
                        val ans = when (checkedId) {
                            R.id.rb_a -> "A"
                            R.id.rb_b -> "B"
                            R.id.rb_c -> "C"
                            R.id.rb_d -> "D"
                            else -> null
                        }
                        if (ans != null) {
                            answers[questionId] = ans
                            (itemView.context as? ExamDetailActivity)?.updateProgress()
                        }
                    }
                }
                "fill_blank" -> {
                    tilAnswer.visibility = View.VISIBLE
                    etAnswer.setText(answers[questionId] ?: "")
                    etAnswer.addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            answers[questionId] = s?.toString() ?: ""
                            (itemView.context as? ExamDetailActivity)?.updateProgress()
                        }
                    })
                }
                "short_answer" -> {
                    tilEssay.visibility = View.VISIBLE
                    etEssay.setText(answers[questionId] ?: "")
                    etEssay.addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            answers[questionId] = s?.toString() ?: ""
                            (itemView.context as? ExamDetailActivity)?.updateProgress()
                        }
                    })
                }
            }
        }

        private fun getTypeLabel(type: String): String = when (type) {
            "single" -> "单选题"
            "judge" -> "判断题"
            "fill_blank" -> "填空题"
            "short_answer" -> "简答题"
            else -> type
        }
    }
}
