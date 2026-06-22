package com.onlineaq.student.ui.examdetail

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.*
import com.onlineaq.student.ui.resultdetail.ResultDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExamDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTimer: TextView
    private lateinit var tvProgress: TextView
    private lateinit var rvQuestions: RecyclerView
    private lateinit var scrollQuestionNav: HorizontalScrollView
    private lateinit var layoutQuestionNav: LinearLayout
    private lateinit var btnNextUnanswered: MaterialButton
    private lateinit var btnSubmit: MaterialButton
    private lateinit var questionLayoutManager: LinearLayoutManager

    private var examId: Int = 0
    private var questions: List<Question> = emptyList()
    private val answers = mutableMapOf<Int, String>()
    private var timer: CountDownTimer? = null
    private var remainingSeconds: Int = 0
    private var usedSeconds: Int = 0
    private var examDurationMinutes: Int = 0
    private var attemptId: String? = null
    private val navViews = mutableListOf<TextView>()
    private var activeQuestionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
        scrollQuestionNav = findViewById(R.id.scroll_question_nav)
        layoutQuestionNav = findViewById(R.id.layout_question_nav)
        btnNextUnanswered = findViewById(R.id.btn_next_unanswered)
        btnSubmit = findViewById(R.id.btn_submit)

        toolbar.setNavigationOnClickListener { finish() }
        btnNextUnanswered.setOnClickListener { jumpToNextUnanswered() }
        btnSubmit.setOnClickListener { attemptSubmit() }

        questionLayoutManager = LinearLayoutManager(this)
        rvQuestions.layoutManager = questionLayoutManager
        rvQuestions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstCompletely = questionLayoutManager.findFirstCompletelyVisibleItemPosition()
                val firstVisible = questionLayoutManager.findFirstVisibleItemPosition()
                val target = when {
                    firstCompletely != RecyclerView.NO_POSITION -> firstCompletely
                    firstVisible != RecyclerView.NO_POSITION -> firstVisible
                    else -> return
                }
                if (target != activeQuestionIndex) {
                    highlightActiveQuestion(target)
                }
            }
        })

        loadExamDetail()
    }

    private fun loadExamDetail() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getExamDetail(examId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val detail = response.body()?.data
                        if (detail != null) {
                            questions = detail.questions
                            examDurationMinutes = detail.exam.duration
                            attemptId = detail.attemptId
                            toolbar.title = detail.exam.examName
                            setupQuestions()
                            startTimer()
                        } else {
                            Toast.makeText(this@ExamDetailActivity, "考试数据为空", Toast.LENGTH_SHORT).show()
                            finish()
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
        val adapter = QuestionAdapter(questions, answers) { updateProgress() }
        rvQuestions.adapter = adapter
        renderQuestionNav()
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
        tvTimer.text = String.format("剩余 %02d:%02d", minutes, seconds)
        tvTimer.setTextColor(
            if (remainingSeconds < 300) getColor(R.color.red_500)
            else getColor(R.color.teal_500)
        )
    }

    fun updateProgress() {
        val answered = answers.size
        val total = questions.size
        tvProgress.text = "已答 $answered / $total"
        updateQuestionNavState()
    }

    private fun renderQuestionNav() {
        navViews.clear()
        layoutQuestionNav.removeAllViews()
        questions.forEachIndexed { index, _ ->
            val chip = TextView(this).apply {
                text = "${index + 1}"
                gravity = Gravity.CENTER
                textSize = 13f
                setPadding(24, 16, 24, 16)
                setTextColor(getColor(R.color.m3_on_surface))
                background = getDrawable(R.drawable.bg_question_nav_idle)
                    setOnClickListener {
                        rvQuestions.smoothScrollToPosition(index)
                        highlightActiveQuestion(index)
                    }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginEnd = 10
            }
            chip.layoutParams = params
            navViews.add(chip)
            layoutQuestionNav.addView(chip)
        }
        highlightActiveQuestion(0)
    }

    private fun updateQuestionNavState(activeIndex: Int? = null) {
        navViews.forEachIndexed { index, textView ->
            val answered = answers[questions[index].questionId]?.isNotBlank() == true
            when {
                activeIndex != null && index == activeIndex -> {
                    textView.background = getDrawable(R.drawable.bg_question_nav_active)
                    textView.setTextColor(getColor(R.color.white))
                }
                answered -> {
                    textView.background = getDrawable(R.drawable.bg_question_nav_done)
                    textView.setTextColor(getColor(R.color.gradient_center))
                }
                else -> {
                    textView.background = getDrawable(R.drawable.bg_question_nav_idle)
                    textView.setTextColor(getColor(R.color.m3_on_surface))
                }
            }
        }
    }

    private fun highlightActiveQuestion(index: Int) {
        activeQuestionIndex = index
        updateQuestionNavState(index)
        val targetView = navViews.getOrNull(index) ?: return
        scrollQuestionNav.post {
            val scrollX = targetView.left - (scrollQuestionNav.width - targetView.width) / 2
            scrollQuestionNav.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }
    }

    private fun jumpToNextUnanswered() {
        if (questions.isEmpty()) return
        val nextIndex = ((activeQuestionIndex + 1) until questions.size).firstOrNull { index ->
            answers[questions[index].questionId].isNullOrBlank()
        } ?: (0 until activeQuestionIndex).firstOrNull { index ->
            answers[questions[index].questionId].isNullOrBlank()
        }

        if (nextIndex == null) {
            Toast.makeText(this, "当前所有题目都已作答", Toast.LENGTH_SHORT).show()
            return
        }

        rvQuestions.smoothScrollToPosition(nextIndex)
        highlightActiveQuestion(nextIndex)
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
            AnswerItem(questionId = q.questionId, studentAnswer = answers[q.questionId] ?: "")
        }

        val request = SubmitExamRequest(
            attemptId = attemptId,
            useTime = usedSeconds,
            answers = answerItems
        )

        lifecycleScope.launch(Dispatchers.IO) {
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
    private val answers: MutableMap<Int, String>,
    private val onAnswerChanged: () -> Unit
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
        private val tvTypeBadge = itemView.findViewById<TextView>(R.id.tv_question_type_badge)
        private val tvContent = itemView.findViewById<TextView>(R.id.tv_question_content)
        private val rgOptions = itemView.findViewById<RadioGroup>(R.id.rg_options)
        private val rbA = itemView.findViewById<android.widget.RadioButton>(R.id.rb_a)
        private val rbB = itemView.findViewById<android.widget.RadioButton>(R.id.rb_b)
        private val rbC = itemView.findViewById<android.widget.RadioButton>(R.id.rb_c)
        private val rbD = itemView.findViewById<android.widget.RadioButton>(R.id.rb_d)
        private val cardA = itemView.findViewById<MaterialCardView>(R.id.card_option_a)
        private val cardB = itemView.findViewById<MaterialCardView>(R.id.card_option_b)
        private val cardC = itemView.findViewById<MaterialCardView>(R.id.card_option_c)
        private val cardD = itemView.findViewById<MaterialCardView>(R.id.card_option_d)
        private val tilAnswer = itemView.findViewById<TextInputLayout>(R.id.til_answer)
        private val etAnswer = itemView.findViewById<TextInputEditText>(R.id.et_answer)
        private val tilEssay = itemView.findViewById<TextInputLayout>(R.id.til_essay)
        private val etEssay = itemView.findViewById<TextInputEditText>(R.id.et_essay)

        private var questionId = 0

        fun bind(question: Question, position: Int) {
            questionId = question.questionId
            tvIndex.text = "第 ${position + 1} 题"
            tvTypeBadge.text = getTypeLabel(question.questionType)
            tvContent.text = question.questionContent

            rgOptions.visibility = View.GONE
            tilAnswer.visibility = View.GONE
            tilEssay.visibility = View.GONE
            cardA.visibility = View.GONE
            cardB.visibility = View.GONE
            cardC.visibility = View.GONE
            cardD.visibility = View.GONE

            when (question.questionType) {
                "single", "judge" -> {
                    rgOptions.visibility = View.VISIBLE
                    cardA.visibility = View.VISIBLE
                    cardB.visibility = View.VISIBLE
                    cardC.visibility = View.VISIBLE
                    cardD.visibility = View.VISIBLE

                    rbA.text = "A. ${question.optionA ?: ""}"
                    rbB.text = "B. ${question.optionB ?: ""}"
                    rbC.text = if (question.questionType == "single") "C. ${question.optionC ?: ""}" else "C. 对"
                    rbD.text = if (question.questionType == "single") "D. ${question.optionD ?: ""}" else "D. 错"

                    val saved = answers[questionId]
                    clearRadioGroupListener()
                    if (saved != null) {
                        when (saved) {
                            "A" -> { rgOptions.check(R.id.rb_a); updateCardSelection("A") }
                            "B" -> { rgOptions.check(R.id.rb_b); updateCardSelection("B") }
                            "C" -> { rgOptions.check(R.id.rb_c); updateCardSelection("C") }
                            "D" -> { rgOptions.check(R.id.rb_d); updateCardSelection("D") }
                        }
                    } else {
                        rgOptions.clearCheck()
                        updateCardSelection(null)
                    }

                    setupCardClickListeners()

                    rgOptions.setOnCheckedChangeListener { _, checkedId ->
                        val ans = when (checkedId) {
                            R.id.rb_a -> "A"
                            R.id.rb_b -> "B"
                            R.id.rb_c -> "C"
                            R.id.rb_d -> "D"
                            else -> null
                        }
                        updateCardSelection(ans)
                        if (ans != null) {
                            answers[questionId] = ans
                        } else {
                            answers.remove(questionId)
                        }
                        onAnswerChanged()
                    }
                }
                "fill_blank" -> {
                    tilAnswer.visibility = View.VISIBLE
                    etAnswer.setText(answers[questionId] ?: "")
                    etAnswer.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            answers[questionId] = s?.toString() ?: ""
                            onAnswerChanged()
                        }
                    })
                }
                "short_answer" -> {
                    tilEssay.visibility = View.VISIBLE
                    etEssay.setText(answers[questionId] ?: "")
                    etEssay.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            answers[questionId] = s?.toString() ?: ""
                            onAnswerChanged()
                        }
                    })
                }
            }
        }

        private fun setupCardClickListeners() {
            cardA.setOnClickListener { rgOptions.check(rbA.id) }
            cardB.setOnClickListener { rgOptions.check(rbB.id) }
            cardC.setOnClickListener { rgOptions.check(rbC.id) }
            cardD.setOnClickListener { rgOptions.check(rbD.id) }
        }

        private fun clearRadioGroupListener() {
            rgOptions.setOnCheckedChangeListener(null)
        }

        private fun updateCardSelection(selected: String?) {
            val ctx = itemView.context
            val primaryColor = ctx.getColor(R.color.m3_primary)
            val outlineColor = ctx.getColor(R.color.m3_outline)

            listOf(
                cardA to "A", cardB to "B", cardC to "C", cardD to "D"
            ).forEach { (card, key) ->
                if (card.visibility != View.GONE) {
                    if (key == selected) {
                        card.setStrokeColor(primaryColor)
                        card.strokeWidth = 2
                    } else {
                        card.strokeWidth = 1
                        card.setStrokeColor(outlineColor)
                    }
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
