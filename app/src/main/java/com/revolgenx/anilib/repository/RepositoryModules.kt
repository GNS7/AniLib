package com.revolgenx.anilib.repository

import androidx.room.Room
import com.revolgenx.anilib.repository.db.AnilibDatabase
import com.revolgenx.anilib.repository.db.TorrentRepository
import com.revolgenx.anilib.repository.network.BaseGraphRepository
import com.revolgenx.anilib.repository.network.NetworkProvider
import com.revolgenx.anilib.repository.network.GraphRepositoryImpl
import org.koin.dsl.module

val networkModules = module {
    single { NetworkProvider.provideApolloClient(get()) }
    single { NetworkProvider.provideOkHttpClient() }
}

val repositoryModules = module {
    factory<BaseGraphRepository> { GraphRepositoryImpl(get()) }
    single { TorrentRepository(get()) }
}

val databaseModules = module{
    //database
    single {
        Room.databaseBuilder(get(), AnilibDatabase::class.java, "anilib_v2.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    single {
        get<AnilibDatabase>().torrentDao()
    }
}
