package com.precisionfarming.farm.api

import com.precisionfarming.farm.application.FarmService
import com.precisionfarming.farm.application.UpsertFarm
import com.precisionfarming.farm.application.UpsertField
import com.precisionfarming.farm.application.UpsertSeason
import com.precisionfarming.security.FarmAccess
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
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
class FarmController(
    private val farmService: FarmService,
    private val farmAccess: FarmAccess,
) {
    @GetMapping("/farms")
    fun farms() = farmService.listFarms(farmAccess.current())

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/farms")
    fun createFarm(@Valid @RequestBody body: FarmRequest) =
        farmService.createFarm(farmAccess.current(), UpsertFarm(body.name, body.location, body.areaHa, body.timezone))

    @GetMapping("/farms/{id}")
    fun farm(@PathVariable id: UUID) = farmService.getFarm(farmAccess.current(), id)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PatchMapping("/farms/{id}")
    fun patchFarm(@PathVariable id: UUID, @Valid @RequestBody body: FarmRequest) =
        farmService.patchFarm(farmAccess.current(), id, UpsertFarm(body.name, body.location, body.areaHa, body.timezone))

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @DeleteMapping("/farms/{id}")
    fun deleteFarm(@PathVariable id: UUID) = farmService.deleteFarm(farmAccess.current(), id)

    @GetMapping("/fields")
    fun fields(@RequestParam(required = false) farmId: UUID?) =
        farmService.listFields(farmAccess.current(), farmId)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PostMapping("/fields")
    fun createField(@Valid @RequestBody body: FieldRequest) =
        farmService.createField(
            farmAccess.current(),
            UpsertField(body.farmId, body.name, body.areaHa, body.crop, body.variety, body.geometry),
        )

    @GetMapping("/fields/{id}")
    fun field(@PathVariable id: UUID) = farmService.getField(farmAccess.current(), id)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PatchMapping("/fields/{id}")
    fun patchField(@PathVariable id: UUID, @Valid @RequestBody body: FieldRequest) =
        farmService.patchField(
            farmAccess.current(),
            id,
            UpsertField(body.farmId, body.name, body.areaHa, body.crop, body.variety, body.geometry),
        )

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @DeleteMapping("/fields/{id}")
    fun deleteField(@PathVariable id: UUID) = farmService.deleteField(farmAccess.current(), id)

    @GetMapping("/seasons")
    fun seasons(@RequestParam(required = false) farmId: UUID?) =
        farmService.listSeasons(farmAccess.current(), farmId)

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PostMapping("/seasons")
    fun createSeason(@Valid @RequestBody body: SeasonRequest) =
        farmService.createSeason(
            farmAccess.current(),
            UpsertSeason(body.farmId, body.name, body.crop, body.startDate, body.endDate, body.status),
        )

    @PreAuthorize("hasAnyRole('ADMIN','FARM_MANAGER')")
    @PatchMapping("/seasons/{id}")
    fun patchSeason(@PathVariable id: UUID, @Valid @RequestBody body: SeasonRequest) =
        farmService.patchSeason(
            farmAccess.current(),
            id,
            UpsertSeason(body.farmId, body.name, body.crop, body.startDate, body.endDate, body.status),
        )
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class FarmSeedController(private val farmService: FarmService) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    fun reset(): Map<String, String> {
        farmService.seed()
        return mapOf("status" to "seeded", "service" to "farm")
    }
}
