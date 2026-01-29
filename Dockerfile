# Java 17（Spring Boot 推奨）
FROM eclipse-temurin:17-jdk

# 作業ディレクトリ
WORKDIR /app

# jar をコピー
COPY target/*.jar app.jar

# Render が渡す PORT を使う
ENV PORT=8080
EXPOSE 8080

# 起動コマンド
ENTRYPOINT ["java", "-jar", "app.jar"]
