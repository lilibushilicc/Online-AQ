package com.onlineaq.student.ui.results

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.ExamResult
import com.onlineaq.student.ui.resultdetail.ResultDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultsFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvResults: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_results, container, false)
        swipeRefresh = root.findViewById(R.id.swipe_refresh)
        rvResults = root.findViewById(R.id.rv_results)
        tvEmpty = root.findViewById(R.id.tv_empty)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = ResultAdapter { result ->
            val intent = Intent(requireContext(), ResultDetailActivity::class.java)
            intent.putExtra("result_id", result.resultId)
            startActivity(intent)
        }

        swipeRefresh.setOnRefreshListener { loadResults() }
        loadResults()
    }

    private fun loadResults() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getMyResults()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val data = response.body()?.data ?: emptyList()
                        (rvResults.adapter as? ResultAdapter)?.submitList(data)
                        tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                        rvResults.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
                    } else {
                        Toast.makeText(requireContext(), "加载成绩失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
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
            tvExamName.text = "考试 #${result.examId}"
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
