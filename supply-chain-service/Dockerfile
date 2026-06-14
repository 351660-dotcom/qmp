# 构建阶段：在仓库根目录上下文中，仅构建 supply-chain-service 及其依赖的 inventory-kernel
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY inventory-kernel/pom.xml inventory-kernel/pom.xml
COPY supply-chain-service/pom.xml supply-chain-service/pom.xml

COPY inventory-kernel/src inventory-kernel/src
COPY supply-chain-service/src supply-chain-service/src

RUN mvn -B -pl supply-chain-service -am -DskipTests package

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/supply-chain-service/target/supply-chain-service.jar app.jar

EXPOSE 8091
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
