package com.example.solarShop.data.modules

import com.example.solarShop.repo.LanguageRepository
import com.example.solarShop.repo.LanguageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LanguageModule {

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        impl: LanguageRepositoryImpl
    ): LanguageRepository
}
