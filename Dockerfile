FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

ENV TZ=Asia/Seoul

RUN apt-get update && apt-get install -y tzdata tini && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    apt-get clean

COPY build/libs/*.jar app.jar

EXPOSE 8080

# tini를 PID 1로 두어 헬스체크 curl 등 자식 프로세스를 reap, 좀비 누적 방지
ENTRYPOINT ["/usr/bin/tini", "--", "java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]