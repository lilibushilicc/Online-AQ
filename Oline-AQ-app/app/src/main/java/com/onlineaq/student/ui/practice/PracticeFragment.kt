package com.onlineaq.student.ui.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PracticeFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvQuestions: RecyclerView
    private lateinit var tvStats: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var dropdownCategory: MaterialAutoCompleteTextView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnRetry: MaterialButton

    private var allQuestions: List<Question> = emptyList()
    private var currentQuestions: List<Question> = emptyList()
    private val answers = mutableMapOf<Int, String>()
    private var submitted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = inflater.inflate(R.layout.fragment_practice, container, false)
        swipeRefresh = root.findViewById(R.id.swipe_refresh)
        rvQuestions = root.findViewById(R.id.rv_practice_questions)
        tvStats = root.findViewById(R.id.tv_practice_stats)
        tvEmpty = root.findViewById(R.id.tv_empty)
        dropdownCategory = root.findViewById(R.id.dropdown_category)
        btnSubmit = root.findViewById(R.id.btn_submit_practice)
        btnRetry = root.findViewById(R.id.btn_retry_practice)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvQuestions.layoutManager = LinearLayoutManager(requireContext())
        rvQuestions.adapter = PracticeAdapter(currentQuestions, answers, { updateStats() }, { submitted })

        dropdownCategory.setOnItemClickListener { _, _, _, _ ->
            applyFilter(dropdownCategory.text?.toString())
        }
        swipeRefresh.setOnRefreshListener { loadData() }
        btnSubmit.setOnClickListener { submitPractice() }
        btnRetry.setOnClickListener { resetPractice() }
        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val categoryResponse = RetrofitClient.apiService.getQuestionCategories()
                val questionResponse = RetrofitClient.apiService.getQuestions()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    if (categoryResponse.isSuccessful && questionResponse.isSuccessful && questionResponse.body()?.code == 200) {
                        val categories = listOf("全部分类") + (categoryResponse.body()?.data ?: emptyList())
                        dropdownCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))
                        if (dropdownCategory.text.isNullOrBlank()) dropdownCategory.setText("全部分类", false)
                        allQuestions = questionResponse.body()?.data?.list ?: emptyList()
                        applyFilter(dropdownCategory.text?.toString())
                    } else {
                        Toast.makeText(requireContext(), "加载练习题失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyFilter(category: String?) {
        val target = category?.takeIf { it.isNotBlank() && it != "全部分类" }
        currentQuestions = if (target == null) allQuestions else allQuestions.filter { it.category == target }
        resetPractice(clearFilter = false)
        renderQuestions()
    }

    private fun resetPractice(clearFilter: Boolean = false) {
        answers.clear()
        submitted = false
        btnSubmit.visibility = View.VISIBLE
        btnRetry.visibility = View.GONE
        if (clearFilter) dropdownCategory.setText("全部分类", false)
        renderQuestions()
    }

    private fun renderQuestions() {
        (rvQuestions.adapter as? PracticeAdapter)?.submit(currentQuestions)
        rvQuestions.visibility = if (currentQuestions.isEmpty()) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (currentQuestions.isEmpty()) View.VISIBLE else View.GONE
        updateStats()
    }

    private fun submitPractice() {
        if (currentQuestions.isEmpty()) return
        if (answers.size < currentQuestions.size) {
            Toast.makeText(requireContext(), "请完成所有题目后再提交", Toast.LENGTH_SHORT).show()
            return
        }
        submitted = true
        btnSubmit.visibility = View.GONE
        btnRetry.visibility = View.VISIBLE
        (rvQuestions.adapter as? PracticeAdapter)?.notifyDataSetChanged()
        updateStats()
    }

    private fun updateStats() {
        val total = currentQuestions.size
        val answered = answers.count { it.value.isNotBlank() }
        val correct = if (submitted) currentQuestions.count { answers[it.questionId] == it.correctAnswer } else 0
        tvStats.text = if (submitted) {
            "共 $total 题，已答 $answered 题，答对 $correct 题"
        } else {
            "共 $total 题，已答 $answered 题"
        }
    }
}

private class PracticeAdapter(
    private var items: List<Question>,
    private val answers: MutableMap<Int, String>,
    private val onAnswerChanged: () -> Unit,
    private val isSubmitted: () -> Boolean,
) : RecyclerView.Adapter<PracticeAdapter.PracticeViewHolder>() {

    fun submit(list: List<Question>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PracticeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_practice_question, parent, false)
        return PracticeViewHolder(view)
    }

    override fun onBindViewHolder(holder: PracticeViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class PracticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIndex = itemView.findViewById<TextView>(R.id.tv_question_index)
        private val tvTitle = itemView.findViewById<TextView>(R.id.tv_question_title)
        private val tvHint = itemView.findViewById<TextView>(R.id.tv_answer_hint)
        private val rgOptions = itemView.findViewById<RadioGroup>(R.id.rg_options)
        private val rbA = itemView.findViewById<android.widget.RadioButton>(R.id.rb_a)
        private val rbB = itemView.findViewById<android.widget.RadioButton>(R.id.rb_b)
        private val rbC = itemView.findViewById<android.widget.RadioButton>(R.id.rb_c)
        private val rbD = itemView.findViewById<android.widget.RadioButton>(R.id.rb_d)
        private val tilAnswer = itemView.findViewById<TextInputLayout>(R.id.til_answer)
        private val etAnswer = itemView.findViewById<TextInputEditText>(R.id.et_answer)
        private val tilEssay = itemView.findViewById<TextInputLayout>(R.id.til_essay)
        private val etEssay = itemView.findViewById<TextInputEditText>(R.id.et_essay)
        private var answerWatcher: SimpleAfterTextChanged? = null
        private var essayWatcher: SimpleAfterTextChanged? = null

        fun bind(question: Question, position: Int) {
            tvIndex.text = "第 ${position + 1} 题"
            tvTitle.text = question.questionContent
            tvHint.visibility = View.GONE

            rgOptions.visibility = View.GONE
            tilAnswer.visibility = View.GONE
            tilEssay.visibility = View.GONE

            when (question.questionType) {
                "single", "judge" -> {
                    rgOptions.visibility = View.VISIBLE
                    rbA.text = "A. ${question.optionA.orEmpty()}"
                    rbB.text = "B. ${question.optionB.orEmpty()}"
                    rbC.text = if (question.questionType == "judge") "C. 对" else "C. ${question.optionC.orEmpty()}"
                    rbD.text = if (question.questionType == "judge") "D. 错" else "D. ${question.optionD.orEmpty()}"
                    rgOptions.setOnCheckedChangeListener(null)
                    when (answers[question.questionId]) {
                        "A" -> rgOptions.check(R.id.rb_a)
                        "B" -> rgOptions.check(R.id.rb_b)
                        "C" -> rgOptions.check(R.id.rb_c)
                        "D" -> rgOptions.check(R.id.rb_d)
                        else -> rgOptions.clearCheck()
                    }
                    rgOptions.setOnCheckedChangeListener { _, checkedId ->
                        if (isSubmitted()) return@setOnCheckedChangeListener
                        answers[question.questionId] = when (checkedId) {
                            R.id.rb_a -> "A"
                            R.id.rb_b -> "B"
                            R.id.rb_c -> "C"
                            R.id.rb_d -> "D"
                            else -> ""
                        }
                        onAnswerChanged()
                    }
                }

                "fill_blank" -> {
                    tilAnswer.visibility = View.VISIBLE
                    answerWatcher?.let { etAnswer.removeTextChangedListener(it) }
                    etAnswer.setText(answers[question.questionId].orEmpty())
                    etAnswer.isEnabled = !isSubmitted()
                    answerWatcher = SimpleAfterTextChanged { text ->
                        if (!isSubmitted()) {
                            answers[question.questionId] = text
                            onAnswerChanged()
                        }
                    }
                    etAnswer.addTextChangedListener(answerWatcher)
                }

                else -> {
                    tilEssay.visibility = View.VISIBLE
                    essayWatcher?.let { etEssay.removeTextChangedListener(it) }
                    etEssay.setText(answers[question.questionId].orEmpty())
                    etEssay.isEnabled = !isSubmitted()
                    essayWatcher = SimpleAfterTextChanged { text ->
                        if (!isSubmitted()) {
                            answers[question.questionId] = text
                            onAnswerChanged()
                        }
                    }
                    etEssay.addTextChangedListener(essayWatcher)
                }
            }

            if (isSubmitted()) {
                val correct = answers[question.questionId] == question.correctAnswer
                tvHint.visibility = View.VISIBLE
                tvHint.text = if (correct) "回答正确" else "参考答案：${question.correctAnswer.orEmpty()}"
                tvHint.setTextColor(itemView.context.getColor(if (correct) R.color.green_500 else R.color.red_500))
                tvHint.setBackgroundResource(if (correct) R.drawable.bg_hint_success else R.drawable.bg_hint_error)
            } else {
                tvHint.visibility = View.VISIBLE
                val current = answers[question.questionId].orEmpty()
                tvHint.text = if (current.isBlank()) "未作答" else "已填写，提交后查看答案"
                tvHint.setTextColor(itemView.context.getColor(if (current.isBlank()) R.color.gray_500 else R.color.blue_500))
                tvHint.setBackgroundResource(R.drawable.bg_hint_info)
            }
        }
    }
}
