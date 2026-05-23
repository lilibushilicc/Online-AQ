package com.onlineaq.student.ui.practice

import android.text.Editable
import android.text.TextWatcher

class SimpleAfterTextChanged(
    private val onChanged: (String) -> Unit,
) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(s: Editable?) {
        onChanged(s?.toString().orEmpty())
    }
}
