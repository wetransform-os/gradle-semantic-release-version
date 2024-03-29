package to.wetransform.gradle.version;

import java.util.function.BiPredicate
import org.gradle.api.Plugin;
import org.gradle.api.Project;

class VersionPlugin implements Plugin<Project> {

  private static def SEM_VER_REGEX = /(\d+)\.(\d+)\.(\d+)/

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

    project.task('verifyReleaseVersion') {
      group 'Version'
      description 'Check if a release version is configured, otherwise (if the version is a -SNAPSHOT version) fail'

      doLast {
        def version = project.version

        assert version
        assert !version.endsWith('-SNAPSHOT')
        assert version =~ SEM_VER_REGEX
      }
    }

    // set version

    project.afterEvaluate {
      if(it.version != Project.DEFAULT_VERSION) {
        throw new IllegalStateException("Version may not be configured if version plugin is applied")
      } else {
        it.version = determineVersion(
          it, it.versionConfig.versionFile, it.versionConfig.gitDir, it.versionConfig.verifyTag
        )
      }
    }
  }

  String determineVersion(Project project, File versionFile, File gitDir, BiPredicate<String, String> verifyTag) {
    // read version from file
    def releaseVersion = null
    if (versionFile.exists()) {
      releaseVersion = versionFile.text.trim()

      // verify version
      if (releaseVersion) {
        def matcher = releaseVersion =~ SEM_VER_REGEX
        if (!matcher) {
          throw new IllegalStateException("Provided version for last release is not a valid semantic version: $releaseVersion")
        }
      }
    }

    if (!releaseVersion) {
      // assume initial snapshot
      project.logger.info("Version file does not exist or contains no version, assuming 1.0.0-SNAPSHOT")
      return '1.0.0-SNAPSHOT'
    }

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
        tag.commit.id == currentCommit && verifyTag.test(tag.name, releaseVersion)
      }
    }

    if ('true'.equalsIgnoreCase(System.getenv('RELEASE'))) {
      // force release version if repo is dirty (e.g. during release in CI)
      // but still verify tag
      if (tagOnCurrentCommit) {
        return releaseVersion
      }
      else {
        throw new IllegalStateException("There is no matching tag for the configured release version $releaseVersion")
      }
    }

    if (tagOnCurrentCommit && !dirty) {
      project.logger.info("Current commit is tagged and repository clean, using release version specified in file: $releaseVersion")
      releaseVersion
    }
    else {
      // build snapshot version with next minor version
      def matcher = releaseVersion =~ SEM_VER_REGEX
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
