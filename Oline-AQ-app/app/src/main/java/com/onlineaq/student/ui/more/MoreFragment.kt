package com.onlineaq.student.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.onlineaq.student.R
import com.onlineaq.student.ui.login.LoginActivity
import com.onlineaq.student.utils.ServerConfig
import com.onlineaq.student.utils.ThemeConfig
import com.onlineaq.student.utils.TokenManager

class MoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_more, container, false)

        val tvName = root.findViewById<TextView>(R.id.tv_display_name)
        val tvServerUrl = root.findViewById<TextView>(R.id.tv_server_url)
        val toggleTheme = root.findViewById<MaterialButtonToggleGroup>(R.id.toggle_theme)
        val switchAmoled = root.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_amoled)

        tvName.text = TokenManager.getRealName() ?: "学生"

        tvServerUrl.text = "服务器: ${ServerConfig.getBaseUrl()}"

        val currentTheme = ThemeConfig.getThemeMode()
        when (currentTheme) {
            ThemeConfig.MODE_LIGHT -> toggleTheme.check(R.id.btn_theme_light)
            ThemeConfig.MODE_DARK -> toggleTheme.check(R.id.btn_theme_dark)
            else -> toggleTheme.check(R.id.btn_theme_system)
        }

        switchAmoled.isChecked = ThemeConfig.isAmoled()

        toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btn_theme_light -> ThemeConfig.MODE_LIGHT
                R.id.btn_theme_dark -> ThemeConfig.MODE_DARK
                else -> ThemeConfig.MODE_SYSTEM
            }
            ThemeConfig.setThemeMode(mode)
            requireActivity().recreate()
        }

        switchAmoled.setOnCheckedChangeListener { _, isChecked ->
            ThemeConfig.setAmoled(isChecked)
            requireActivity().recreate()
        }

        root.findViewById<MaterialButton>(R.id.btn_server_settings).setOnClickListener {
            showServerSettings(tvServerUrl)
        }

        root.findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            TokenManager.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }

        return root
    }

    private fun showServerSettings(tvServerUrl: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_settings, null)
        val etUrl = dialogView.findViewById<TextInputEditText>(R.id.et_server_url)
        etUrl.setText(ServerConfig.getBaseUrl())
        etUrl.setSelection(etUrl.text?.length ?: 0)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save).setOnClickListener {
            val url = etUrl.text?.toString()?.trim() ?: return@setOnClickListener
            if (url.isNotBlank()) {
                ServerConfig.setBaseUrl(url)
                tvServerUrl.text = "服务器: ${ServerConfig.getBaseUrl()}"
                Toast.makeText(requireContext(), "服务器地址已更新，请重新登录", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_reset).setOnClickListener {
            ServerConfig.resetToDefault()
            etUrl.setText(ServerConfig.getBaseUrl())
            etUrl.setSelection(etUrl.text?.length ?: 0)
            tvServerUrl.text = "服务器: ${ServerConfig.getBaseUrl()}"
            Toast.makeText(requireContext(), "已恢复默认地址", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
