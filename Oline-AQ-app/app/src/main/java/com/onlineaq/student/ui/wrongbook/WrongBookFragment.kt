package com.onlineaq.student.ui.wrongbook

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.NotebookRequest
import com.onlineaq.student.data.model.WrongNotebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WrongBookFragment : Fragment() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var containerBooks: LinearLayout
    private lateinit var tvSummary: TextView
    private var notebooks: List<WrongNotebook> = emptyList()
    private var totalWrongCount: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = inflater.inflate(R.layout.fragment_wrong_book, container, false)
        swipeRefresh = root.findViewById(R.id.swipe_refresh)
        containerBooks = root.findViewById(R.id.layout_notebooks)
        tvSummary = root.findViewById(R.id.tv_wrong_summary)
        root.findViewById<MaterialButton>(R.id.btn_create_notebook).setOnClickListener { showNotebookDialog() }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setOnRefreshListener { loadData() }
        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val notebookResponse = RetrofitClient.apiService.getWrongNotebooks()
                val wrongResponse = RetrofitClient.apiService.getWrongQuestions()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    if (notebookResponse.isSuccessful && wrongResponse.isSuccessful) {
                        notebooks = notebookResponse.body()?.data.orEmpty()
                        totalWrongCount = wrongResponse.body()?.data.orEmpty().sumOf { it.questions.size }
                        renderBooks()
                    } else {
                        Toast.makeText(requireContext(), "加载错题本失败", Toast.LENGTH_SHORT).show()
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

    private fun renderBooks() {
        containerBooks.removeAllViews()
        tvSummary.text = "共 ${notebooks.size} 个错题本，累计 $totalWrongCount 道错题"

        val allCard = layoutInflater.inflate(R.layout.item_wrong_book, containerBooks, false)
        allCard.findViewById<TextView>(R.id.tv_notebook_name).text = "全部错题"
        allCard.findViewById<TextView>(R.id.tv_notebook_desc).text = "按考试分组查看全部错题"
        allCard.findViewById<TextView>(R.id.tv_notebook_count).text = "$totalWrongCount 道错题"
        allCard.findViewById<MaterialButton>(R.id.btn_edit_notebook).visibility = View.GONE
        allCard.findViewById<MaterialButton>(R.id.btn_delete_notebook).visibility = View.GONE
        allCard.setOnClickListener { openDetail("all", "全部错题") }
        containerBooks.addView(allCard)

        notebooks.forEach { notebook ->
            val item = layoutInflater.inflate(R.layout.item_wrong_book, containerBooks, false)
            item.findViewById<TextView>(R.id.tv_notebook_name).text = notebook.notebookName
            item.findViewById<TextView>(R.id.tv_notebook_desc).text = notebook.description ?: "暂无描述"
            item.findViewById<TextView>(R.id.tv_notebook_count).text = "${notebook.itemCount} 道错题"
            item.findViewById<MaterialButton>(R.id.btn_edit_notebook).setOnClickListener {
                showNotebookDialog(notebook)
            }
            item.findViewById<MaterialButton>(R.id.btn_delete_notebook).setOnClickListener {
                deleteNotebook(notebook)
            }
            item.setOnClickListener { openDetail(notebook.notebookId.toString(), notebook.notebookName) }
            containerBooks.addView(item)
        }
    }

    private fun openDetail(notebookId: String, title: String) {
        startActivity(Intent(requireContext(), WrongBookDetailActivity::class.java).apply {
            putExtra("notebook_id", notebookId)
            putExtra("notebook_title", title)
        })
    }

    private fun showNotebookDialog(notebook: WrongNotebook? = null) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_notebook, null)
        val etName = view.findViewById<TextInputEditText>(R.id.et_notebook_name)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_notebook_desc)
        etName.setText(notebook?.notebookName.orEmpty())
        etDesc.setText(notebook?.description.orEmpty())

        AlertDialog.Builder(requireContext())
            .setTitle(if (notebook == null) "新建错题本" else "编辑错题本")
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton(if (notebook == null) "创建" else "保存") { _, _ ->
                val name = etName.text?.toString()?.trim().orEmpty()
                val desc = etDesc.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveNotebook(notebook?.notebookId, name, desc)
            }
            .show()
    }

    private fun saveNotebook(notebookId: Int?, name: String, desc: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = NotebookRequest(name, desc)
                val response = if (notebookId == null) {
                    RetrofitClient.apiService.createWrongNotebook(request)
                } else {
                    RetrofitClient.apiService.updateWrongNotebook(notebookId, request)
                }
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        Toast.makeText(requireContext(), if (notebookId == null) "错题本已创建" else "错题本已更新", Toast.LENGTH_SHORT).show()
                        loadData()
                    } else {
                        Toast.makeText(requireContext(), response.body()?.message ?: "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteNotebook(notebook: WrongNotebook) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除错题本")
            .setMessage("确认删除 ${notebook.notebookName} 吗？错题本里的题目不会被删除。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.apiService.deleteWrongNotebook(notebook.notebookId)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body()?.code == 200) {
                                loadData()
                            } else {
                                Toast.makeText(requireContext(), response.body()?.message ?: "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }
}
