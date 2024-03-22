package to.wetransform.gradle.version;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

class VersionPlugin implements Plugin<Project> {
  void apply(Project project) {
    //XXX not sure how to easiest use the service - instead the repo is opened manually
    // project.apply(plugin: 'org.ajoberstar.grgit.service')

    // register extension
    project.extensions.create('versionConfig', VersionExtension, project)

    // define tasks
    project.task('showVersion') {
      group 'Version'
      description 'Print current project version'

      doLast {
        println "Version: ${project.version}"
      }
    }

    project.task('setReleaseVersion') {
      group 'Version'
      description 'Set a new release version (write to version file), provide version to set as Gradle property `newVersion`'

      doLast {
        def versionFile = project.versionConfig.versionFile
        versionFile.text = project.properties['newVersion']
      }
    }

    // set version

    project.afterEvaluate {
      if(it.version != Project.DEFAULT_VERSION) {
        throw new IllegalStateException("Version may not be configured if version plugin is applied")
      } else {
        it.version = determineVersion(it, it.versionConfig.versionFile, it.versionConfig.gitDir)
      }
    }
  }

  String determineVersion(Project project, File versionFile, File gitDir) {
    // read version from file
    if (!versionFile.exists()) {
      // assume initial snapshot
      project.logger.info("Version file does not exists, assuming 1.0.0-SNAPSHOT")
      return '1.0.0-SNAPSHOT'
    }
    def releaseVersion = versionFile.text.trim()

    def dirty = false
    def tagOnCurrentCommit = false
    def grgit
    try {
      grgit = org.ajoberstar.grgit.Grgit.open(dir: gitDir)
    } catch (IllegalStateException e) {
      project.logger.warn("Could not open Git repository in $gitDir", e)
      grgit = null
    }
    if (grgit) {
      dirty = !grgit.status().isClean()
      def currentCommit = grgit.head().id
      tagOnCurrentCommit = grgit.tag.list().findAll { tag ->
        tag.commit.id == currentCommit
      }
    }
    if (tagOnCurrentCommit && !dirty) {
      //TODO check tags?
      //XXX for now assume tag represents release
      project.logger.info("Current commit is tagged and repository clean, using release version specified in file: $releaseVersion")
      releaseVersion
    }
    else {
      // build snapshot version with next minor version
      def matcher = releaseVersion =~ /(\d+)\.(\d+)\.(\d+)/
      if (matcher) {
        project.logger.info("Current commit is not tagged or repository is dirty, using snapshot version based on last release")

        def major = matcher[0][1] as int
        def minor = matcher[0][2] as int

        "${major}.${minor+1}.0-SNAPSHOT"
      }
      else {
        throw new IllegalStateException("Provided version not a semantic version")
      }
    }
  }
}
