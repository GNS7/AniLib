package com.revolgenx.anilib.dialog

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.pranavpandey.android.dynamic.support.dialog.fragment.DynamicDialogFragment
import com.pranavpandey.android.dynamic.support.widget.DynamicButton


typealias OnShowListener = ((dialog: DynamicDialog) -> Unit)?
typealias OnButtonClickedListener = ((dialogInterface: DialogInterface, which: Int) -> Unit)?

abstract class BaseDialogFragment : DynamicDialogFragment() {

    protected open var titleRes: Int = 0
    protected open var viewRes: Int = 0
    protected open var positiveText: Int = 0
    protected open var negativeText: Int = 0

    protected open var neutralText: Int = 0
    protected open var messageText: Int = 0

    var isAutoDismissEnabled = false

    var onShowListener: OnShowListener = null
    var onButtonClickedListener: OnButtonClickedListener = null

    protected open fun onPositiveClicked(dialogInterface: DialogInterface, which: Int) {
        onButtonClickedListener?.invoke(dialogInterface, which)
    }

    protected open fun onNegativeClicked(dialogInterface: DialogInterface, which: Int) {
        onButtonClickedListener?.invoke(dialogInterface, which)
    }

    protected open fun onNeutralClicked(dialogInterface: DialogInterface, which: Int) {
        onButtonClickedListener?.invoke(dialogInterface, which)
    }

    protected open fun onShowListener(alertDialog: DynamicDialog) {
        onShowListener?.invoke(alertDialog)
    }


    override fun onCustomiseBuilder(
        dialogBuilder: DynamicDialog.Builder,
        savedInstanceState: Bundle?
    ): DynamicDialog.Builder {
        with(dialogBuilder) {
            setTitle(titleRes)
            if (viewRes != 0)
                setView(viewRes)
            if (messageText != 0)
                setMessage(messageText)
            setPositiveButton(positiveText) { dialog, which ->
                onPositiveClicked(dialog, which)
            }
            setNegativeButton(negativeText) { dialog, which ->
                onNegativeClicked(dialog, which)
            }
            if (neutralText != 0) {
                setNeutralButton(neutralText) { dialog, which ->
                    onNeutralClicked(dialog, which)
                }
            }
            isAutoDismiss = isAutoDismissEnabled
        }
        return super.onCustomiseBuilder(dialogBuilder, savedInstanceState)
    }


    override fun onCustomiseDialog(
        alertDialog: DynamicDialog,
        savedInstanceState: Bundle?
    ): DynamicDialog {
        with(alertDialog) {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE)?.let {
                    (it as DynamicButton).isAllCaps = false
                }

                getButton(AlertDialog.BUTTON_NEGATIVE)?.let {
                    (it as DynamicButton).isAllCaps = false
                }
                getButton(AlertDialog.BUTTON_NEUTRAL)?.let {
                    (it as DynamicButton).isAllCaps = false
                }

                onShowListener(alertDialog)
            }
        }
        return super.onCustomiseDialog(alertDialog, savedInstanceState)
    }
}