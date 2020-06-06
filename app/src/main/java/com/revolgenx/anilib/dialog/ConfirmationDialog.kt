package com.revolgenx.anilib.dialog

import com.revolgenx.anilib.R

class ConfirmationDialog : BaseDialogFragment() {

    companion object {
        val TAG: String = ConfirmationDialog::class.java.simpleName
        open class Builder {
            private var title: Int? = null
            private var viewRes: Int? = null
            private var positiveText: Int? = null
            private var negativeText: Int? = null
            private var neutralText: Int? = null
            private var messageText: Int? = null
            private var onShowListener: OnShowListener = null
            private var onButtonClickedListener: OnButtonClickedListener =
                null

            fun titleRes(res: Int) {
                title = res
            }

            fun viewRes(res: Int) {
                viewRes = res
            }

            fun positiveTextRes(textRes: Int) {
                this.positiveText = textRes
            }

            fun negativeTextRes(textRes: Int) {
                this.negativeText = textRes
            }

            fun neutralTextRes(textRes: Int) {
                this.neutralText = textRes
            }

            fun messageTextRes(textRes: Int) {
                this.messageText = textRes
            }

            fun onButtonClicked(listener: OnButtonClickedListener) {
                this.onButtonClickedListener = listener
            }

            fun onShowListener(listener: OnShowListener) {
                this.onShowListener = listener
            }

            fun build(): ConfirmationDialog {
                return ConfirmationDialog().also {
                    it.titleRes = title!!
                    it.viewRes = viewRes ?: 0
                    it.messageText = messageText ?: 0
                    it.neutralText = neutralText ?: 0
                    it.onShowListener = onShowListener
                    it.onButtonClickedListener = onButtonClickedListener
                }
            }

        }
    }

    override var positiveText: Int? = R.string.yes
    override var negativeText: Int? = R.string.no

}