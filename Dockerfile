FROM sbtscala/scala-sbt:eclipse-temurin-21.0.6_7_1.10.10_3.6.4 AS builder

WORKDIR /app
COPY . .

RUN sbt stage

FROM eclipse-temurin:21-jre

COPY --from=builder /app/target/universal/stage /app

WORKDIR /app

CMD ["./bin/gotrip-backend"]