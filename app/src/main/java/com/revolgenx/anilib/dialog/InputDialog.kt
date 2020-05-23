package com.revolgenx.anilib.dialog

import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.pranavpandey.android.dynamic.support.dialog.fragment.DynamicDialogFragment
import com.revolgenx.anilib.R
import com.revolgenx.anilib.util.getClipBoardText
import kotlinx.android.synthetic.main.input_dialog_layout.*

class InputDialog : BaseDialogFragment() {
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

    override var titleRes: Int = 0
        get() = arguments?.getInt(titleKey) ?: R.string.input_text

    override var viewRes: Int = R.layout.input_dialog_layout
    override var positiveText: Int = R.string.done
    override var negativeText: Int = R.string.cancel

    override fun onShowListener(alertDialog: DynamicDialog) {
        alertDialog.apply {
            pasteInputIv.setOnClickListener {
                textInputEt.setText(requireContext().getClipBoardText())
            }
        }
    }

    override fun onPositiveClicked(dialogInterface: DialogInterface, which: Int) {
        if (dialogInterface is DynamicDialog) {
            onInputDoneListener?.invoke(dialogInterface.textInputEt.text.toString())
        }
    }

    override fun onNegativeClicked(dialogInterface: DialogInterface, which: Int) {
        dismiss()
    }

}