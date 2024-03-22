package to.wetransform.gradle.version

import org.gradle.api.Project

class VersionExtension {

  VersionExtension(Project project) {
    this.versionFile = project.rootProject.file('version.txt')
    this.gitDir = project.rootProject.projectDir
  }

  File versionFile

  File gitDir

}
