.PHONY: infra seed test web
infra:
	docker compose up -d
test:
	./gradlew test
web:
	cd web && npm install && npm run dev
seed:
	./gradlew seedDemoData
