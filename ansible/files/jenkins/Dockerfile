FROM jenkins/jenkins:lts

RUN jenkins-plugin-cli --version

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER root

RUN apt-get update && apt-get install -y docker.io

USER jenkins
