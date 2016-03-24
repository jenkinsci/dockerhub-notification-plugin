# Changelog

Only noting significant user-visible or major API changes, not internal code cleanups and minor bug fixes.

## upcoming relese

## 2.0 (March 24, 2016)

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

