# 构建阶段：在仓库根目录上下文中，仅构建 hotel-pms-service 及其依赖的 inventory-kernel
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY inventory-kernel/pom.xml inventory-kernel/pom.xml
COPY hotel-pms-service/pom.xml hotel-pms-service/pom.xml

COPY inventory-kernel/src inventory-kernel/src
COPY hotel-pms-service/src hotel-pms-service/src

RUN mvn -B -pl hotel-pms-service -am -DskipTests package

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/hotel-pms-service/target/hotel-pms-service.jar app.jar

EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
