/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.zeapo.pwdstore.R

class MultiselectableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var multiselected: Boolean = false

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        if (multiselected) {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            View.mergeDrawableStates(drawableState, STATE_MULTISELECTED)
            return drawableState
        }
        return super.onCreateDrawableState(extraSpace)
    }

    fun setMultiSelected(on: Boolean) {
        if (!multiselected) {
            multiselected = true
            refreshDrawableState()
        }
        isActivated = on
    }

    companion object {
        private val STATE_MULTISELECTED = intArrayOf(R.attr.state_multiselected)
    }
}
