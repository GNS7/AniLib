package com.revolgenx.anilib.adapter

import android.util.SparseBooleanArray
import android.widget.Filter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView


abstract class SelectableAdapter<VH : RecyclerView.ViewHolder, T : Any>(diffUtil: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffUtil) {

    val selectedItemCount: Int
        get() = selectedItems.size()

    private val selectedItems: SparseBooleanArray by lazy { SparseBooleanArray() }
    private val filter = SearchFilter()
    protected val searchTempList = mutableListOf<T>()


    fun isSelected(position: Int): Boolean {
        return getSelectedItems().contains(position)
    }

    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }


    fun clearSelection() {
        val selection = getSelectedItems()
        for (i in selection) {
            notifyItemChanged(i)
        }
        selectedItems.clear()
    }

    fun getSelectedItems(): List<Int> {
        val items = mutableListOf<Int>()
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    fun setSelectedItems(items: ArrayList<Int>) {
        for (i in items) {
            selectedItems.put(i, true)
        }
    }

    fun selectAll() {
        for (i in 0 until currentList.size) {
            selectedItems.put(i, true)
        }
        notifyDataSetChanged()
    }

    inner class SearchFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            this@SelectableAdapter.performFiltering(constraint)
            return FilterResults()
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }

    abstract fun performFiltering(constraint: CharSequence?)
    fun search(query: String) {
        filter.filter(query)
    }
}