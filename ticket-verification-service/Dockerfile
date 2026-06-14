# 多阶段构建：整库进上下文（根 pom 列出全部模块，需所有模块 pom 可解析），
# 仅用 -pl/-am 编译本服务及其依赖。.dockerignore 已排除 target/.git/docs。
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl ticket-verification-service -am -DskipTests package

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/ticket-verification-service/target/ticket-verification-service.jar app.jar

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
