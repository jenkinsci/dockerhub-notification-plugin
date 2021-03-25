# TODO

## Auto Hook

Currently the [Docker Hub API](https://docs.docker.com/docker-hub/repos/#webhooks) doesn't support adding web hooks. 
When support for that is added this plugin should be updated to *automagically* add the hook when configured.
 
## Hook origin Security

The plugin currently blindly accept a wel formed HTTP POST to the end point that contains expected data.
Some measures should be taken to be a bit more secure. Two examples that could both be implemented.
 
### Origin IP check

The [Docker Hub API](https://docs.docker.com/docker-hub/repos/#webhooks) documentation mentions an IP range that all their hooks are coming from.
Implement an IP range check that ignores HTTP POSTs from clients not in that range.
The ranges should be configurable by an admin in case of for example internal proxies.
Work has been started on the `from-trusted-ip` branch.

### Poll Docker Hub when a hook arrives to verify that something has actually happened.

This could be tricky due to the CDN that Docker Hub is using. At least from a user's perspective 
information isn't updated in the UI until minutes after the actual push and web hook post.
