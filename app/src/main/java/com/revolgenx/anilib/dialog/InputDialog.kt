package com.revolgenx.anilib.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.pranavpandey.android.dynamic.support.dialog.fragment.DynamicDialogFragment
import com.revolgenx.anilib.R
import com.revolgenx.anilib.util.getClipBoardText
import kotlinx.android.synthetic.main.input_dialog_layout.*

class InputDialog : BaseDialogFragment() {
    companion object {
        const val titleKey = "title_key"
        private const val inputTypeKey = "input_type_key"
        private const val defaultInputKey = "default_input_key"
        private const val showPasteButtonKey = "show_paste_button_key"
        val tag = InputDialog::class.java.simpleName
        fun newInstance(
            title: Int? = null,
            inputType: Int? = null,
            default: String? = null,
            showPasteButton: Boolean = true
        ): InputDialog {
            return InputDialog().also {
                it.arguments = bundleOf(
                    titleKey to title,
                    inputTypeKey to inputType,
                    defaultInputKey to default,
                    showPasteButtonKey to showPasteButton
                )
            }
        }
    }

    var onInputDoneListener: ((str: String) -> Unit)? = null

    override var titleRes: Int? = 0
        get() = arguments?.getInt(titleKey)?.takeIf { it != 0 }

    override var viewRes: Int? = R.layout.input_dialog_layout
    override var positiveText: Int? = R.string.done
    override var negativeText: Int? = R.string.cancel

    override fun onShowListener(alertDialog: DynamicDialog, savedInstanceState: Bundle?) {
        alertDialog.apply {
            arguments?.getInt(inputTypeKey)?.let {
                textInputEt.inputType = it
            }

            arguments?.getString(defaultInputKey)?.let {
                textInputEt.setText(it)
            }

            if (arguments?.getBoolean(showPasteButtonKey) == true) {
                pasteInputIv.setOnClickListener {
                    textInputEt.setText(requireContext().getClipBoardText())
                }
            } else {
                pasteInputIv.visibility = View.GONE
            }
        }
    }

    override fun onPositiveClicked(dialogInterface: DialogInterface, which: Int) {
        if (dialogInterface is DynamicDialog) {
            onInputDoneListener?.invoke(dialogInterface.textInputEt.text.toString().trim())
        }
    }

    override fun onNegativeClicked(dialogInterface: DialogInterface, which: Int) {
        dismiss()
    }

}