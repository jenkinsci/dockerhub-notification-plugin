CloudBees Docker Hub Notification
================

This plugin provides integration between Jenkins and Docker Hub, utilizing a dockerhub hook to trigger one (or more) Jenkins job(s).
This allows you to implement continuous delivery pipelines based on Docker in Jenkins.

Upon receiving a new image notification as web-hook from Docker Hub, jenkins will trigger all jobs that have the Docker Hub trigger
enabled and use the incoming Docker image as part of the Build.  A `DockerHub Pull` build step is provided to retrieve
the latest image from Hub.

# How to

Configure your Docker Hub repository with a webhook to your public jenkins instance `http://JENKINS/dockerhub-webhook/notify`

In your <a href="https://hub.docker.com/">hub.docker.com</a> repository, you can find the "webhooks" section and point it to your jenkins instance: 

<img src="dockerhub.png">

# Examples

Payloads submitted by the hub:

* [Payload from your own repository](src/test/resources/own-repository-payload.json).
* [Payload from a public repository](src/test/resources/public-repository-payload.json).

The plugin can be tested with

    curl -X POST -H "Content-Type: application/json" http://localhost:8080/jenkins/dockerhub-webhook/notify -d @src/test/resources/public-repository-payload.json
