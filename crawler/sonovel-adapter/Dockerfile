FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS build

ARG SONOVEL_COMMIT=76150dbd2827b4de97cde83cffb3ee367bc9be3a

RUN apt-get update \
    && apt-get install -y --no-install-recommends git maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /src
RUN git clone https://github.com/freeok/so-novel.git . \
    && git checkout "${SONOVEL_COMMIT}"

# The upstream application starts an unconditional client-reporting thread.
# The local adapter deliberately removes that call before compilation.
RUN sed -i '/new Thread(ClientReportRepository::report).start();/d' \
    src/main/java/com/pcdd/sonovel/Main.java

RUN mvn -P linux-x64 -DskipTests -Dlaunch4j.skip=true dependency:go-offline

COPY XmReaderBookServlet.java src/main/java/com/pcdd/sonovel/web/servlet/XmReaderBookServlet.java
RUN sed -i '/context.addServlet(SuggestionServlet.class, "\/suggestion");/a\        context.addServlet(XmReaderBookServlet.class, "/xmreader/book");' \
    src/main/java/com/pcdd/sonovel/web/WebServer.java

RUN mvn -P linux-x64 -DskipTests -Dlaunch4j.skip=true clean package

FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu

WORKDIR /sonovel
COPY --from=build /src/target/app-jar-with-dependencies.jar app.jar
COPY --from=build /src/bundle/rules rules
COPY --from=build /src/LICENSE LICENSE
COPY config.ini config.ini

EXPOSE 7765

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -jar app.jar"]
