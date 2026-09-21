package com.precisionfarming.harvest.domain

import com.precisionfarming.common.DomainException
import java.math.BigDecimal

object StorageOccupancy {
    fun usedT(lots: Iterable<BigDecimal>): BigDecimal =
        lots.fold(BigDecimal.ZERO, BigDecimal::add)

    fun requireFits(capacityT: BigDecimal, usedT: BigDecimal) {
        if (capacityT.signum() <= 0 || usedT.signum() < 0 || usedT > capacityT) {
            throw DomainException("STORAGE_CAPACITY_INVALID", "usedT must be between 0 and capacityT")
        }
    }
}
