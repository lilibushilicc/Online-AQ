package com.onlineaq.student.ui.profile

import android.content.Intent
import android.net.Uri
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.onlineaq.student.BuildConfig
import com.onlineaq.student.R
import com.onlineaq.student.data.api.RetrofitClient
import com.onlineaq.student.data.model.AnnouncementItem
import com.onlineaq.student.data.model.ProfileUpdateRequest
import com.onlineaq.student.data.model.UserProfile
import com.onlineaq.student.ui.login.LoginActivity
import com.onlineaq.student.utils.ServerConfig
import com.onlineaq.student.utils.ThemeConfig
import com.onlineaq.student.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var tvDisplayName: TextView
    private lateinit var tvProfileAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileUsername: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileCreateTime: TextView
    private lateinit var tvAnnouncementCount: TextView
    private lateinit var announcementContainer: LinearLayout
    private lateinit var tvServerUrl: TextView
    private lateinit var toggleTheme: MaterialButtonToggleGroup
    private lateinit var switchAmoled: androidx.appcompat.widget.SwitchCompat

    private var currentProfile: UserProfile? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root = inflater.inflate(R.layout.fragment_profile, container, false)
        tvDisplayName = root.findViewById(R.id.tv_display_name)
        tvProfileAvatar = root.findViewById(R.id.tv_profile_avatar)
        tvProfileName = root.findViewById(R.id.tv_profile_name)
        tvProfileUsername = root.findViewById(R.id.tv_profile_username)
        tvProfileEmail = root.findViewById(R.id.tv_profile_email)
        tvProfileCreateTime = root.findViewById(R.id.tv_profile_create_time)
        tvAnnouncementCount = root.findViewById(R.id.tv_announcement_count)
        announcementContainer = root.findViewById(R.id.layout_announcements)
        tvServerUrl = root.findViewById(R.id.tv_server_url)
        toggleTheme = root.findViewById(R.id.toggle_theme)
        switchAmoled = root.findViewById(R.id.switch_amoled)

        initThemeSection()
        initStaticActions(root)
        loadProfile()
        loadAnnouncements()
        return root
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
        loadAnnouncements()
    }

    private fun initThemeSection() {
        tvServerUrl.text = "服务器: ${ServerConfig.getBaseUrl()}"
        when (ThemeConfig.getThemeMode()) {
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
    }

    private fun initStaticActions(root: View) {
        root.findViewById<MaterialButton>(R.id.btn_edit_profile).setOnClickListener { showEditProfileDialog() }
        root.findViewById<MaterialButton>(R.id.btn_change_password).setOnClickListener { showPasswordDialog() }
        root.findViewById<MaterialButton>(R.id.btn_mark_all_read).setOnClickListener { markAllAnnouncementsRead() }
        root.findViewById<MaterialButton>(R.id.btn_check_version).setOnClickListener { checkVersionUpdate() }
        root.findViewById<MaterialButton>(R.id.btn_server_settings).setOnClickListener { showServerSettings() }
        root.findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            TokenManager.clear()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }
    }

    private fun loadProfile() {
        tvDisplayName.text = TokenManager.getRealName() ?: "学生"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getMyProfile()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        currentProfile = response.body()?.data
                        bindProfile(currentProfile)
                    } else {
                        Toast.makeText(requireContext(), response.body()?.message ?: "加载个人资料失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindProfile(profile: UserProfile?) {
        tvProfileName.text = profile?.realName ?: "学生"
        tvProfileUsername.text = profile?.username ?: "-"
        tvProfileEmail.text = if (profile?.email.isNullOrBlank()) "未设置" else profile?.email
        tvProfileCreateTime.text = profile?.createTime?.replace("T", " ")?.take(19) ?: "-"
        tvDisplayName.text = profile?.realName ?: TokenManager.getRealName() ?: "学生"
        tvProfileAvatar.text = (profile?.realName ?: TokenManager.getRealName() ?: "学").take(1)
    }

    private fun loadAnnouncements() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getUnreadAnnouncements()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val info = response.body()?.data
                        val items = info?.announcements.orEmpty()
                        tvAnnouncementCount.text = "未读 ${info?.unreadCount ?: 0} 条"
                        renderAnnouncements(items)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun renderAnnouncements(items: List<AnnouncementItem>) {
        announcementContainer.removeAllViews()
        if (items.isEmpty()) {
            val emptyView = layoutInflater.inflate(R.layout.item_empty_state, announcementContainer, false)
            emptyView.findViewById<TextView>(R.id.tv_empty_text).text = "暂无公告，后续通知会显示在这里"
            announcementContainer.addView(emptyView)
            return
        }

        items.forEach { item ->
            val card = layoutInflater.inflate(R.layout.item_profile_announcement, announcementContainer, false)
            card.findViewById<TextView>(R.id.tv_announcement_title).text = item.title
            card.findViewById<TextView>(R.id.tv_announcement_time).text = item.createTime
            card.findViewById<TextView>(R.id.tv_announcement_content).text = item.content
            val btnRead = card.findViewById<MaterialButton>(R.id.btn_mark_read)
            btnRead.visibility = if (item.read) View.GONE else View.VISIBLE
            card.alpha = if (item.read) 0.72f else 1f
            btnRead.setOnClickListener { markAnnouncementRead(item.announcementId) }
            announcementContainer.addView(card)
        }
    }

    private fun markAnnouncementRead(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.apiService.markAnnouncementRead(id)
                withContext(Dispatchers.Main) { loadAnnouncements() }
            } catch (_: Exception) {
            }
        }
    }

    private fun markAllAnnouncementsRead() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.apiService.markAllAnnouncementsRead()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "已标记全部已读", Toast.LENGTH_SHORT).show()
                    loadAnnouncements()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditProfileDialog() {
        val profile = currentProfile ?: return
        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etName = view.findViewById<TextInputEditText>(R.id.et_name)
        val etEmail = view.findViewById<TextInputEditText>(R.id.et_email)
        etName.setText(profile.realName)
        etEmail.setText(profile.email ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("编辑资料")
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text?.toString()?.trim().orEmpty()
                val email = etEmail.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "姓名不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                updateProfile(ProfileUpdateRequest(realName = name, email = email))
            }
            .show()
    }

    private fun showPasswordDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOld = view.findViewById<TextInputEditText>(R.id.et_old_password)
        val etNew = view.findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirm = view.findViewById<TextInputEditText>(R.id.et_confirm_password)

        AlertDialog.Builder(requireContext())
            .setTitle("修改密码")
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton("确认") { _, _ ->
                val oldPwd = etOld.text?.toString()?.trim().orEmpty()
                val newPwd = etNew.text?.toString()?.trim().orEmpty()
                val confirmPwd = etConfirm.text?.toString()?.trim().orEmpty()
                if (oldPwd.isBlank() || newPwd.length < 6 || newPwd != confirmPwd) {
                    Toast.makeText(requireContext(), "请检查密码输入", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                updateProfile(ProfileUpdateRequest(oldPassword = oldPwd, newPassword = newPwd), relogin = true)
            }
            .show()
    }

    private fun updateProfile(request: ProfileUpdateRequest, relogin: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.updateMyProfile(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val profile = response.body()?.data
                        if (profile != null) {
                            currentProfile = profile
                            bindProfile(profile)
                            if (!relogin) {
                                TokenManager.saveLoginData(
                                    token = TokenManager.getToken().orEmpty(),
                                    userId = profile.userId,
                                    username = profile.username,
                                    realName = profile.realName,
                                    role = profile.role,
                                )
                            }
                        }
                        Toast.makeText(requireContext(), if (relogin) "密码修改成功，请重新登录" else "资料已更新", Toast.LENGTH_SHORT).show()
                        if (relogin) {
                            TokenManager.clear()
                            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            requireActivity().finish()
                        }
                    } else {
                        Toast.makeText(requireContext(), response.body()?.message ?: "更新失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkVersionUpdate() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getLatestAppVersion()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        val versionInfo = response.body()?.data ?: return@withContext
                        if (versionInfo.versionCode <= BuildConfig.VERSION_CODE) {
                            Toast.makeText(requireContext(), "当前已是最新版本 v${BuildConfig.VERSION_NAME}", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }
                        val dialog = AlertDialog.Builder(requireContext())
                            .setTitle("发现新版本 v${versionInfo.versionName}")
                            .setMessage(versionInfo.releaseNotes.ifBlank { "有新版本可用，请更新后使用。" })
                            .setCancelable(false)
                        if (versionInfo.downloadUrl.isNotBlank()) {
                            dialog.setPositiveButton("立即更新") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.downloadUrl))
                                startActivity(intent)
                            }
                        }
                        dialog.setNegativeButton("稍后再说", null)
                        dialog.show()
                    } else {
                        Toast.makeText(requireContext(), "获取版本信息失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showServerSettings() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_settings, null)
        val etUrl = dialogView.findViewById<TextInputEditText>(R.id.et_server_url)
        etUrl.setText(ServerConfig.getBaseUrl())
        etUrl.setSelection(etUrl.text?.length ?: 0)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            val url = etUrl.text?.toString()?.trim().orEmpty()
            if (url.isNotBlank()) {
                ServerConfig.setBaseUrl(url)
                tvServerUrl.text = "服务器: ${ServerConfig.getBaseUrl()}"
                Toast.makeText(requireContext(), "服务器地址已更新，请重新登录", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_reset).setOnClickListener {
            ServerConfig.resetToDefault()
            etUrl.setText(ServerConfig.getBaseUrl())
            tvServerUrl.text = "服务器: ${ServerConfig.getBaseUrl()}"
            Toast.makeText(requireContext(), "已恢复默认地址", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
