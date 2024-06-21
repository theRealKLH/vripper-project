FROM maven:3.9.6 AS build

RUN mkdir /build
COPY . /build

WORKDIR /build
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21 AS run

ARG VERSION
ENV VERSION=${VERSION:-5.8.0}
ENV JAR_FILE=vripper-web-${VERSION}.jar
ENV VRIPPER_DIR=/vripper

RUN mkdir ${VRIPPER_DIR}
COPY --from=build /build/vripper-web/target/${JAR_FILE} ${VRIPPER_DIR}
WORKDIR ${VRIPPER_DIR}
RUN mkdir downloads

EXPOSE 8080/tcp 

CMD java -Duser.dir=${VRIPPER_DIR}/base -Duser.home=${VRIPPER_DIR}/downloads -jar ${VRIPPER_DIR}/${JAR_FILE}
