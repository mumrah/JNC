FROM openjdk:10-jre-slim

COPY build/distributions/java-node-controller.tar /java-node-controller.tar

RUN tar xvf java-node-controller.tar && cd java-node-controller

CMD ["./bin/java-node-controller", "conf/sample.ini"]
