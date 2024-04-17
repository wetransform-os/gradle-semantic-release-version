package to.wetransform.gradle.version

import java.util.function.BiPredicate
import org.gradle.api.Project

class VersionExtension {

  VersionExtension(Project project) {
    this.versionFile = project.rootProject.file('version.txt')
    this.gitDir = project.rootProject.projectDir
  }

  /**
   * States if a version file is used to determine the (release) version.
   * If disabled will solely rely on informaton on Git tags.
   */
  boolean useVersionFile = false

  /**
   * Valid glob patterns for selecting tags when determininig version from Git.
   * See also https://git-scm.com/docs/git-describe#Documentation/git-describe.txt---matchltpatterngt
   */
  Iterable<String> tagGlobPatterns = ["*.*.*"]

  /**
   * Location of the file that holds the last release version, if it exists.
   */
  File versionFile

  /**
   * Location of the git repository to check.
   */
  File gitDir

  /**
   * Test to verify if a tag is a version tag and if the version matches the provided release version.
   */
  BiPredicate<String, String> verifyTag = { tag, version ->
    def matcher = tag =~ /^v?((\d+)\.(\d+)\.(\d+))$/
    if (matcher) {
      matcher[0][1] == version
    }
    else {
      false
    }
  }

  /**
   * Whether to include buildscript dependencies when checking for SNAPSHOT dependencies.
   */
  boolean checkBuildscriptForSnapshotDependencies = false

}
