package com.onlineaq.student.ui.results

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.Exam
import com.onlineaq.student.data.model.ExamResult
import com.onlineaq.student.ui.examhistory.ExamHistoryActivity
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
        rvResults.adapter = ExamResultGroupAdapter { group ->
            val intent = Intent(requireContext(), ExamHistoryActivity::class.java)
            intent.putExtra("exam_id", group.examId)
            intent.putExtra("exam_name", group.examName)
            startActivity(intent)
        }

        swipeRefresh.setOnRefreshListener { loadResults() }
        loadResults()
    }

    private fun loadResults() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resultsResponse = RetrofitClient.apiService.getMyResults()
                val examsResponse = RetrofitClient.apiService.getStudentExams()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    if (resultsResponse.isSuccessful && resultsResponse.body()?.code == 200) {
                        val results = resultsResponse.body()?.data ?: emptyList()
                        val exams = if (examsResponse.isSuccessful && examsResponse.body()?.code == 200) {
                            examsResponse.body()?.data ?: emptyList()
                        } else emptyList()

                        val examMap = exams.associateBy { it.examId }
                        val grouped = results.groupBy { it.examId }.map { (examId, examResults) ->
                            val exam = examMap[examId]
                            val latest = examResults.maxByOrNull { it.submitTime ?: "" }
                            val best = examResults.maxByOrNull { it.totalScore ?: 0.0 }
                            ExamResultGroup(
                                examId = examId,
                                examName = exam?.examName ?: "试卷 #$examId",
                                allowRetake = exam?.allowRetake ?: false,
                                submissionCount = examResults.size,
                                latestScore = latest?.totalScore?.toInt() ?: 0,
                                bestScore = best?.totalScore?.toInt() ?: 0,
                                latestTime = latest?.submitTime
                            )
                        }.sortedByDescending { it.latestTime }

                        (rvResults.adapter as? ExamResultGroupAdapter)?.submitList(grouped)
                        tvEmpty.visibility = if (grouped.isEmpty()) View.VISIBLE else View.GONE
                        rvResults.visibility = if (grouped.isEmpty()) View.GONE else View.VISIBLE
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

data class ExamResultGroup(
    val examId: Int,
    val examName: String,
    val allowRetake: Boolean,
    val submissionCount: Int,
    val latestScore: Int,
    val bestScore: Int,
    val latestTime: String?,
)

class ExamResultGroupAdapter(
    private val onItemClick: (ExamResultGroup) -> Unit
) : RecyclerView.Adapter<ExamResultGroupAdapter.GroupViewHolder>() {

    private var items: List<ExamResultGroup> = emptyList()

    fun submitList(list: List<ExamResultGroup>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exam_result_card, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExamName = itemView.findViewById<TextView>(R.id.tv_exam_name)
        private val tvSubmissionCount = itemView.findViewById<TextView>(R.id.tv_submission_count)
        private val tvLatestScore = itemView.findViewById<TextView>(R.id.tv_latest_score)
        private val tvBestScore = itemView.findViewById<TextView>(R.id.tv_best_score)
        private val tvLastSubmitTime = itemView.findViewById<TextView>(R.id.tv_last_submit_time)

        fun bind(group: ExamResultGroup) {
            tvExamName.text = group.examName
            tvSubmissionCount.text = "已考 ${group.submissionCount} 次"
            tvLatestScore.text = group.latestScore.toString()
            tvBestScore.text = group.bestScore.toString()
            tvLastSubmitTime.text = "最近提交: ${group.latestTime?.take(16)?.replace("T", " ") ?: "—"}"
            itemView.setOnClickListener { onItemClick(group) }
        }
    }
}
