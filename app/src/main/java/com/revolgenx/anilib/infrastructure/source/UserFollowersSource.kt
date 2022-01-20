package com.revolgenx.anilib.infrastructure.source

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.revolgenx.anilib.common.infrastruture.source.BaseRecyclerSource
import com.revolgenx.anilib.user.data.field.UserFollowerField
import com.revolgenx.anilib.user.data.model.UserFollowersModel
import com.revolgenx.anilib.user.service.UserService
import io.reactivex.disposables.CompositeDisposable

class UserFollowersSource(
    field: UserFollowerField,
    private val userService: UserService,
    private val compositeDisposable: CompositeDisposable
) :
    BaseRecyclerSource<UserFollowersModel, UserFollowerField>(field) {
    override fun areItemsTheSame(first: UserFollowersModel, second: UserFollowersModel): Boolean {
        return first.id == second.id
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        super.onPageOpened(page, dependencies)
        field.page = pageNo
        userService.getFollowersUsers(field, compositeDisposable) {
            postResult(page, it)
        }
    }
}
