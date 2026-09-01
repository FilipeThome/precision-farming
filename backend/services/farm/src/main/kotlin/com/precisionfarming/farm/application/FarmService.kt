package com.precisionfarming.farm.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.DomainException
import com.precisionfarming.common.NotFoundException
import com.precisionfarming.common.concurrency.VirtualJobs
import com.precisionfarming.farm.infrastructure.FarmEntity
import com.precisionfarming.farm.infrastructure.FarmJpaRepository
import com.precisionfarming.farm.infrastructure.FieldEntity
import com.precisionfarming.farm.infrastructure.FieldJpaRepository
import com.precisionfarming.farm.infrastructure.SeasonEntity
import com.precisionfarming.farm.infrastructure.SeasonJpaRepository
import com.precisionfarming.security.AccessScope
import com.precisionfarming.security.DemoFarmDirectory
import com.precisionfarming.security.DemoFieldFarms
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Callable

data class FarmDto(val id: UUID, val name: String, val location: String, val areaHa: BigDecimal, val timezone: String)
data class FieldDto(
    val id: UUID,
    val farmId: UUID,
    val name: String,
    val areaHa: BigDecimal,
    val crop: String,
    val variety: String?,
    val geometry: String,
)
data class UpsertFarm(val name: String, val location: String, val areaHa: BigDecimal, val timezone: String)
data class UpsertField(
    val farmId: UUID,
    val name: String,
    val areaHa: BigDecimal,
    val crop: String,
    val variety: String?,
    val geometry: String,
)
data class SeasonDto(
    val id: UUID,
    val farmId: UUID,
    val name: String,
    val crop: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: String,
)
data class UpsertSeason(
    val farmId: UUID,
    val name: String,
    val crop: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: String,
)

@Service
class FarmService(
    private val farms: FarmJpaRepository,
    private val fields: FieldJpaRepository,
    private val seasons: SeasonJpaRepository,
) {
    private val gf = GeometryFactory(PrecisionModel(), 4326)
    private val wktReader = WKTReader(gf)
    private val json = ObjectMapper()

    fun listFarms(scope: AccessScope) = farms.findAllById(scope.farmIds).map { it.toDto() }

    fun getFarm(scope: AccessScope, id: UUID): FarmDto {
        scope.requireFarm(id)
        return farms.findById(id).orElseThrow { NotFoundException("FARM_NOT_FOUND", "Farm not found") }.toDto()
    }

    @Transactional
    fun createFarm(cmd: UpsertFarm): FarmDto {
        val entity = FarmEntity(UUID.randomUUID(), cmd.name, cmd.location, cmd.areaHa, cmd.timezone)
        val saved = farms.save(entity).toDto()
        DemoFarmDirectory.register(saved.id)
        return saved
    }

    @Transactional
    fun patchFarm(scope: AccessScope, id: UUID, cmd: UpsertFarm): FarmDto {
        scope.requireFarm(id)
        val e = farms.findById(id).orElseThrow { NotFoundException("FARM_NOT_FOUND", "Farm not found") }
        e.name = cmd.name
        e.location = cmd.location
        e.areaHa = cmd.areaHa
        e.timezone = cmd.timezone
        return farms.save(e).toDto()
    }

    @Transactional
    fun deleteFarm(scope: AccessScope, id: UUID) {
        scope.requireFarm(id)
        if (!farms.existsById(id)) throw NotFoundException("FARM_NOT_FOUND", "Farm not found")
        seasons.deleteByFarmId(id)
        fields.deleteByFarmId(id)
        farms.deleteById(id)
        DemoFarmDirectory.unregister(id)
    }

    fun listFields(scope: AccessScope, farmId: UUID?) =
        fields.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    fun getField(scope: AccessScope, id: UUID): FieldDto {
        val e = fields.findById(id).orElseThrow { NotFoundException("FIELD_NOT_FOUND", "Field not found") }
        scope.requireEntityFarm(e.farmId)
        return e.toDto()
    }

    @Transactional
    fun createField(scope: AccessScope, cmd: UpsertField): FieldDto {
        scope.requireFarm(cmd.farmId)
        if (!farms.existsById(cmd.farmId)) throw NotFoundException("FARM_NOT_FOUND", "Farm not found")
        val geom = parseMulti(cmd.geometry)
        val entity = FieldEntity(UUID.randomUUID(), cmd.farmId, cmd.name, cmd.areaHa, cmd.crop, cmd.variety, geom, geom.centroid)
        val saved = fields.save(entity).toDto()
        DemoFieldFarms.register(saved.id, saved.farmId)
        return saved
    }

    @Transactional
    fun patchField(scope: AccessScope, id: UUID, cmd: UpsertField): FieldDto {
        val e = fields.findById(id).orElseThrow { NotFoundException("FIELD_NOT_FOUND", "Field not found") }
        scope.requireEntityFarm(e.farmId)
        scope.requireFarm(cmd.farmId)
        val geom = parseMulti(cmd.geometry)
        e.farmId = cmd.farmId
        e.name = cmd.name
        e.areaHa = cmd.areaHa
        e.crop = cmd.crop
        e.variety = cmd.variety
        e.geometry = geom
        e.centroid = geom.centroid
        return fields.save(e).toDto()
    }

    @Transactional
    fun deleteField(scope: AccessScope, id: UUID) {
        val e = fields.findById(id).orElseThrow { NotFoundException("FIELD_NOT_FOUND", "Field not found") }
        scope.requireEntityFarm(e.farmId)
        fields.deleteById(id)
    }

    fun listSeasons(scope: AccessScope, farmId: UUID?) =
        seasons.findByFarmIdIn(scope.resolveFarms(farmId)).map { it.toDto() }

    @Transactional
    fun createSeason(scope: AccessScope, cmd: UpsertSeason): SeasonDto {
        scope.requireFarm(cmd.farmId)
        if (!farms.existsById(cmd.farmId)) throw NotFoundException("FARM_NOT_FOUND", "Farm not found")
        return seasons.save(
            SeasonEntity(UUID.randomUUID(), cmd.farmId, cmd.name, cmd.crop, cmd.startDate, cmd.endDate, cmd.status),
        ).toDto()
    }

    @Transactional
    fun patchSeason(scope: AccessScope, id: UUID, cmd: UpsertSeason): SeasonDto {
        val e = seasons.findById(id).orElseThrow { NotFoundException("SEASON_NOT_FOUND", "Season not found") }
        scope.requireEntityFarm(e.farmId)
        scope.requireFarm(cmd.farmId)
        e.farmId = cmd.farmId
        e.name = cmd.name
        e.crop = cmd.crop
        e.startDate = cmd.startDate
        e.endDate = cmd.endDate
        e.status = cmd.status
        return seasons.save(e).toDto()
    }

    @Transactional
    fun seed() {
        data class FarmSeed(val key: String, val name: String, val location: String, val area: String)
        val farmSeeds = listOf(
            FarmSeed("farm-001", "Fazenda Boa Vista", "São Gabriel do Oeste - MS", "1250"),
            FarmSeed("farm-002", "Fazenda Santa Helena", "Rio Verde - GO", "980"),
            FarmSeed("farm-003", "Fazenda Horizonte", "Sorriso - MT", "2450"),
            FarmSeed("farm-004", "Fazenda Primavera", "Lucas do Rio Verde - MT", "1650"),
            FarmSeed("farm-005", "Fazenda Campo Alegre", "Dourados - MS", "870"),
            FarmSeed("farm-006", "Fazenda Vale Verde", "Campo Novo do Parecis - MT", "1420"),
            FarmSeed("farm-007", "Fazenda Estrela do Sul", "Pedra Preta - MT", "1180"),
            FarmSeed("farm-008", "Fazenda Nova Esperança", "Chapadão do Sul - MS", "960"),
        )
        val existingFarms = farms.findAllById(farmSeeds.map { DemoIds.uuid(it.key) }).map { it.id }.toHashSet()
        farms.saveAll(
            farmSeeds.filter { DemoIds.uuid(it.key) !in existingFarms }.map { s ->
                FarmEntity(DemoIds.uuid(s.key), s.name, s.location, BigDecimal(s.area), "America/Campo_Grande")
            },
        )
        data class FieldSeed(val key: String, val farm: String, val name: String, val area: String, val crop: String, val lng: Double, val lat: Double)
        val fieldSeeds = listOf(
            FieldSeed("field-001", "farm-001", "Talhão 01", "120.5", "Soja", -54.57, -19.39),
            FieldSeed("field-002", "farm-001", "Talhão 02", "95.0", "Milho", -54.55, -19.41),
            FieldSeed("field-003", "farm-001", "Talhão 03", "80.0", "Soja", -54.59, -19.37),
            FieldSeed("field-004", "farm-002", "Talhão Norte", "210.0", "Soja", -50.92, -17.79),
            FieldSeed("field-005", "farm-002", "Talhão Sul", "175.0", "Milho", -50.90, -17.81),
            FieldSeed("field-006", "farm-003", "Talhão A", "320.0", "Soja", -55.47, -12.54),
            FieldSeed("field-007", "farm-003", "Talhão B", "280.0", "Milho", -55.45, -12.56),
            FieldSeed("field-008", "farm-003", "Talhão C", "190.0", "Algodão", -55.49, -12.52),
            FieldSeed("field-009", "farm-004", "Talhão Leste", "150.0", "Soja", -55.90, -13.05),
            FieldSeed("field-010", "farm-004", "Talhão Oeste", "140.0", "Milho", -55.92, -13.07),
            FieldSeed("field-011", "farm-004", "Talhão Centro", "110.0", "Soja", -55.91, -13.06),
            FieldSeed("field-012", "farm-005", "Talhão 1", "95.0", "Soja", -54.80, -22.22),
            FieldSeed("field-013", "farm-005", "Talhão 2", "88.0", "Milho", -54.82, -22.24),
            FieldSeed("field-014", "farm-001", "Talhão 04", "70.0", "Milho", -54.56, -19.38),
            FieldSeed("field-015", "farm-002", "Talhão Leste", "130.0", "Soja", -50.91, -17.80),
            FieldSeed("field-016", "farm-005", "Talhão 3", "102.0", "Soja", -54.81, -22.23),
            FieldSeed("field-017", "farm-006", "Talhão VV-01", "155.0", "Soja", -57.88, -13.68),
            FieldSeed("field-018", "farm-006", "Talhão VV-02", "140.0", "Milho", -57.86, -13.70),
            FieldSeed("field-019", "farm-007", "Talhão ES-Norte", "125.0", "Soja", -54.10, -16.62),
            FieldSeed("field-020", "farm-007", "Talhão ES-Sul", "118.0", "Algodão", -54.08, -16.64),
            FieldSeed("field-021", "farm-008", "Talhão NE-01", "105.0", "Soja", -52.62, -18.79),
            FieldSeed("field-022", "farm-008", "Talhão NE-02", "98.0", "Milho", -52.60, -18.81),
        )
        val existingFields = fields.findAllById(fieldSeeds.map { DemoIds.uuid(it.key) }).map { it.id }.toHashSet()
        val missing = fieldSeeds.filter { DemoIds.uuid(it.key) !in existingFields }
        if (missing.isNotEmpty()) {
            val generated = VirtualJobs.all(
                missing.map { s ->
                    Callable {
                        val poly = rectangle(s.lng, s.lat, 0.04)
                        FieldEntity(
                            DemoIds.uuid(s.key), DemoIds.uuid(s.farm), s.name, BigDecimal(s.area), s.crop, "Demo",
                            poly, poly.centroid,
                        )
                    }
                },
            )
            fields.saveAll(generated)
        }
        data class SeasonSeed(
            val key: String, val farm: String, val name: String, val crop: String,
            val start: LocalDate, val end: LocalDate, val status: String,
        )
        val seasonSeeds = listOf(
            SeasonSeed("season-001", "farm-001", "Safra 2025/26", "Soja", LocalDate.of(2025, 9, 15), LocalDate.of(2026, 3, 30), "ACTIVE"),
            SeasonSeed("season-002", "farm-001", "Safrinha 2026", "Milho", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 7, 15), "PLANNED"),
            SeasonSeed("season-003", "farm-002", "Safra 2025/26", "Soja", LocalDate.of(2025, 9, 20), LocalDate.of(2026, 3, 25), "ACTIVE"),
            SeasonSeed("season-004", "farm-003", "Safra 2025/26", "Soja", LocalDate.of(2025, 9, 10), LocalDate.of(2026, 3, 20), "ACTIVE"),
            SeasonSeed("season-005", "farm-004", "Safra 2025/26", "Milho", LocalDate.of(2025, 10, 1), LocalDate.of(2026, 4, 15), "ACTIVE"),
            SeasonSeed("season-006", "farm-005", "Safra 2025/26", "Soja", LocalDate.of(2025, 9, 25), LocalDate.of(2026, 3, 28), "ACTIVE"),
            SeasonSeed("season-007", "farm-006", "Safra 2025/26", "Soja", LocalDate.of(2025, 9, 12), LocalDate.of(2026, 3, 22), "ACTIVE"),
            SeasonSeed("season-008", "farm-007", "Safra 2025/26", "Algodão", LocalDate.of(2025, 10, 5), LocalDate.of(2026, 5, 30), "ACTIVE"),
            SeasonSeed("season-009", "farm-008", "Safra 2025/26", "Soja", LocalDate.of(2025, 9, 18), LocalDate.of(2026, 3, 26), "ACTIVE"),
            SeasonSeed("season-010", "farm-002", "Safrinha 2026", "Milho", LocalDate.of(2026, 2, 10), LocalDate.of(2026, 7, 20), "PLANNED"),
        )
        val existingSeasons = seasons.findAllById(seasonSeeds.map { DemoIds.uuid(it.key) }).map { it.id }.toHashSet()
        seasons.saveAll(
            seasonSeeds.filter { DemoIds.uuid(it.key) !in existingSeasons }.map { s ->
                SeasonEntity(DemoIds.uuid(s.key), DemoIds.uuid(s.farm), s.name, s.crop, s.start, s.end, s.status)
            },
        )
    }

    private fun parseMulti(raw: String): Geometry {
        val text = raw.trim()
        val parsed = try {
            if (text.startsWith("{")) parseGeoJson(text) else wktReader.read(text)
        } catch (ex: DomainException) {
            throw ex
        } catch (ex: Exception) {
            throw DomainException("INVALID_GEOMETRY", ex.message ?: "Invalid geometry")
        }
        val geom = toMultiPolygon(parsed)
        geom.srid = 4326
        return geom
    }

    private fun parseGeoJson(text: String): Geometry {
        val node = json.readTree(text)
        val type = node.path("type").asText()
        val coordinates = node.get("coordinates")
            ?: throw DomainException("INVALID_GEOMETRY", "GeoJSON coordinates are required")
        return when (type) {
            "Polygon" -> polygonFromRings(coordinates)
            "MultiPolygon" -> {
                if (!coordinates.isArray || coordinates.isEmpty) {
                    throw DomainException("INVALID_GEOMETRY", "MultiPolygon coordinates are empty")
                }
                gf.createMultiPolygon(coordinates.map { polygonFromRings(it) }.toTypedArray())
            }
            else -> throw DomainException("INVALID_GEOMETRY", "Expected Polygon or MultiPolygon")
        }
    }

    private fun polygonFromRings(rings: JsonNode): Polygon {
        if (!rings.isArray || rings.isEmpty) {
            throw DomainException("INVALID_GEOMETRY", "Polygon rings are required")
        }
        val parsed = rings.map { ringToLinearRing(it) }
        return gf.createPolygon(parsed.first(), parsed.drop(1).toTypedArray())
    }

    private fun ringToLinearRing(ring: JsonNode): LinearRing {
        if (!ring.isArray || ring.size() < 4) {
            throw DomainException("INVALID_GEOMETRY", "A linear ring needs at least 4 positions")
        }
        val points = ring.map { pos ->
            if (!pos.isArray || pos.size() < 2) {
                throw DomainException("INVALID_GEOMETRY", "Each position needs longitude and latitude")
            }
            Coordinate(pos[0].asDouble(), pos[1].asDouble())
        }.toMutableList()
        if (points.first().x != points.last().x || points.first().y != points.last().y) {
            points += Coordinate(points.first())
        }
        return gf.createLinearRing(points.toTypedArray())
    }

    private fun toMultiPolygon(geom: Geometry): Geometry = when (geom) {
        is MultiPolygon -> geom
        is Polygon -> gf.createMultiPolygon(arrayOf(geom))
        else -> throw DomainException("INVALID_GEOMETRY", "Expected Polygon or MultiPolygon")
    }

    private fun rectangle(lng: Double, lat: Double, d: Double): Geometry {
        val shell = gf.createLinearRing(
            arrayOf(
                Coordinate(lng, lat),
                Coordinate(lng + d, lat),
                Coordinate(lng + d, lat + d),
                Coordinate(lng, lat + d),
                Coordinate(lng, lat),
            ),
        )
        val geom: Geometry = gf.createMultiPolygon(arrayOf(gf.createPolygon(shell)))
        geom.srid = 4326
        return geom
    }

    private fun toGeoJson(geometry: Geometry): String {
        val polygons = when (geometry) {
            is MultiPolygon -> (0 until geometry.numGeometries).map { geometry.getGeometryN(it) as Polygon }
            is Polygon -> listOf(geometry)
            else -> throw DomainException("INVALID_GEOMETRY", "Expected Polygon or MultiPolygon")
        }
        val coords = polygons.joinToString(",") { polygon ->
            val rings = buildList {
                add(polygon.exteriorRing)
                for (i in 0 until polygon.numInteriorRing) add(polygon.getInteriorRingN(i))
            }
            val encoded = rings.joinToString(",") { ring ->
                ring.coordinates.joinToString(",", "[", "]") { "[${it.x},${it.y}]" }
            }
            "[$encoded]"
        }
        return """{"type":"MultiPolygon","coordinates":[$coords]}"""
    }

    private fun FarmEntity.toDto() = FarmDto(id, name, location, areaHa, timezone)
    private fun FieldEntity.toDto() = FieldDto(id, farmId, name, areaHa, crop, variety, toGeoJson(geometry))
    private fun SeasonEntity.toDto() = SeasonDto(id, farmId, name, crop, startDate, endDate, status)
}

@Service
class FarmSeedRunner(
    private val farmService: FarmService,
    @Value("\${app.seed:true}") private val seed: Boolean,
) {
    @Bean
    fun seedFarms() = ApplicationRunner { if (seed) farmService.seed() }
}
