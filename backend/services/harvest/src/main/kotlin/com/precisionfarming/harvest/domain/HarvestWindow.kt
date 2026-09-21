package com.precisionfarming.harvest.domain

import com.precisionfarming.common.DomainException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

object HarvestWindow {
    data class Range(val start: Instant, val end: Instant)

    fun resolve(start: Instant?, end: Instant?, now: Instant): Range {
        if (start == null && end == null) {
            return Range(now.plus(1, ChronoUnit.DAYS), now.plus(5, ChronoUnit.DAYS))
        }
        if (start == null || end == null) {
            throw DomainException("HARVEST_WINDOW_INVALID", "Harvest window is invalid")
        }
        if (!end.isAfter(start) || Duration.between(start, end) > Duration.ofDays(180)) {
            throw DomainException("HARVEST_WINDOW_INVALID", "Harvest window is invalid")
        }
        return Range(start, end)
    }
}
