package com.revolgenx.anilib.dialog

import android.os.Bundle
import androidx.core.os.bundleOf
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.pranavpandey.android.dynamic.support.dialog.fragment.DynamicDialogFragment
import com.revolgenx.anilib.R
import com.revolgenx.anilib.util.getClipBoardText
import kotlinx.android.synthetic.main.input_dialog_layout.*

class InputDialog : DynamicDialogFragment() {
    companion object {
        const val titleKey = "title_key"
        val tag = InputDialog::class.java.simpleName
        fun newInstance(title: Int? = null): InputDialog {
            return InputDialog().also {
                it.arguments = bundleOf(titleKey to title)
            }
        }
    }

    var onInputDoneListener: ((str: String) -> Unit)? = null

    override fun onCustomiseBuilder(
        dialogBuilder: DynamicDialog.Builder,
        savedInstanceState: Bundle?
    ): DynamicDialog.Builder {
        with(dialogBuilder) {
            setTitle(arguments?.getInt(titleKey) ?: R.string.input_text)
            setView(R.layout.input_dialog_layout)
            setPositiveButton(R.string.done) { dialogInterface, _ ->
                if (dialogInterface is DynamicDialog) {
                    onInputDoneListener?.invoke(dialogInterface.textInputEt.text.toString())
                }
            }
            setNegativeButton(R.string.cancel) { _, _ ->
                dismiss()
            }
            isAutoDismiss = false
        }
        return super.onCustomiseBuilder(dialogBuilder, savedInstanceState)
    }

    override fun onCustomiseDialog(
        alertDialog: DynamicDialog,
        savedInstanceState: Bundle?
    ): DynamicDialog {
        alertDialog.apply {
            setOnShowListener {
                pasteInputIv.setOnClickListener {
                    textInputEt.setText(requireContext().getClipBoardText())
                }
            }
        }
        return super.onCustomiseDialog(alertDialog, savedInstanceState)
    }
}