#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BACKEND = ROOT / "backend"
PKG = "com.precisionfarming"

SERVICES = [
    ("auth", 8081, "auth_db"),
    ("farm", 8082, "farm_db"),
    ("asset", 8083, "asset_db"),
    ("telemetry", 8084, "telemetry_db"),
    ("weather", 8085, "weather_db"),
    ("operation", 8086, "operation_db"),
    ("inventory", 8087, "inventory_db"),
    ("alert", 8088, "alert_db"),
    ("ai", 8089, "ai_db"),
    ("notification", 8090, "notification_db"),
    ("file", 8091, "file_db"),
    ("reporting", 8092, "reporting_db"),
    ("sync", 8093, "sync_db"),
    ("integration", 8094, "integration_db"),
]


def w(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.lstrip("\n").rstrip() + "\n", encoding="utf-8")


def pascal(name: str) -> str:
    return "".join(p.title() for p in name.split("-"))


def service_build(name: str) -> str:
    extra = ""
    if name == "farm":
        extra += "    implementation(libs.hibernate.spatial)\n    implementation(libs.jts)\n"
    return f"""
plugins {{
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dep.mgmt)
}}

java {{
    toolchain {{ languageVersion.set(JavaLanguageVersion.of(21)) }}
}}

dependencies {{
    implementation(project(":backend:libs:common"))
    implementation(project(":backend:libs:security"))
    implementation(libs.spring.boot.web)
    implementation(libs.spring.boot.jpa)
    implementation(libs.spring.boot.validation)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.security)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.postgresql)
    implementation(libs.jackson.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc)
    implementation(libs.micrometer.prometheus)
    implementation(libs.spring.boot.amqp)
{extra}
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
}}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {{
    archiveBaseName.set("{name}-service")
}}

tasks.withType<Test> {{
    useJUnitPlatform()
}}
"""


def yml(name: str, port: int, db: str) -> str:
    db_port = "5433" if name == "telemetry" else "5432"
    return f"""
spring:
  application:
    name: {name}-service
  datasource:
    url: jdbc:postgresql://${{DB_HOST:localhost}}:${{DB_PORT:{db_port}}}/{db}
    username: ${{DB_USER:precision}}
    password: ${{DB_PASSWORD:precision}}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate.jdbc.time_zone: UTC
      hibernate.jdbc.batch_size: 50
      hibernate.order_inserts: true
  flyway:
    enabled: true
  rabbitmq:
    host: ${{RABBIT_HOST:localhost}}
    port: ${{RABBIT_PORT:5672}}
    username: ${{RABBIT_USER:precision}}
    password: ${{RABBIT_PASSWORD:precision}}
    listener:
      simple:
        auto-startup: false
  threads:
    virtual:
      enabled: true
server:
  port: {port}
management:
  endpoints:
    web:
      exposure:
        include: health,info
  health:
    rabbit:
      enabled: false
app:
  security:
    jwt-public-key: ${{JWT_PUBLIC_KEY:}}
    allow-demo-secrets: ${{ALLOW_DEMO_SECRETS:false}}
    issuer: precision-farming
  seed: ${{APP_SEED:true}}
  clients:
    farm: ${{FARM_URL:http://localhost:8082}}
    asset: ${{ASSET_URL:http://localhost:8083}}
    inventory: ${{INVENTORY_URL:http://localhost:8087}}
    operation: ${{OPERATION_URL:http://localhost:8086}}
    weather: ${{WEATHER_URL:http://localhost:8085}}
"""


SQL = {
    "auth": """
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL
);
""",
    "farm": """
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE TABLE farms (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    location VARCHAR(160) NOT NULL,
    area_ha NUMERIC(12,4) NOT NULL,
    timezone VARCHAR(64) NOT NULL
);
CREATE TABLE fields (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL REFERENCES farms(id),
    name VARCHAR(160) NOT NULL,
    area_ha NUMERIC(12,4) NOT NULL,
    crop VARCHAR(80) NOT NULL,
    variety VARCHAR(80),
    geometry geometry(MultiPolygon, 4326) NOT NULL,
    centroid geometry(Point, 4326)
);
CREATE INDEX IF NOT EXISTS idx_fields_farm_id ON fields (farm_id);
CREATE INDEX IF NOT EXISTS idx_fields_geometry ON fields USING GIST (geometry);
""",
    "asset": """
CREATE TABLE machines (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(80) NOT NULL,
    manufacturer VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_machines_farm_status ON machines (farm_id, status);
""",
    "telemetry": """
CREATE TABLE telemetry_observations (
    id UUID NOT NULL,
    machine_id UUID NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    speed_kmh DOUBLE PRECISION,
    rpm DOUBLE PRECISION,
    fuel_pct DOUBLE PRECISION,
    engine_temp_c DOUBLE PRECISION,
    PRIMARY KEY (id, observed_at)
);
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
    PERFORM create_hypertable('telemetry_observations', 'observed_at', if_not_exists => TRUE);
  END IF;
END$$;
CREATE INDEX IF NOT EXISTS idx_telemetry_machine_observed
    ON telemetry_observations (machine_id, observed_at);
""",
    "weather": """
CREATE TABLE weather_forecasts (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    forecast_at TIMESTAMPTZ NOT NULL,
    temperature_min NUMERIC(6,2),
    temperature_max NUMERIC(6,2),
    rain_mm NUMERIC(8,2),
    rain_probability NUMERIC(5,2),
    wind_kmh NUMERIC(6,2),
    humidity_pct NUMERIC(5,2),
    spraying_window VARCHAR(32),
    vintage VARCHAR(32)
);
CREATE INDEX IF NOT EXISTS idx_weather_farm_forecast
    ON weather_forecasts (farm_id, forecast_at);
""",
    "operation": """
CREATE TABLE operations (
    id UUID PRIMARY KEY,
    field_id UUID NOT NULL,
    farm_id UUID NOT NULL,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    planned_start TIMESTAMPTZ,
    planned_end TIMESTAMPTZ,
    actual_start TIMESTAMPTZ,
    actual_end TIMESTAMPTZ,
    machine_id UUID,
    pause_reason VARCHAR(255),
    item_id UUID,
    item_quantity NUMERIC(12,3)
);
CREATE TABLE saga_instances (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    type VARCHAR(80) NOT NULL,
    state VARCHAR(40) NOT NULL,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_operations_farm ON operations (farm_id);
CREATE INDEX IF NOT EXISTS idx_operations_status ON operations (status);
CREATE INDEX IF NOT EXISTS idx_saga_operation ON saga_instances (operation_id);
""",
    "inventory": """
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(14,3) NOT NULL,
    reserved NUMERIC(14,3) NOT NULL DEFAULT 0
);
CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES inventory_items(id),
    type VARCHAR(40) NOT NULL,
    quantity NUMERIC(14,3) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    reference VARCHAR(160)
);
CREATE INDEX IF NOT EXISTS idx_inventory_items_farm ON inventory_items (farm_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_item ON inventory_movements (item_id);
""",
    "alert": """
CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    farm_id UUID NOT NULL,
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_alerts_farm_status ON alerts (farm_id, status);
""",
    "ai": """
CREATE TABLE predictions (
    id UUID PRIMARY KEY,
    type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    score NUMERIC(6,4) NOT NULL,
    confidence NUMERIC(6,4) NOT NULL,
    model VARCHAR(80) NOT NULL,
    model_version VARCHAR(40) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    explanation TEXT NOT NULL,
    horizon_hours INT
);
CREATE INDEX IF NOT EXISTS idx_predictions_entity ON predictions (entity_id);
""",
    "notification": """
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id);
""",
    "file": """
CREATE TABLE files (
    id UUID PRIMARY KEY,
    farm_id UUID,
    field_id UUID,
    kind VARCHAR(40) NOT NULL,
    source VARCHAR(80) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    acquisition_at TIMESTAMPTZ,
    processing_version VARCHAR(40),
    quality VARCHAR(40)
);
CREATE INDEX IF NOT EXISTS idx_files_farm ON files (farm_id);
""",
    "reporting": """
CREATE TABLE report_jobs (
    id UUID PRIMARY KEY,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
""",
    "sync": """
CREATE TABLE sync_commands (
    id UUID PRIMARY KEY,
    device_id VARCHAR(80) NOT NULL,
    client_operation_id VARCHAR(80) NOT NULL UNIQUE,
    command_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sync_device_created ON sync_commands (device_id, created_at);
""",
    "integration": """
CREATE TABLE connectors (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(80) NOT NULL,
    mode VARCHAR(20) NOT NULL
);
""",
}


def app_kt(name: str) -> str:
    cls = f"{pascal(name)}Application"
    return f"""
package {PKG}.{name}

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["{PKG}"])
@EnableScheduling
class {cls}

fun main(args: Array<String>) {{
    runApplication<{cls}>(*args)
}}
"""


def generic_test(name: str) -> str:
    return f"""
package {PKG}.{name}

import com.precisionfarming.common.DemoIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class {pascal(name)}DomainTest {{
    @Test
    fun demoIdsAreStable() {{
        assertEquals(DemoIds.uuid("{name}-001"), DemoIds.uuid("{name}-001"))
    }}
}}
"""


def main() -> None:
    for name, port, db in SERVICES:
        base = BACKEND / "services" / name
        w(base / "build.gradle.kts", service_build(name))
        w(base / f"src/main/kotlin/{PKG.replace('.', '/')}/{name}/{pascal(name)}Application.kt", app_kt(name))
        w(base / "src/main/resources/application.yml", yml(name, port, db))
        w(base / "src/main/resources/db/migration/V1__init.sql", SQL[name])
        w(base / f"src/test/kotlin/{PKG.replace('.', '/')}/{name}/{pascal(name)}DomainTest.kt", generic_test(name))
        w(base / "Dockerfile", f"""
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/{name}-service-*.jar app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
""")
    print("generated", len(SERVICES), "services")


if __name__ == "__main__":
    main()
