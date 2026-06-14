# 构建阶段：在仓库根目录上下文中，仅构建 dining-pos-service 及其依赖的 inventory-kernel
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY inventory-kernel/pom.xml inventory-kernel/pom.xml
COPY dining-pos-service/pom.xml dining-pos-service/pom.xml

COPY inventory-kernel/src inventory-kernel/src
COPY dining-pos-service/src dining-pos-service/src

RUN mvn -B -pl dining-pos-service -am -DskipTests package

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/dining-pos-service/target/dining-pos-service.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
