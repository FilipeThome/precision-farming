package com.precisionfarming.reporting

import com.precisionfarming.reporting.application.ReportingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReportingCsvCellTest {
    @Test
    fun prefixesExcelFormulasAndQuotesRfc4180() {
        assertEquals("plain", ReportingService.csvCell("plain"))
        assertEquals("\"a,b\"", ReportingService.csvCell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", ReportingService.csvCell("say \"hi\""))
        assertEquals("\"'=CMD()\"", ReportingService.csvCell("=CMD()"))
        assertTrue(ReportingService.csvCell(" =CMD()").contains("'="))
        assertTrue(ReportingService.csvCell("@SUM(A1)").startsWith("\"'@"))
        assertTrue(ReportingService.csvCell("+1+1").startsWith("\"'+"))
    }
}
