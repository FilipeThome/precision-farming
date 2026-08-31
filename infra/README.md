# AWS skeleton (P1)

Do not apply. Local Docker Compose (`docker-compose.yml`) runs PostGIS, TimescaleDB, RabbitMQ, Redis and MinIO on network `precision-farming`. Host port 8080 is reserved for the gateway process.

Future Terraform: ECS/Fargate, RDS PostGIS, optional Timescale, ElastiCache, Amazon MQ (or SQS behind a port), S3, IAM least privilege.
