package com.precisionfarming.mobile

import com.precisionfarming.mobile.data.EntityNames
import com.precisionfarming.mobile.data.FarmDto
import com.precisionfarming.mobile.data.FieldDto
import com.precisionfarming.mobile.data.MachineDto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityNamesTest {
    @After
    fun tearDown() = EntityNames.clear()

    @Test
    fun unknownIdIsNullUntilRegistered() {
        assertNull(EntityNames.nameOf(null))
        assertNull(EntityNames.nameOf("f1"))
        EntityNames.registerFarms(listOf(FarmDto(id = "f1", name = "Fazenda Rio Claro", location = "MT")))
        assertEquals("Fazenda Rio Claro", EntityNames.nameOf("f1"))
        assertNull(EntityNames.nameOf("f2"))
    }

    @Test
    fun registersFieldsAndMachinesAndIgnoresBlankNames() {
        EntityNames.registerFields(listOf(FieldDto(id = "field-1", name = "Talhão 7"), FieldDto(id = "field-blank", name = "  ")))
        EntityNames.registerMachines(listOf(MachineDto(id = "m1", name = "Trator 9", status = "IDLE", type = "TRACTOR"), MachineDto(id = "m-blank", name = "", status = "IDLE", type = "TRACTOR")))
        assertEquals("Talhão 7", EntityNames.nameOf("field-1"))
        assertNull(EntityNames.nameOf("field-blank"))
        assertEquals("Trator 9", EntityNames.nameOf("m1"))
        assertNull(EntityNames.nameOf("m-blank"))
    }

    @Test
    fun laterRegisterOverwritesAndClearDropsNames() {
        EntityNames.registerFarms(listOf(FarmDto(id = "f1", name = "A", location = "x")))
        assertEquals("A", EntityNames.nameOf("f1"))
        EntityNames.registerFarms(listOf(FarmDto(id = "f1", name = "B", location = "x")))
        assertEquals("B", EntityNames.nameOf("f1"))
        EntityNames.clear()
        assertNull(EntityNames.nameOf("f1"))
    }
}
