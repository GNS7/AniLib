package com.revolgenx.anilib.viewmodel

import androidx.lifecycle.viewModelScope
import com.revolgenx.anilib.field.MediaListFilterField
import com.revolgenx.anilib.field.list.MediaListField
import com.revolgenx.anilib.model.EntryListEditorMediaModel
import com.revolgenx.anilib.model.list.MediaListModel
import com.revolgenx.anilib.repository.util.Resource
import com.revolgenx.anilib.service.list.MediaListService
import com.revolgenx.anilib.service.media.MediaListEntryService
import com.revolgenx.anilib.source.MediaListSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
* todo addmedialist might not update on rotaion rare case
* todo create proper observer not stable for increasing progress
* */
abstract class MediaListViewModel(
    private val mediaListService: MediaListService,
    private val entryService: MediaListEntryService
) :
    SourceViewModel<MediaListSource, MediaListField>() {
    var filteredList: MutableList<MediaListModel>? = null
    val listMap by lazy { mutableMapOf<Int, MediaListModel>() }
    var filter = MediaListFilterField()

    override var field: MediaListField = MediaListField()

    override fun createSource(): MediaListSource {
        filteredList = null
        if (source == null) {
            source = MediaListSource(field, listMap, mediaListService, compositeDisposable)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                filteredList = if (filter.format == null) listMap.values else {
                    listMap.values.filter { it.format == filter.format }
                }.let {
                    if (filter.status == null) it else it.filter { it.status == filter.status }
                }.let {
                    if (filter.genre == null) it else it.filter { it.genres?.contains(filter.genre!!) == true }
                }.let {
                    if (filter.search.isNullOrEmpty()) it else {
                        it.filter {
                            it.title?.userPreferred?.contains(
                                filter.search!!, true
                            ) == true
                        }
                    }
                }.toMutableList()
                launch(Dispatchers.Main) {
                    source?.filterPage(filteredList!!)
                }
            }
        }
        return source!!
    }



    fun increaseProgress(model:EntryListEditorMediaModel, callback: (Resource<EntryListEditorMediaModel>) -> Unit){
        entryService.increaseProgress(model, compositeDisposable, callback)
    }



    override fun onCleared() {
        listMap.clear()
        filteredList = null
        compositeDisposable.clear()
        super.onCleared()
    }

    fun renewSource() {
        source = null
        filteredList = null
        listMap.clear()
    }
}
