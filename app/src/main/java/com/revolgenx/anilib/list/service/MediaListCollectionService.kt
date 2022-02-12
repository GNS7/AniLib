package com.revolgenx.anilib.list.service

import com.revolgenx.anilib.app.setting.data.model.MediaListOptionModel
import com.revolgenx.anilib.app.setting.data.model.MediaListOptionTypeModel
import com.revolgenx.anilib.app.setting.data.model.getRowOrder
import com.revolgenx.anilib.infrastructure.repository.network.BaseGraphRepository
import com.revolgenx.anilib.infrastructure.repository.network.converter.toModel
import com.revolgenx.anilib.infrastructure.repository.util.ERROR
import com.revolgenx.anilib.infrastructure.repository.util.Resource
import com.revolgenx.anilib.list.constant.ListConstant
import com.revolgenx.anilib.list.data.field.MediaListCollectionField
import com.revolgenx.anilib.list.data.model.MediaListCollectionModel
import com.revolgenx.anilib.list.data.model.MediaListGroupModel
import com.revolgenx.anilib.list.data.model.MediaListModel
import com.revolgenx.anilib.list.data.model.toModel
import com.revolgenx.anilib.media.data.model.MediaModel
import com.revolgenx.anilib.media.data.model.toModel
import com.revolgenx.anilib.user.data.model.UserModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class MediaListCollectionService(private val graphRepository: BaseGraphRepository) {

    fun getMediaListCollection(
        field: MediaListCollectionField,
        compositeDisposable: CompositeDisposable,
        resourceCallback: (Resource<MediaListCollectionModel>) -> Unit
    ) {
        val disposable = graphRepository.request(field.toQueryOrMutation())
            .map {
                it.data?.mediaListCollection?.let { collection ->
                    MediaListCollectionModel().also { collectionModel ->
                        collectionModel.user = collection.user?.let { user ->
                            UserModel().also { userModel ->
                                userModel.id = user.id
                                userModel.mediaListOptions =
                                    user.mediaListOptions?.let { option ->
                                        MediaListOptionModel().also { optionModel ->
                                            optionModel.scoreFormat = option.scoreFormat?.ordinal
                                            optionModel.rowOrder = getRowOrder(option.rowOrder)
                                            optionModel.rowOrder = getRowOrder(option.rowOrder)
                                            optionModel.animeList =
                                                option.animeList?.let { type ->
                                                    MediaListOptionTypeModel().also { typeModel ->
                                                        typeModel.customLists =
                                                            type.customLists?.filterNotNull()
                                                        typeModel.sectionOrder =
                                                            type.sectionOrder?.filterNotNull()
                                                    }
                                                }
                                            optionModel.mangaList =
                                                option.mangaList?.let { type ->
                                                    MediaListOptionTypeModel().also { typeModel ->
                                                        typeModel.customLists =
                                                            type.customLists?.filterNotNull()
                                                        typeModel.sectionOrder =
                                                            type.sectionOrder?.filterNotNull()
                                                    }
                                                }
                                        }
                                    }
                            }
                        }

                        collectionModel.lists = collection.lists?.filterNotNull()?.map { group ->
                            MediaListGroupModel().also { groupModel ->
                                groupModel.name = group.name
                                groupModel.isCustomList = group.isCustomList == true
                                groupModel.isCompletedList = group.isCompletedList == true
                                groupModel.entries = group.entries?.filterNotNull()?.map {
                                    it.mediaListEntry.toModel().also { entry ->
                                        entry.media = it.media?.mediaContent?.toModel()
                                    }
                                }?.toMutableList()
                            }
                        }?.toMutableList()?.also {
                            val model = MediaListGroupModel()
                            model.name = "All"
                            model.entries =
                                it.filter { f -> ListConstant.listDefaultGroup.contains(f.name) }
                                    .flatMap { it.entries ?: emptyList() }.toMutableList()
                            it.add(model)
                        }
                    }
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                resourceCallback.invoke(Resource.success(it))
            }, {
                Timber.e(it)
                resourceCallback.invoke(Resource.error(it.message ?: ERROR, null, it))
            })
        compositeDisposable.add(disposable)
    }
}