package com.onlineaq.student.ui.results

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.ExamResult
import com.onlineaq.student.ui.resultdetail.ResultDetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultsActivity : AppCompatActivity() {

    private lateinit var rvResults: RecyclerView
    private lateinit var adapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvResults = findViewById(R.id.rv_results)
        rvResults.layoutManager = LinearLayoutManager(this)

        adapter = ResultAdapter { result ->
            val intent = Intent(this, ResultDetailActivity::class.java)
            intent.putExtra("result_id", result.resultId)
            startActivity(intent)
        }
        rvResults.adapter = adapter

        loadResults()
    }

    private fun loadResults() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getMyResults()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val data = response.body()?.data ?: emptyList()
                        adapter.submitList(data)
                    }
                }
            } catch (_: Exception) { }
        }
    }
}

class ResultAdapter(
    private val onItemClick: (ExamResult) -> Unit
) : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    private var items: List<ExamResult> = emptyList()

    fun submitList(list: List<ExamResult>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExamName = itemView.findViewById<TextView>(R.id.tv_exam_name)
        private val tvSubmitTime = itemView.findViewById<TextView>(R.id.tv_submit_time)
        private val tvStat = itemView.findViewById<TextView>(R.id.tv_stat)
        private val tvScore = itemView.findViewById<TextView>(R.id.tv_score)
        private val progressScore = itemView.findViewById<ProgressBar>(R.id.progress_score)

        fun bind(result: ExamResult) {
            tvExamName.text = "考试成绩 #${result.resultId}"
            tvSubmitTime.text = result.submitTime?.take(16)?.replace("T", " ") ?: ""
            tvScore.text = result.totalScore?.toInt()?.toString() ?: "0"

            val correct = result.correctCount ?: 0
            val wrong = result.wrongCount ?: 0
            tvStat.text = "正确: $correct  错误: $wrong"
            progressScore.progress = result.totalScore?.toInt() ?: 0

            itemView.setOnClickListener { onItemClick(result) }
        }
    }
}
