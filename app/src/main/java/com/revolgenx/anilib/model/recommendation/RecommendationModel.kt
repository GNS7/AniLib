package com.revolgenx.anilib.model.recommendation

import com.revolgenx.anilib.model.BaseModel
import com.revolgenx.anilib.model.CommonMediaModel

open class RecommendationModel : BaseModel() {
    var recommendationId: Int? = null
        set(value) {
            baseId = value
            field = value
        }

    var rating: Int? = null
    var userRating: Int? = null
    var recommendationFrom: CommonMediaModel? = null
    var recommended: CommonMediaModel? = null
}
