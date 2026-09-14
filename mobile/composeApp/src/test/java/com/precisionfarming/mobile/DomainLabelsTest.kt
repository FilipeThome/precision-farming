package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.EntityNames
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.MachineDto
import com.precisionfarming.mobile.i18n.AppLocale
import com.precisionfarming.mobile.i18n.DomainLabels
import com.precisionfarming.mobile.i18n.LocaleStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DomainLabelsTest {
    @After
    fun tearDown() = EntityNames.clear()

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
    fun uuidResolvesOnlyAfterBackendReturnedTheEntity() {
        val farmId = "11111111-1111-1111-1111-111111111111"
        val fieldId = "22222222-2222-2222-2222-222222222222"
        val machineId = "33333333-3333-3333-3333-333333333333"
        assertEquals("11111111", DomainLabels.label(farmId))

        EntityNames.registerFarms(listOf(FarmDto(id = farmId, name = "Fazenda Rio Claro", location = "MT")))
        EntityNames.registerFields(listOf(FieldDto(id = fieldId, name = "Talhão 7")))
        EntityNames.registerMachines(listOf(MachineDto(id = machineId, name = "Trator 9", status = "IDLE", type = "TRACTOR")))

        assertEquals("Fazenda Rio Claro", DomainLabels.label(farmId))
        assertEquals("Talhão 7", DomainLabels.label(fieldId))
        assertEquals("Trator 9", DomainLabels.label(machineId))
    }

    @Test
    fun blankNamesAreNotRegistered() {
        val fieldId = "44444444-4444-4444-4444-444444444444"
        EntityNames.registerFields(listOf(FieldDto(id = fieldId, name = "  ")))
        assertEquals("44444444", DomainLabels.label(fieldId))
    }

    @Test
    fun backendCodesAreLocalized() {
        LocaleStore.setLocale(AppLocale.PT_BR)
        assertEquals("Troca de filtros", DomainLabels.label("FILTER_CHANGE"))
        assertEquals("Pivô Norte", DomainLabels.label("PIVOT_NORTH"))
        assertEquals("Déficit hídrico estimado", DomainLabels.label("WATER_DEFICIT"))
        assertEquals("Óleo hidráulico", DomainLabels.label("HYDRAULIC_OIL"))
        assertEquals("Drone", DomainLabels.label("DRONE"))
        assertEquals("Ocioso", DomainLabels.label("IDLE"))
        LocaleStore.setLocale(AppLocale.EN_US)
        assertEquals("DemoJohnDeere", DomainLabels.label("DemoJohnDeere"))
        assertEquals("listMachines", DomainLabels.label("listMachines"))
        assertEquals("North Pivot", DomainLabels.label("PIVOT_NORTH"))
        assertEquals("Idle", DomainLabels.label("IDLE"))
    }
}
