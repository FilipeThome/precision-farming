package com.precisionfarming.security

import com.precisionfarming.common.DemoIds
import com.precisionfarming.common.PemKeys
import com.precisionfarming.common.UnauthorizedException
import com.precisionfarming.security.issuer.JwtIssuer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.UUID

class JwtAndFarmAccessTest {
    private val farmAccess = FarmAccess()
    private val props = rsaProps()
    private val jwtIssuer = JwtIssuer(props)
    private val accessDecoder: JwtDecoder = run {
        val nimbus = NimbusJwtDecoder.withPublicKey(PemKeys.parsePublic(props.jwtPublicKey)).build()
        JwtDecoder { token ->
            val jwt = nimbus.decode(token)
            JwtAccessType.requireAccess(jwt.getClaimAsString(JwtAccessType.CLAIM))
            jwt
        }
    }
    private val resourceDecoder: JwtDecoder = run {
        val nimbus = NimbusJwtDecoder.withPublicKey(PemKeys.parsePublic(props.jwtPublicKey)).build()
        JwtDecoder { token ->
            val jwt = nimbus.decode(token)
            JwtAccessType.requireResourceToken(jwt.getClaimAsString(JwtAccessType.CLAIM))
            jwt
        }
    }

    @Test
    fun requireAccessRejectsRefreshAndMissing() {
        assertThrows(JwtException::class.java) { JwtAccessType.requireAccess(JwtAccessType.REFRESH) }
        assertThrows(JwtException::class.java) { JwtAccessType.requireAccess(null) }
        JwtAccessType.requireAccess(JwtAccessType.ACCESS)
    }

    @Test
    fun requireResourceTokenAcceptsAccessAndService() {
        JwtAccessType.requireResourceToken(JwtAccessType.ACCESS)
        JwtAccessType.requireResourceToken(JwtAccessType.SERVICE)
        assertThrows(JwtException::class.java) { JwtAccessType.requireResourceToken(JwtAccessType.REFRESH) }
    }

    @Test
    fun decoderRejectsRefreshToken() {
        val refresh = jwtIssuer.createRefreshToken(UUID.randomUUID()).token
        assertThrows(JwtException::class.java) { accessDecoder.decode(refresh) }
        assertThrows(JwtException::class.java) { resourceDecoder.decode(refresh) }
    }

    @Test
    fun decoderAcceptsAccessToken() {
        val farm = DemoIds.uuid("farm-001")
        val token = jwtIssuer.createAccessToken(
            UUID.randomUUID(),
            "manager@precisionfarming.demo",
            "FARM_MANAGER",
            listOf(farm),
        )
        val jwt = accessDecoder.decode(token)
        assertEquals(JwtAccessType.ACCESS, jwt.getClaimAsString(JwtAccessType.CLAIM))
    }

    @Test
    fun resourceDecoderAcceptsServiceToken() {
        val farm = DemoIds.uuid("farm-001")
        val token = jwtIssuer.createServiceToken(DemoIds.uuid("svc-operation"), listOf(farm))
        val jwt = resourceDecoder.decode(token)
        assertEquals(JwtAccessType.SERVICE, jwt.getClaimAsString(JwtAccessType.CLAIM))
        assertEquals("SERVICE", jwt.getClaimAsString("role"))
        assertThrows(JwtException::class.java) { accessDecoder.decode(token) }
    }

    @Test
    fun accessTokenIncludesTypeAccessAndFarmIds() {
        val userId = UUID.randomUUID()
        val farms = DemoFarmDirectory.forRole("FARM_MANAGER")
        val token = jwtIssuer.createAccessToken(userId, "manager@precisionfarming.demo", "FARM_MANAGER", farmIds = farms)
        val claims = jwtIssuer.parse(token)
        assertEquals("access", claims["type"])
        assertEquals(userId.toString(), claims.subject)
    }

    @Test
    fun demoFarmDirectoryScopesMatchRoles() {
        assertEquals(8, DemoFarmDirectory.ALL.size)
        assertTrue(DemoFarmDirectory.forRole("ADMIN").containsAll(DemoFarmDirectory.ALL))
        assertTrue(
            DemoFarmDirectory.forRole("FARM_MANAGER").containsAll(
                setOf(DemoIds.uuid("farm-001"), DemoIds.uuid("farm-002"), DemoIds.uuid("farm-003")),
            ),
        )
        assertTrue(
            DemoFarmDirectory.forRole("MAINTENANCE").containsAll(
                setOf(DemoIds.uuid("farm-001"), DemoIds.uuid("farm-002"), DemoIds.uuid("farm-003")),
            ),
        )
        assertEquals(setOf(DemoIds.uuid("farm-001")), DemoFarmDirectory.forRole("OPERATOR"))
    }

    @Test
    fun runtimeRegistrationsExtendSeedMapsWithoutReplacingThem() {
        val field = UUID.randomUUID()
        val machine = UUID.randomUUID()
        val item = UUID.randomUUID()
        val farm = DemoIds.uuid("farm-001")
        DemoFieldFarms.register(field, farm)
        DemoMachineFarms.register(machine, farm)
        DemoItemFarms.register(item, farm)
        DemoFieldFarms.requireBelongsToFarm(field, farm)
        DemoMachineFarms.requireBelongsToFarm(machine, farm)
        DemoItemFarms.requireBelongsToFarm(item, farm)
        assertEquals(DemoIds.uuid("farm-001"), DemoFieldFarms.farmId(DemoIds.uuid("field-001")))
    }

    @Test
    fun jwtFarmIdsAreNotExpandedByRuntimeDirectory() {
        val created = UUID.randomUUID()
        DemoFarmDirectory.register(created)
        try {
            val scope = farmAccess.fromJwt(
                jwt(
                    farmIds = listOf(DemoIds.uuid("farm-001").toString()),
                    subject = UUID.randomUUID().toString(),
                    role = "ADMIN",
                ),
            )
            assertFalse(scope.farmIds.contains(created))
            assertEquals(setOf(DemoIds.uuid("farm-001")), scope.farmIds)
        } finally {
            DemoFarmDirectory.unregister(created)
        }
    }

    @Test
    fun demoScopeMapsCoverDensifiedIds() {
        assertEquals(DemoIds.uuid("farm-008"), DemoFieldFarms.farmId(DemoIds.uuid("field-022")))
        assertEquals(DemoIds.uuid("farm-008"), DemoMachineFarms.farmId(DemoIds.uuid("machine-012")))
        assertEquals(DemoIds.uuid("farm-003"), DemoMachineFarms.farmId(DemoIds.uuid("machine-013")))
        assertEquals(DemoIds.uuid("farm-006"), DemoMachineFarms.farmId(DemoIds.uuid("machine-014")))
        assertEquals(DemoIds.uuid("farm-008"), DemoItemFarms.farmId(DemoIds.uuid("item-016")))
        DemoFieldFarms.requireBelongsToFarm(DemoIds.uuid("field-014"), DemoIds.uuid("farm-001"))
        DemoItemFarms.requireBelongsToFarm(DemoIds.uuid("item-001"), DemoIds.uuid("farm-001"))
    }

    @Test
    fun emptyFarmIdsFailsClosed() {
        val jwt = jwt(farmIds = emptyList(), subject = UUID.randomUUID().toString())
        val ex = assertThrows(UnauthorizedException::class.java) { farmAccess.fromJwt(jwt) }
        assertEquals("UNAUTHORIZED", ex.code)
        assertEquals("Missing farmIds claim", ex.message)
    }

    @Test
    fun userFarmGrantsJoinJwtScopeForThatUserOnly() {
        val existing = DemoIds.uuid("farm-001")
        val created = UUID.randomUUID()
        val owner = UUID.randomUUID()
        val other = UUID.randomUUID()
        UserFarmGrants.grant(owner, created)
        try {
            val ownerScope = farmAccess.fromJwt(
                jwt(farmIds = listOf(existing.toString()), subject = owner.toString(), role = "ADMIN"),
            )
            assertEquals(setOf(existing, created), ownerScope.farmIds)
            val otherScope = farmAccess.fromJwt(
                jwt(farmIds = listOf(existing.toString()), subject = other.toString(), role = "ADMIN"),
            )
            assertEquals(setOf(existing), otherScope.farmIds)
        } finally {
            UserFarmGrants.revoke(created, owner)
        }
    }

    @Test
    fun blankTenantDefaultsToDemo() {
        val farm = DemoIds.uuid("farm-001")
        val scope = farmAccess.fromJwt(
            jwt(farmIds = listOf(farm.toString()), subject = UUID.randomUUID().toString(), tenantId = ""),
        )
        assertEquals(DemoTenant.ID, scope.tenantId)
    }

    @Test
    fun foreignTenantIsRejected() {
        val farm = DemoIds.uuid("farm-001")
        assertThrows(UnauthorizedException::class.java) {
            farmAccess.fromJwt(
                jwt(
                    farmIds = listOf(farm.toString()),
                    subject = UUID.randomUUID().toString(),
                    tenantId = UUID.randomUUID().toString(),
                ),
            )
        }
    }

    @Test
    fun invalidTenantIsRejected() {
        val farm = DemoIds.uuid("farm-001")
        assertThrows(UnauthorizedException::class.java) {
            farmAccess.fromJwt(
                jwt(farmIds = listOf(farm.toString()), subject = UUID.randomUUID().toString(), tenantId = "not-a-uuid"),
            )
        }
    }

    @Test
    fun claimedFarmIdsAreHonored() {
        val farm = DemoIds.uuid("farm-001")
        val userId = UUID.randomUUID()
        val scope = farmAccess.fromJwt(
            jwt(farmIds = listOf(farm.toString()), subject = userId.toString(), role = "OPERATOR"),
        )
        assertEquals(setOf(farm), scope.farmIds)
        assertEquals("OPERATOR", scope.role)
        assertEquals(userId, scope.userId)
    }

    @Test
    fun demoItemMustBelongToFarm() {
        val farm1 = DemoIds.uuid("farm-001")
        val farm2 = DemoIds.uuid("farm-002")
        DemoItemFarms.requireBelongsToFarm(DemoIds.uuid("item-001"), farm1)
        assertThrows(com.precisionfarming.common.ForbiddenException::class.java) {
            DemoItemFarms.requireBelongsToFarm(DemoIds.uuid("item-001"), farm2)
        }
    }

    @Test
    fun serviceTokenRequiresFarmIds() {
        assertThrows(IllegalArgumentException::class.java) {
            jwtIssuer.createServiceToken(UUID.randomUUID(), emptyList())
        }
    }

    private fun jwt(
        farmIds: List<String>,
        subject: String,
        role: String = "ADMIN",
        tenantId: String? = DemoTenant.ID.toString(),
    ): Jwt {
        val builder = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(subject)
            .claim("role", role)
            .claim("farmIds", farmIds)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
        if (tenantId != null) builder.claim("tenantId", tenantId)
        return builder.build()
    }

    companion object {
        fun rsaProps(): JwtProperties {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(2048)
            val pair = gen.generateKeyPair()
            val pub = pair.public as RSAPublicKey
            val priv = pair.private as RSAPrivateKey
            fun pem(type: String, der: ByteArray): String {
                val b64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der)
                return "-----BEGIN $type-----\n$b64\n-----END $type-----\n"
            }
            return JwtProperties(
                jwtPublicKey = pem("PUBLIC KEY", pub.encoded),
                jwtPrivateKey = pem("PRIVATE KEY", priv.encoded),
                allowDemoSecrets = true,
            )
        }
    }
}
