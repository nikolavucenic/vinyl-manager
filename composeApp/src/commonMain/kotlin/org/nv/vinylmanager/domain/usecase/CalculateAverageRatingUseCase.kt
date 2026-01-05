package org.nv.vinylmanager.domain.usecase

import org.nv.vinylmanager.domain.model.Track

class CalculateAverageRatingUseCase {
    operator fun invoke(tracks: List<Track>): Double? {
        val rated = tracks.mapNotNull { it.rating }
        if (rated.isEmpty()) return null
        return rated.average()
    }
}
