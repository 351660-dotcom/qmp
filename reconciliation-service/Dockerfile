# 构建阶段：在仓库根目录上下文中，仅构建 reconciliation-service 及其依赖的 inventory-kernel
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY inventory-kernel/pom.xml inventory-kernel/pom.xml
COPY reconciliation-service/pom.xml reconciliation-service/pom.xml

COPY inventory-kernel/src inventory-kernel/src
COPY reconciliation-service/src reconciliation-service/src

RUN mvn -B -pl reconciliation-service -am -DskipTests package

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/reconciliation-service/target/reconciliation-service.jar app.jar

EXPOSE 8093
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
