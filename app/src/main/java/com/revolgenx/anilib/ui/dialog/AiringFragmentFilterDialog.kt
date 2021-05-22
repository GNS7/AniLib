package com.revolgenx.anilib.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.revolgenx.anilib.R
import com.revolgenx.anilib.common.preference.getAiringField
import com.revolgenx.anilib.common.preference.loggedIn
import com.revolgenx.anilib.common.preference.storeAiringField
import com.revolgenx.anilib.common.ui.dialog.BaseDialogFragment
import com.revolgenx.anilib.data.meta.type.ALAiringSort
import com.revolgenx.anilib.databinding.AiringFilterDialogLayoutBinding
import com.revolgenx.anilib.ui.dialog.sorting.AniLibSortingModel
import com.revolgenx.anilib.ui.dialog.sorting.SortOrder
import com.revolgenx.anilib.ui.view.makeSpinnerAdapter

class AiringFragmentFilterDialog : BaseDialogFragment<AiringFilterDialogLayoutBinding>() {
    companion object {
        fun newInstance(): AiringFragmentFilterDialog {
            return AiringFragmentFilterDialog()
        }
    }

    private val airingField by lazy {
        getAiringField(requireContext())
    }

    override var positiveText: Int? = R.string.done
    override var negativeText: Int? = R.string.cancel
    override var titleRes: Int? = R.string.filter

    var onDoneListener: (() -> Unit)? = null

    override fun bindView(): AiringFilterDialogLayoutBinding {
        return AiringFilterDialogLayoutBinding.inflate(provideLayoutInflater)
    }

    override fun builder(dialogBuilder: DynamicDialog.Builder, savedInstanceState: Bundle?) {
        binding.showAllAiringSwitch.isChecked = !airingField.notYetAired

        if (requireContext().loggedIn()) {
            binding.showFromWatchListSwitch.visibility = View.VISIBLE
            binding.showFromPlanningListSwitch.visibility = View.VISIBLE
            binding.showFromPlanningListSwitch.isChecked = airingField.showFromPlanning
            binding.showFromWatchListSwitch.isChecked = airingField.showFromWatching
        }


        val saveSortIndex:Int
        val savedSortOrder: SortOrder
        val alAiringSortEnums = ALAiringSort.values()


        val savedAiringSort = airingField.sort!!

        savedSortOrder = if (savedAiringSort % 2 == 0) {
            saveSortIndex = alAiringSortEnums.first { it.sort == savedAiringSort }.ordinal
            SortOrder.ASC
        } else {
            saveSortIndex = alAiringSortEnums.first { it.sort == savedAiringSort - 1 }.ordinal
            SortOrder.DESC
        }

        resources.getStringArray(R.array.al_airing_sort).mapIndexed { index, s ->
            AniLibSortingModel(
                alAiringSortEnums[index],
                s,
                if (index == saveSortIndex) savedSortOrder else SortOrder.NONE,
                allowNone = false
            )
        }.let {
            binding.alAiringSort.setSortItems(it)
        }
    }

    override fun onPositiveClicked(dialogInterface: DialogInterface, which: Int) {
        airingField.notYetAired = !binding.showAllAiringSwitch.isChecked
        airingField.showFromPlanning = binding.showFromPlanningListSwitch.isChecked
        airingField.showFromWatching = binding.showFromWatchListSwitch.isChecked
        airingField.sort = getActiveAiringSort()
        storeAiringField(requireContext(), airingField)

        onDoneListener?.invoke()

        super.onPositiveClicked(dialogInterface, which)
    }

    private fun getActiveAiringSort(): Int {
        return binding.alAiringSort.getActiveSortItem()!!.let {
            if (it.order == SortOrder.DESC) {
                (it.data as ALAiringSort).sort + 1
            } else {
                (it.data as ALAiringSort).sort
            }
        }
    }

}