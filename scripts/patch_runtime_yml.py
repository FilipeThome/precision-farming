from pathlib import Path

ROOT = Path(r"f:/workspace/precision_farming/backend/services")

OLD_RABBIT = """  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: ${RABBIT_PORT:5672}
    username: ${RABBIT_USER:precision}
    password: ${RABBIT_PASSWORD:precision}
"""

NEW_RABBIT = """  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: ${RABBIT_PORT:5672}
    username: ${RABBIT_USER:precision}
    password: ${RABBIT_PASSWORD:precision}
    listener:
      simple:
        auto-startup: false
"""

for yml in ROOT.glob("*/src/main/resources/application.yml"):
    text = yml.read_text(encoding="utf-8")
    if "auto-startup:" not in text:
        text = text.replace(OLD_RABBIT, NEW_RABBIT)
    if "hibernate.jdbc.batch_size" not in text:
        text = text.replace(
            "      hibernate.jdbc.time_zone: UTC\n",
            "      hibernate.jdbc.time_zone: UTC\n"
            "      hibernate.jdbc.batch_size: 50\n"
            "      hibernate.order_inserts: true\n",
        )
    if "health:" not in text.split("management:", 1)[-1]:
        text = text.replace(
            "    include: health,info\n",
            "    include: health,info\n"
            "  health:\n"
            "    rabbit:\n"
            "      enabled: false\n",
        )
    yml.write_text(text, encoding="utf-8")
    print("patched", yml.as_posix().split("/services/")[-1].split("/")[0])
print("done")
