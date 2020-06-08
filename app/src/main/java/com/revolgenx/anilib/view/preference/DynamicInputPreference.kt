package com.revolgenx.anilib.view.preference

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.pranavpandey.android.dynamic.preferences.DynamicPreferences
import com.pranavpandey.android.dynamic.support.setting.DynamicSimplePreference
import com.revolgenx.anilib.R
import com.revolgenx.anilib.dialog.InputDialog

class DynamicInputPreference : DynamicSimplePreference {
    private var inputTextType: Int = InputType.TYPE_CLASS_TEXT
    private var preferenceType: Int = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    ) {
        val a = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.DynamicInputPreference
        )

        try {
            inputTextType = a.getInt(
                R.styleable.DynamicInputPreference_inputType,
                InputType.TYPE_CLASS_TEXT
            )
            preferenceType = a.getInt(
                R.styleable.DynamicInputPreference_preferenceType,
                0
            )
        } finally {
            a.recycle()
        }

        findViewById<ImageView?>(com.pranavpandey.android.dynamic.support.R.id.ads_preference_icon)?.visibility =
            View.GONE

        setOnPreferenceClickListener {
            (context as? AppCompatActivity)?.supportFragmentManager?.let { fragmentManager ->
                InputDialog.newInstance(
                    inputType = this@DynamicInputPreference.inputTextType,
                    default = prefValue
                ).let { dialog ->
                    dialog.onInputDoneListener = list@{ inputValue ->
                        if (inputValue.isEmpty()) return@list

                        setValueString(inputValue, true)
                        when (preferenceType) {
                            1 -> {
                                DynamicPreferences.getInstance()
                                    .save(preferenceKey!!, inputValue.toInt())
                            }
                            else -> {
                                DynamicPreferences.getInstance().save(preferenceKey!!, inputValue)
                            }
                        }
                    }
                    dialog.show(fragmentManager, InputDialog.tag)
                }
            }
        }

        setValueString(prefValue, true)
    }

    private val prefValue
        get() = if (preferenceType == 1) {
            DynamicPreferences.getInstance().load(preferenceKey!!, valueString.toString().toInt())
                .toString()
        } else {
            DynamicPreferences.getInstance().load(preferenceKey!!, valueString.toString())
        }

}