package org.nv.vinylmanager.di

import org.koin.dsl.module
import org.nv.vinylmanager.data.remote.MusicBrainzApi
import org.nv.vinylmanager.data.repository.InMemoryVinylRepository
import org.nv.vinylmanager.domain.repository.VinylRepository
import org.nv.vinylmanager.domain.usecase.CalculateAverageRatingUseCase
import org.nv.vinylmanager.domain.usecase.DailyPickManager
import org.nv.vinylmanager.domain.usecase.SuggestedToSellUseCase
import org.nv.vinylmanager.presentation.VinylViewModel

fun appModule() = module {
    single { MusicBrainzApi() }
    single { CalculateAverageRatingUseCase() }
    single { SuggestedToSellUseCase() }
    single<VinylRepository> { InMemoryVinylRepository(get(), get()) }
    single { DailyPickManager(get()) }
    single { VinylViewModel(get(), get(), get(), get()) }
}
