# Java 后端运行镜像：内置 ffmpeg，用于视频截封面与转码（uploadVideo / VideoProcessingService）
# Zeabur：将「服务根目录」设为 MyBlog-java，或把本 Dockerfile 指到该路径。
# 构建：docker build -t myblog-java .
# 运行：需配合环境变量 MYSQL_*、REDIS_*、PORT 等（与 application.properties 一致）

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package \
    && mv target/MyBlog-*.jar target/app.jar

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && ffmpeg -version

WORKDIR /app
COPY --from=build /src/target/app.jar app.jar

EXPOSE 8080
ENV PORT=8080

# Zeabur / PaaS 常通过 JAVA_TOOL_OPTIONS 注入 -Xmx；此处仅兜底
ENV JAVA_OPTS="-XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -jar /app/app.jar"]
