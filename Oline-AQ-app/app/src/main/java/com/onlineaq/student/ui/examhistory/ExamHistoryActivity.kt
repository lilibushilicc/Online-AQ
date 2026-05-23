package com.onlineaq.student.ui.examhistory

import android.content.Intent
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
import com.google.android.material.appbar.MaterialToolbar
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.ExamResult
import com.onlineaq.student.ui.resultdetail.ResultDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExamHistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView

    private var examId: Int = 0
    private var examName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam_history)

        examId = intent.getIntExtra("exam_id", 0)
        examName = intent.getStringExtra("exam_name") ?: "试卷 #$examId"
        if (examId == 0) {
            finish()
            return
        }

        toolbar = findViewById(R.id.toolbar)
        rvHistory = findViewById(R.id.rv_history)
        tvEmpty = findViewById(R.id.tv_empty)

        toolbar.title = examName
        toolbar.setNavigationOnClickListener { finish() }

        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = ExamHistoryAdapter { result ->
            val intent = Intent(this, ResultDetailActivity::class.java)
            intent.putExtra("result_id", result.resultId)
            startActivity(intent)
        }

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getMyResults()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val data = response.body()?.data.orEmpty()
                            .filter { it.examId == examId }
                            .sortedByDescending { it.submitTime }
                        (rvHistory.adapter as? ExamHistoryAdapter)?.submitList(data)
                        tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                        rvHistory.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
                    } else {
                        Toast.makeText(this@ExamHistoryActivity, "加载历史记录失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExamHistoryActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class ExamHistoryAdapter(
    private val onItemClick: (ExamResult) -> Unit
) : RecyclerView.Adapter<ExamHistoryAdapter.HistoryViewHolder>() {

    private var items: List<ExamResult> = emptyList()

    fun submitList(list: List<ExamResult>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exam_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position], position, items.size)
    }

    override fun getItemCount() = items.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAttempt = itemView.findViewById<TextView>(R.id.tv_attempt_index)
        private val tvTime = itemView.findViewById<TextView>(R.id.tv_submit_time)
        private val tvScore = itemView.findViewById<TextView>(R.id.tv_score)
        private val tvStat = itemView.findViewById<TextView>(R.id.tv_stat)

        fun bind(result: ExamResult, position: Int, total: Int) {
            tvAttempt.text = "第 ${total - position} 次提交"
            tvTime.text = result.submitTime?.take(16)?.replace("T", " ") ?: ""
            tvScore.text = result.totalScore?.toInt()?.toString() ?: "0"
            val correct = result.correctCount ?: 0
            val wrong = result.wrongCount ?: 0
            tvStat.text = "正确: $correct  错误: $wrong"
            itemView.setOnClickListener { onItemClick(result) }
        }
    }
}
