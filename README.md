gradle-semantic-release-version
===============================

Gradle plugin that manages the project version based on:

- A version file containing the last release version
- Information from the git repository the project resides in

The plugin works based on the following assumptions:

1. When a release is created
    - The release version is written to the version file
        - The task `setReleaseVersion` can be used for that
    - A commit is created the includes the change to the version file and other release related changes
    - A tag is created that marks the commit as the release
2. If the git repository is clean and HEAD points to a tag, the project version is the release version
3. If the git repository is not clean or HEAD does not point to a tag, the project version is a SNAPSHOT version that increases the minor version compared to the last release version
4. If no release version is configured or the version file is missing, the project version is `1.0.0-SNAPSHOT`

If the environment variable `RELEASE` is set to `true`, the release version is used, even if the repository is not clean or there is no tag.
This is intended for use cases where the release version was set, but the repository is dirty or no tag was created.

By default the version file is assumed to be the file `version.txt` in the root project.