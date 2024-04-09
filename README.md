gradle-semantic-release-version
===============================

Gradle plugin that manages the project version based on information from the Git repository the project resides in, primarily the Git tags.

Optionally instead of determining the version from the Git tags, a version file containing the last release version can be used.

The plugin works based on the following assumptions:

1. When a release is created a respective Git tag is created that marks a commit as the release
2. Versions use semantic versioning (`<major>.<minor>.<patch>`) and tags follow this pattern or have a `v` as prefix
3. If the git repository is clean and HEAD points to such a tag, the project version is the release version
4. If the git repository is not clean or HEAD does not point to such a tag, the project version is a SNAPSHOT version that increases the minor version compared to the last release version
5. If no release version can be determined, the project version is `1.0.0-SNAPSHOT`

Plugin variants
---------------

There are two variants of the plugin:

1. `semantic-release-version` is the variant that uses default settings and is not configurable
2. `semantic-release-version-custom` is the variant that is configurable where the behavior can be adapted

The `semantic-release-version` plugin does not use a version file and assumes tags that use the semantic version for a release, optionally including a prefix `v`.

Easiest way to use it is using the version published in the [Gradle Plugin Portal](https://plugins.gradle.org/):

```groovy
plugins {
  id 'to.wetransform.semantic-release-version' version '<version>'
}
```

The `semantic-release-version-custom` plugin allows adapting the plugin configuration and changing settings related to which tags are recognized as version tags and also allows to determine the last release version from a file that is part of the repository.

```groovy
plugins {
  id 'to.wetransform.semantic-release-version-custom' version '<version>'
}
```

When using this plugin variant you need to be aware that the version is only set after evaluation of the Gradle configuration.
That means any logic using the project version also must be executed after evaluation, for example:

```groovy
afterEvaluate {
  // access `version`
}
```

Use release version for dirty repository
----------------------------------------

If the environment variable `RELEASE` is set to `true`, the release version is used, even if the repository is not clean.
This is intended for use cases where a release tag was created, but the repository is expected to be dirty, e.g. due to other CI tasks.

A cleaner method is avoid the repository being dirty, e.g. by adding additional files that are created during the release process to `.gitignore` if possible.

Configuration
-------------

Please note that configuring the plugin is only possible when using the plugin variant `semantic-release-version-custom`.

### Using a version file

If you do not want to rely on determining the last release version from Git, you can instead use a version file.

In that case when a release is created it is expected that:

- The release version is written to the version file
    - The task `setReleaseVersion` can be used for that
- A commit is created the includes the change to the version file and other release related changes
- A tag is created that marks the commit as the release

By default the version file is assumed to be the file `version.txt` in the root project.
