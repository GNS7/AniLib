package com.revolgenx.anilib.viewmodel

import com.revolgenx.anilib.field.search.BrowseField
import com.revolgenx.anilib.field.search.MediaSearchField
import com.revolgenx.anilib.service.BrowseService
import com.revolgenx.anilib.source.BrowseSource
import com.revolgenx.anilib.type.MediaSort

class BrowseFragmentViewModel(private val browseService: BrowseService) :
    SourceViewModel<BrowseSource, BrowseField>() {

    var field: BrowseField = MediaSearchField().also {
        it.sort = MediaSort.TRENDING_DESC.ordinal
    }

    override fun createSource(field: BrowseField): BrowseSource {
        source = BrowseSource(field, browseService, compositeDisposable)
        return source!!
    }

}
