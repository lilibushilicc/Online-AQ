package com.onlineaq.student.ui.examlist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.Exam
import com.onlineaq.student.ui.login.LoginActivity
import com.onlineaq.student.ui.results.ResultsActivity
import com.onlineaq.student.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExamListActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvExams: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var examAdapter: ExamAdapter

    private var exams: List<Exam> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam_list)

        swipeRefresh = findViewById(R.id.swipe_refresh)
        rvExams = findViewById(R.id.rv_exams)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        examAdapter = ExamAdapter { exam ->
            val intent = Intent(this, ExamDetailActivity::class.java)
            intent.putExtra("exam_id", exam.examId)
            startActivity(intent)
        }

        rvExams.layoutManager = LinearLayoutManager(this)
        rvExams.adapter = examAdapter

        swipeRefresh.setOnRefreshListener { loadExams() }

        setupNavigation()

        loadExams()
    }

    private fun setupNavigation() {
        val headerView = navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.tv_header_name).text =
            TokenManager.getRealName() ?: "学生"
        headerView.findViewById<TextView>(R.id.tv_header_role).text = "学生端"

        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.nav_results -> {
                    startActivity(Intent(this, ResultsActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    TokenManager.clear()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadExams() {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getStudentExams()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body()?.code == 200) {
                        exams = response.body()?.data ?: emptyList()
                        examAdapter.submitList(exams)
                    } else {
                        Toast.makeText(this@ExamListActivity, "加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@ExamListActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class ExamAdapter(
    private val onItemClick: (Exam) -> Unit
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

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
        private val tvName = itemView.findViewById<TextView>(R.id.tv_exam_name)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tv_status)
        private val tvDescription = itemView.findViewById<TextView>(R.id.tv_description)
        private val tvDuration = itemView.findViewById<TextView>(R.id.tv_duration)
        private val tvTotalScore = itemView.findViewById<TextView>(R.id.tv_total_score)
        private val tvTimeRange = itemView.findViewById<TextView>(R.id.tv_time_range)

        fun bind(exam: Exam) {
            tvName.text = exam.examName
            tvDescription.text = exam.description ?: ""
            tvDuration.text = "时长: ${exam.duration}分钟"
            tvTotalScore.text = "总分: ${exam.totalScore ?: 0}"

            val start = exam.startTime?.take(16)?.replace("T", " ") ?: "未设置"
            val end = exam.endTime?.take(16)?.replace("T", " ") ?: "未设置"
            tvTimeRange.text = "$start ~ $end"

            tvStatus.text = when (exam.status) {
                "published" -> "可参加"
                "closed" -> "已结束"
                "draft" -> "未发布"
                else -> exam.status
            }

            itemView.setOnClickListener { onItemClick(exam) }
        }
    }
}
