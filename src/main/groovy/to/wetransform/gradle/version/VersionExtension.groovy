package to.wetransform.gradle.version

import java.util.function.BiPredicate
import org.gradle.api.Project

class VersionExtension {

  VersionExtension(Project project) {
    this.versionFile = project.rootProject.file('version.txt')
    this.gitDir = project.rootProject.projectDir
  }

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

}
