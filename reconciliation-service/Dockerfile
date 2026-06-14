# 多阶段构建：整库进上下文（根 pom 列全部模块，需所有模块 pom 可解析），仅用 -pl/-am 编译本服务及其依赖。
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl reconciliation-service -am -DskipTests package

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/reconciliation-service/target/reconciliation-service.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
