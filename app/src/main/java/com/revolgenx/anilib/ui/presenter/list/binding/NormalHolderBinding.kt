package com.revolgenx.anilib.ui.presenter.list.binding

import android.content.Context
import android.graphics.Color
import android.view.View
import com.apollographql.apollo.exception.ApolloHttpException
import com.revolgenx.anilib.R
import com.revolgenx.anilib.common.preference.listEditOrBrowse
import com.revolgenx.anilib.constant.HTTP_TOO_MANY_REQUEST
import com.revolgenx.anilib.infrastructure.event.BrowseGenreEvent
import com.revolgenx.anilib.data.model.EntryListEditorMediaModel
import com.revolgenx.anilib.data.model.list.MediaListModel
import com.revolgenx.anilib.data.model.search.filter.MediaSearchFilterModel
import com.revolgenx.anilib.databinding.MediaListCollectionNormalPresenterLayoutBinding
import com.revolgenx.anilib.ui.presenter.home.discover.MediaListCollectionPresenter
import com.revolgenx.anilib.infrastructure.repository.util.Status
import com.revolgenx.anilib.type.MediaType
import com.revolgenx.anilib.type.ScoreFormat
import com.revolgenx.anilib.ui.view.makeToast
import com.revolgenx.anilib.util.naText
import com.revolgenx.anilib.ui.viewmodel.list.MediaListCollectionViewModel

object NormalHolderBinding {


    fun bind(
        binding: MediaListCollectionNormalPresenterLayoutBinding,
        context: Context,
        item: MediaListModel,
        mediaFormats: Array<String>,
        mediaStatus: Array<String>,
        statusColors: Array<String>,
        isLoggedInUser: Boolean,
        viewModel: MediaListCollectionViewModel
    ) {
        binding.apply {
            mediaListTitleTv.text = item.title?.userPreferred
            mediaListCoverImageView.setImageURI(item.coverImage?.large)
            mediaListFormatTv.text = item.format?.let {
                mediaFormats[it]
            }.naText()

            mediaListStatusTv.text = item.status?.let {
                mediaListStatusTv.color = Color.parseColor(statusColors[it])
                mediaStatus[it]
            }.naText()

            mediaListProgressTv.text =
                context.getString(R.string.s_slash_s_brackets)
                    .format(
                        item.progress?.toString().naText(),
                        if (item.type == MediaType.ANIME.ordinal) item.episodes.naText() else item.chapters.naText(),
                        context.getString(R.string.startdate_format).format(
                            item.listStartDate?.toString().naText(),
                            item.listCompletedDate?.toString().naText()
                        )
                    )

            mediaListProgressTv.compoundDrawablesRelative[0]?.setTint(MediaListCollectionPresenter.textPrimaryColor)

            when (item.scoreFormat) {
                ScoreFormat.POINT_3.ordinal -> {
                    val drawable = when (item.score?.toInt()) {
                        1 -> R.drawable.ic_score_sad
                        2 -> R.drawable.ic_score_neutral
                        3 -> R.drawable.ic_score_smile
                        else -> R.drawable.ic_role
                    }
                    mediaListRatingTv.setImageResource(drawable)
                    mediaListRatingTv.scoreTextVisibility = View.GONE
                }
                ScoreFormat.POINT_10_DECIMAL.ordinal -> {
                    mediaListRatingTv.setText(item.score)
                }
                else -> {
                    mediaListRatingTv.text = item.score?.toInt()
                }
            }

            mediaListGenreLayout.addGenre(item.genres?.take(3)) { genre ->
                BrowseGenreEvent(MediaSearchFilterModel().also {
                    it.genre = listOf(genre.trim())
                }).postEvent
            }

            mediaListStartDateTv.text = context.getString(R.string.startdate_format)
                .format(item.startDate?.toString().naText(), item.endDate?.toString().naText())

            if (isLoggedInUser) {
                mediaListProgressIncrease.setOnClickListener {
                    viewModel.increaseProgress(EntryListEditorMediaModel().also {
                        it.mediaId = item.mediaId
                        it.listId = item.mediaListId
                        it.progress = (item.progress ?: 0).plus(1)
                    }) { res ->
                        when (res.status) {
                            Status.SUCCESS -> {
                                if (res.data?.mediaId == item.mediaId) {
                                    item.progress = res.data?.progress
                                    mediaListProgressTv.text =
                                        context.getString(R.string.s_slash_s_brackets)
                                            .format(
                                                item.progress?.toString().naText(),
                                                if (item.type == MediaType.ANIME.ordinal) item.episodes.naText() else item.chapters.naText(),
                                                context.getString(R.string.startdate_format)
                                                    .format(
                                                        item.listStartDate?.toString().naText(),
                                                        item.listCompletedDate?.toString()
                                                            .naText()
                                                    )
                                            )
                                }
                            }
                            Status.ERROR -> {
                                if (res.exception is ApolloHttpException) {
                                    when (res.exception.code()) {
                                        HTTP_TOO_MANY_REQUEST -> {
                                            context.makeToast(
                                                R.string.too_many_request,
                                                icon = R.drawable.ic_error
                                            )
                                        }
                                        else -> {
                                            context.makeToast(
                                                R.string.operation_failed,
                                                icon = R.drawable.ic_error
                                            )
                                        }
                                    }
                                } else {
                                    context.makeToast(
                                        R.string.operation_failed,
                                        icon = R.drawable.ic_error
                                    )
                                }
                            }
                            Status.LOADING -> {

                            }
                        }

                    }
                }

            } else {
                mediaListProgressIncrease.visibility = View.GONE
            }


            root.setOnClickListener {
                if (isLoggedInUser) {
                    if (listEditOrBrowse(context)) {
                        ListBindingHelper.openMediaListEditor(
                            context,
                            item,
                            mediaListCoverImageView,
                            isLoggedInUser,
                            R.id.drawer_layout
                        )
                        return@setOnClickListener;
                    }
                }
                ListBindingHelper.openMediaBrowse(context, item, mediaListCoverImageView)
            }

            root.setOnLongClickListener {
                if (isLoggedInUser) {
                    if (listEditOrBrowse(context)) {
                        ListBindingHelper.openMediaBrowse(context, item, mediaListCoverImageView)
                        return@setOnLongClickListener true
                    }
                }
                ListBindingHelper.openMediaListEditor(context, item, mediaListCoverImageView, isLoggedInUser, R.id.drawer_layout)
                true
            }
        }
    }
}