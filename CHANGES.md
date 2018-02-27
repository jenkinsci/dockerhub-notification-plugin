# Changelog

Only noting significant user-visible or major API changes, not internal code cleanups and minor bug fixes.

## 2.2.1 (Jan 04, 2018)

* [JENKINS-47736](https://issues.jenkins-ci.org/browse/JENKINS-47736) - 
Stop serializing `JSONObjects` over the Remoting to make the plugin compatible with Jenkins 2.102+
  * More info: [Plugins affected by fix for JEP-200](https://wiki.jenkins.io/display/JENKINS/Plugins+affected+by+fix+for+JEP-200)

## 2.2.0 (Jun 15, 2016)
* Expose tag and pusher in the build environment _([PR #15](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/15))_
* _Dev:_ [JENKINS-35629](https://issues.jenkins-ci.org/browse/JENKINS-35629) Convert to new plugin parent pom _([PR #14](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/14))_

## 2.1.0 (May 30, 2016)
* Fix for [SECURITY-170](https://issues.jenkins-ci.org/browse/SECURITY-170) by changing from adding parameters to the build to adding plain environment variables instead.
  _[PR #13](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/13)_

## 2.0.0 (March 24, 2016)

* [JENKINS-30931](https://issues.jenkins-ci.org/browse/JENKINS-30931) Added improved support for Docker Registry 2.0.
    _[PR #5](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/5),
    [PR #7](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/7),
    [PR #11](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/11)_
    _This resulted in significant refactoring and API changes in the plugin, hence the bump of major version. Data from older versions of the plugin should migrate correctly._
* Substitute Environment Variables into Image Name.
  _[PR #8](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/8)_
* Added CSRF protection exclusions on the web hooks so that the plugin behaves correctly in a more secured Jenkins.
  _[PR #12](https://github.com/jenkinsci/dockerhub-notification-plugin/pull/12)_

## 1.0.2 (June 05, 2015)

* The list view column is no longer added by default.

## 1.0.1 (June 02, 2015)

* The action was not working correctly in a secured Jenkins environment due to [JENKINS-28688](https://issues.jenkins-ci.org/browse/JENKINS-28688) - thanks to [Shay Erlichmen](https://github.com/erlichmen)

## 1.0 (May 26, 2015)

* first release
