package com.precisionfarming.farm.api

import com.precisionfarming.farm.application.FarmService
import com.precisionfarming.farm.application.UpsertFarm
import com.precisionfarming.farm.application.UpsertField
import com.precisionfarming.farm.application.UpsertSeason
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class FarmRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val location: String,
    val areaHa: BigDecimal,
    val timezone: String = "America/Sao_Paulo",
)

data class FieldRequest(
    val farmId: UUID,
    @field:NotBlank val name: String,
    val areaHa: BigDecimal,
    @field:NotBlank val crop: String,
    val variety: String?,
    @field:NotBlank val geometry: String,
)

data class SeasonRequest(
    val farmId: UUID,
    @field:NotBlank val name: String,
    @field:NotBlank val crop: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    @field:NotBlank val status: String,
)

@RestController
@RequestMapping("/api/v1")
class FarmController(private val farmService: FarmService) {
    @GetMapping("/farms")
    fun farms() = farmService.listFarms()

    @PostMapping("/farms")
    fun createFarm(@Valid @RequestBody body: FarmRequest) =
        farmService.createFarm(UpsertFarm(body.name, body.location, body.areaHa, body.timezone))

    @GetMapping("/farms/{id}")
    fun farm(@PathVariable id: UUID) = farmService.getFarm(id)

    @PatchMapping("/farms/{id}")
    fun patchFarm(@PathVariable id: UUID, @Valid @RequestBody body: FarmRequest) =
        farmService.patchFarm(id, UpsertFarm(body.name, body.location, body.areaHa, body.timezone))

    @DeleteMapping("/farms/{id}")
    fun deleteFarm(@PathVariable id: UUID) = farmService.deleteFarm(id)

    @GetMapping("/fields")
    fun fields(@RequestParam(required = false) farmId: UUID?) = farmService.listFields(farmId)

    @PostMapping("/fields")
    fun createField(@Valid @RequestBody body: FieldRequest) =
        farmService.createField(UpsertField(body.farmId, body.name, body.areaHa, body.crop, body.variety, body.geometry))

    @GetMapping("/fields/{id}")
    fun field(@PathVariable id: UUID) = farmService.getField(id)

    @PatchMapping("/fields/{id}")
    fun patchField(@PathVariable id: UUID, @Valid @RequestBody body: FieldRequest) =
        farmService.patchField(id, UpsertField(body.farmId, body.name, body.areaHa, body.crop, body.variety, body.geometry))

    @DeleteMapping("/fields/{id}")
    fun deleteField(@PathVariable id: UUID) = farmService.deleteField(id)

    @GetMapping("/seasons")
    fun seasons(@RequestParam(required = false) farmId: UUID?) = farmService.listSeasons(farmId)

    @PostMapping("/seasons")
    fun createSeason(@Valid @RequestBody body: SeasonRequest) =
        farmService.createSeason(UpsertSeason(body.farmId, body.name, body.crop, body.startDate, body.endDate, body.status))

    @PatchMapping("/seasons/{id}")
    fun patchSeason(@PathVariable id: UUID, @Valid @RequestBody body: SeasonRequest) =
        farmService.patchSeason(id, UpsertSeason(body.farmId, body.name, body.crop, body.startDate, body.endDate, body.status))
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class FarmSeedController(private val farmService: FarmService) {
    @PostMapping("/reset")
    fun reset(): Map<String, String> {
        farmService.seed()
        return mapOf("status" to "seeded", "service" to "farm")
    }
}
