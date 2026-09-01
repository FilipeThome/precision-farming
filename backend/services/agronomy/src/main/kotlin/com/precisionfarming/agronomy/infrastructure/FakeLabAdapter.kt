package com.precisionfarming.agronomy.infrastructure

import com.precisionfarming.agronomy.domain.LabAdapter
import com.precisionfarming.agronomy.domain.LabResult
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FakeLabAdapter : LabAdapter {
    override fun analyze(sampleKey: String): LabResult {
        val hash = sampleKey.hashCode().and(0x7fffffff)
        return LabResult(
            labRef = "LAB-$sampleKey",
            ph = BigDecimal("5.${50 + hash % 40}"),
            organicMatterPct = BigDecimal("${2 + hash % 4}.${hash % 10}"),
            pPpm = BigDecimal("${8 + hash % 20}"),
            kPpm = BigDecimal("${40 + hash % 60}"),
        )
    }
}
