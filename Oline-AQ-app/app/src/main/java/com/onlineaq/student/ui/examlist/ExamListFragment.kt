package com.onlineaq.student.ui.examlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.Exam
import com.onlineaq.student.ui.examdetail.ExamDetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.drawable.GradientDrawable

class ExamListFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvExams: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_exam_list, container, false)
        swipeRefresh = root.findViewById(R.id.swipe_refresh)
        rvExams = root.findViewById(R.id.rv_exams)
        tvEmpty = root.findViewById(R.id.tv_empty)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvExams.layoutManager = GridLayoutManager(requireContext(), 2)
        rvExams.adapter = ExamGridAdapter { exam ->
            val intent = Intent(requireContext(), ExamDetailActivity::class.java)
            intent.putExtra("exam_id", exam.examId)
            startActivity(intent)
        }

        swipeRefresh.setOnRefreshListener { loadExams() }
        loadExams()
    }

    private fun loadExams() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getStudentExams()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val exams = response.body()?.data ?: emptyList()
                        (rvExams.adapter as? ExamGridAdapter)?.submitList(exams)
                        tvEmpty.visibility = if (exams.isEmpty()) View.VISIBLE else View.GONE
                        rvExams.visibility = if (exams.isEmpty()) View.GONE else View.VISIBLE
                    } else {
                        Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
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

class ExamGridAdapter(
    private val onItemClick: (Exam) -> Unit
) : RecyclerView.Adapter<ExamGridAdapter.ExamViewHolder>() {

    private var items: List<Exam> = emptyList()

    fun submitList(list: List<Exam>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exam, parent, false)
        return ExamViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ExamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.card_exam)
        private val tvName = itemView.findViewById<TextView>(R.id.tv_exam_name)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tv_status)
        private val tvDuration = itemView.findViewById<TextView>(R.id.tv_duration)
        private val tvTotalScore = itemView.findViewById<TextView>(R.id.tv_total_score)
        private val tvTimeRange = itemView.findViewById<TextView>(R.id.tv_time_range)

        fun bind(exam: Exam) {
            tvName.text = exam.examName
            tvDuration.text = "${exam.duration}分钟"
            tvTotalScore.text = "共${exam.totalScore?.toInt() ?: 0}分"

            val start = exam.startTime?.take(16)?.replace("T", " ") ?: "未设置"
            val end = exam.endTime?.take(16)?.replace("T", " ") ?: "未设置"
            tvTimeRange.text = "$start ~ $end"

            val statusColor = when (exam.status) {
                "published" -> {
                    tvStatus.text = "可参加"
                    itemView.context.getColor(R.color.status_ongoing)
                }
                "closed" -> {
                    tvStatus.text = "已结束"
                    itemView.context.getColor(R.color.status_closed)
                }
                "draft" -> {
                    tvStatus.text = "未发布"
                    itemView.context.getColor(R.color.status_draft)
                }
                else -> {
                    tvStatus.text = exam.status
                    itemView.context.getColor(R.color.ctp_overlay0)
                }
            }
            val badge = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f)
                setColor(statusColor)
            }
            tvStatus.background = badge

            card.setOnClickListener { onItemClick(exam) }
        }
    }
}
