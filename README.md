CloudBees Docker Hub/Registry Notification
================

This plugin provides integration between 
* Jenkins and Docker Hub
* Jenkins and Docker Registry 2.0

It does so by utilizing webhooks to trigger one (or more) Jenkins job(s).
This allows you to implement continuous delivery pipelines based on Docker in Jenkins.

When Jenkins receives a notification of an updated image that is a web-hook from
Docker Hub, it triggers all jobs that have the Docker Hub
trigger enabled and use the Docker image as part of the build. A
Docker Hub Pull build step is provided to retrieve the latest image from
Hub.

# Configuring Docker Hub

On the Jenkins Configure Global Security page add an api key.

<img src="apitoken.png">

Configure your Docker Hub repository with a webhook to your public jenkins instance `http://JENKINS/dockerhub-webhook/{api-key}/notify`

In your <a href="https://hub.docker.com/">hub.docker.com</a> repository, you can find the "webhooks" section and point it to your jenkins instance: 

<img src="dockerhub.png">

# Configuring Docker Registry 2.0

Follow Docker Registry 2.0 [documentation](https://docs.docker.com/registry/notifications/) on how to configure registry so that it would send notifications to `http://JENKINS/dockerregistry-webhook/{api-key}/notify`

The simplest viable configuration looks like this:
```
  notifications:
    endpoints:
      - name: jenkinslistener
        url: http://JENKINS/dockerregistry-webhook/{api-key}/notify
        timeout: 500ms
        threshold: 5
        backoff: 1s
```

# Configuring Azure Container Registry

You can find a detailed guide on how to configure webhooks on ACR on
[docs.microsoft.com](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-webhook).
Use `http://JENKINS/acr-webhook/{api-key}/notify` as a "Service URI".


# Examples

Payloads submitted by the hub:

* [Payload from your own repository](src/test/resources/own-repository-payload.json).
* [Payload from a public repository](src/test/resources/public-repository-payload.json).

Payloads submitted by the registry:

* [Payload from your own registry](/src/test/resources/private-registry-payload-1-repository.json).

The plugin can be tested with

```bash
    curl -X POST -H "Content-Type: application/json" http://localhost:8080/jenkins/dockerhub-webhook/{api-key}/notify -d @src/test/resources/public-repository-payload.json
```
