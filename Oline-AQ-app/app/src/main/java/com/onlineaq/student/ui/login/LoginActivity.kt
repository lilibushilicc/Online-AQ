package com.onlineaq.student.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.LoginRequest
import com.onlineaq.student.ui.main.MainActivity
import com.onlineaq.student.utils.ServerConfig
import com.onlineaq.student.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvServerHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (TokenManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvServerHint = findViewById(R.id.tv_server_hint)

        updateServerHint()

        btnLogin.setOnClickListener { attemptLogin() }
        findViewById<android.widget.ImageButton>(R.id.btn_server_settings).setOnClickListener { showServerSettings() }
        findViewById<android.widget.ImageView>(R.id.iv_logo).setOnClickListener { showAboutInfo() }

    }

    private fun updateServerHint() {
        val url = ServerConfig.getBaseUrl()
        tvServerHint.text = "服务器: $url"
    }

    private fun showServerSettings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_settings, null)
        val etUrl = dialogView.findViewById<TextInputEditText>(R.id.et_server_url)
        etUrl.setText(ServerConfig.getBaseUrl())
        etUrl.setSelection(etUrl.text?.length ?: 0)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save).setOnClickListener {
            val url = etUrl.text?.toString()?.trim() ?: return@setOnClickListener
            if (url.isNotBlank()) {
                ServerConfig.setBaseUrl(url)
                updateServerHint()
                Toast.makeText(this, "服务器地址已更新，请重新登录", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_reset).setOnClickListener {
            ServerConfig.resetToDefault()
            etUrl.setText(ServerConfig.getBaseUrl())
            etUrl.setSelection(etUrl.text?.length ?: 0)
            updateServerHint()
            Toast.makeText(this, "已恢复默认地址", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAboutInfo() {
        AlertDialog.Builder(this)
            .setTitle("智能在线答题系统")
            .setMessage("教师导入试题、自动解析组卷并发布考试。学生在线作答，系统即时评分并沉淀数据，形成完整的教学测验闭环。")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun attemptLogin() {
        val username = etUsername.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString()?.trim() ?: ""

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "登录中..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(username = username, password = password, role = "student")
                )
                withContext(Dispatchers.Main) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"

                    if (response.isSuccessful && response.body()?.code == 200) {
                        val data = response.body()?.data
                        if (data != null) {
                            TokenManager.saveLoginData(
                                token = data.token,
                                userId = data.userId,
                                username = data.username,
                                realName = data.realName,
                                role = data.role
                            )
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            response.body()?.message ?: "登录失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "登录"
                    Toast.makeText(this@LoginActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
