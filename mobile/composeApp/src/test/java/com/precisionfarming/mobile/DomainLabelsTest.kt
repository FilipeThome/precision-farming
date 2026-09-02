package com.precisionfarming.mobile

import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.LocaleStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DomainLabelsTest {
    @Test
    fun unknownUuidFallsBackToShortId() {
        LocaleStore.setLocale(AppLocale.EN_US)
        assertEquals("aaaaaaaa", DomainLabels.label("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        assertEquals("bbbbbbbb", DomainLabels.label("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
        assertNotEquals(
            DomainLabels.label("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            DomainLabels.label("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        )
    }

    @Test
    fun seededCodesMatchWebTables() {
        LocaleStore.setLocale(AppLocale.PT_BR)
        assertEquals("Troca de filtros", DomainLabels.label("FILTER_CHANGE"))
        assertEquals("Pivô Norte", DomainLabels.label("PIVOT_NORTH"))
        assertEquals("Déficit hídrico estimado", DomainLabels.label("WATER_DEFICIT"))
        assertEquals("Óleo hidráulico", DomainLabels.label("HYDRAULIC_OIL"))
        assertEquals("Talhão Nordeste", DomainLabels.label("9e876222-3f39-3190-ad8c-1c147b28f6e6"))
        assertEquals("Talhão Leste", DomainLabels.label("4aacee0b-3a7c-3823-bd95-4f86bf857350"))
        assertEquals("Drone", DomainLabels.label("DRONE"))
        assertEquals("Drone 01", DomainLabels.label("9861d50d-527b-385c-b89b-b0674467015f"))
        assertEquals("Drone 02", DomainLabels.label("9b2296fa-d133-37db-be2f-be69dc802915"))
        assertEquals("Ocioso", DomainLabels.label("IDLE"))
        LocaleStore.setLocale(AppLocale.EN_US)
        assertEquals("DemoJohnDeere", DomainLabels.label("DemoJohnDeere"))
        assertEquals("listMachines", DomainLabels.label("listMachines"))
        assertEquals("North Pivot", DomainLabels.label("PIVOT_NORTH"))
        assertEquals("Northeast Field", DomainLabels.label("9e876222-3f39-3190-ad8c-1c147b28f6e6"))
    }
}
