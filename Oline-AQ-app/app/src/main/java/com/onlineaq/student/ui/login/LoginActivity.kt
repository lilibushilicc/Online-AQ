package com.onlineaq.student.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.LoginRequest
import com.onlineaq.student.ui.examlist.ExamListActivity
import com.onlineaq.student.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (TokenManager.isLoggedIn()) {
            startActivity(Intent(this, ExamListActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)

        btnLogin.setOnClickListener { attemptLogin() }
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

        CoroutineScope(Dispatchers.IO).launch {
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
                            startActivity(Intent(this@LoginActivity, ExamListActivity::class.java))
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
