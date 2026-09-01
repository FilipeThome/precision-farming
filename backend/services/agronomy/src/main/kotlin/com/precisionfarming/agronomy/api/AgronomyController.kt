package com.precisionfarming.agronomy.api

import com.precisionfarming.agronomy.application.AgronomyService
import com.precisionfarming.agronomy.application.CreatePrescription
import com.precisionfarming.agronomy.application.CreateScouting
import com.precisionfarming.agronomy.application.CreateSoilSample
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AgronomyController(private val svc: AgronomyService) {
    @GetMapping("/scouting")
    fun listScouting(@RequestParam(required = false) farmId: UUID?) = svc.listScouting(farmId)

    @PostMapping("/scouting")
    fun createScouting(@RequestBody body: CreateScouting) = svc.createScouting(body)

    @GetMapping("/soil/samples")
    fun listSoil(@RequestParam(required = false) farmId: UUID?) = svc.listSoil(farmId)

    @PostMapping("/soil/samples")
    fun createSoil(@RequestBody body: CreateSoilSample) = svc.createSoil(body)

    @GetMapping("/recommendations")
    fun listRecommendations(@RequestParam(required = false) farmId: UUID?) = svc.listRecommendations(farmId)

    @GetMapping("/prescriptions")
    fun listPrescriptions(@RequestParam(required = false) farmId: UUID?) = svc.listPrescriptions(farmId)

    @PostMapping("/prescriptions")
    fun createPrescription(@RequestBody body: CreatePrescription) = svc.createPrescription(body)

    @PostMapping("/prescriptions/{id}/approve")
    fun approve(@PathVariable id: UUID) = svc.approvePrescription(id)
}

@RestController
@RequestMapping("/api/v1/dev/seed")
class AgronomySeedController(private val svc: AgronomyService) {
    @PostMapping("/reset")
    fun reset() = mapOf("status" to "seeded", "service" to "agronomy").also { svc.seed() }
}
