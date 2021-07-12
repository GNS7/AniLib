package com.revolgenx.anilib.social.data.field

import com.revolgenx.anilib.ActivityUnionQuery
import com.revolgenx.anilib.data.field.BaseSourceField
import com.revolgenx.anilib.data.field.BaseSourceUserField
import com.revolgenx.anilib.data.meta.type.AlActivityType
import com.revolgenx.anilib.type.ActivityType

open class ActivityUnionField : BaseSourceUserField<ActivityUnionQuery>() {

    var isFollowing: Boolean = false
    var type: AlActivityType = AlActivityType.ALL
    var mediaId:Int? = null
    override fun toQueryOrMutation(): ActivityUnionQuery {
        return ActivityUnionQuery.builder()
            .page(page)
            .perPage(perPage)
            .apply {
                userId?.let {
                    userId(it)
                }
                mediaId?.let {
                    mediaId(it)
                }
                if (isFollowing) {
                    isFollowing(true)
                }
                when (type) {
                    AlActivityType.TEXT -> type(ActivityType.TEXT)
                    AlActivityType.LIST -> type(ActivityType.MEDIA_LIST)
                    AlActivityType.MESSAGE -> type(ActivityType.MESSAGE)
                    else ->
                        if (userId == null) {
                            type_in(
                                listOf(
                                    ActivityType.MEDIA_LIST,
                                    ActivityType.TEXT,
                                    ActivityType.ANIME_LIST,
                                    ActivityType.MANGA_LIST
                                )
                            )
                        }
                }
            }
            .build()
    }

}
