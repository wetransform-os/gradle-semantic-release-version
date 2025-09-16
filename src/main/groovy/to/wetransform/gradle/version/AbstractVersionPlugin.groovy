/*
 * Copyright 2024 wetransform GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package to.wetransform.gradle.version

import java.util.function.BiPredicate

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency

abstract class AbstractVersionPlugin implements Plugin<Project> {

  private static def SEM_VER_REGEX = /^(\d+)\.(\d+)\.(\d+)$/

  private static def SEM_VER_EXTRACT_REGEX = /((\d+)\.(\d+)\.(\d+))$/

  public static def DEFAULT_SNAPSHOT = '1.0.0-SNAPSHOT'

  private final boolean allowConfiguration

  protected AbstractVersionPlugin(boolean allowConfiguration) {
    this.allowConfiguration = allowConfiguration
  }

  void apply(Project project) {
    //XXX not sure how to easiest use the service - instead the repo is opened manually
    // project.apply(plugin: 'org.ajoberstar.grgit.service')

    // register extension
    VersionExtension extension
    if (allowConfiguration) {
      extension = project.extensions.create('versionConfig', VersionExtension, project)
    }
    else {
      extension = new VersionExtension(project)
    }

    // define tasks
    project.task('showVersion') {
      group 'Version'
      description 'Print current project version'

      doLast {
        println "Version: ${project.version}"
      }
    }

    if (allowConfiguration || extension.useVersionFile) {
      project.task('setReleaseVersion') {
        group 'Version'
        description 'Set a new release version (write to version file), provide version to set as Gradle property `newVersion`'

        doLast {
          def versionFile = extension.versionFile
          versionFile.text = project.properties['newVersion']
        }
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

        // check that there are no SNAPSHOT dependencies - a release should not contain any SNAPSHOT dependencies
        //TODO make configurable?
        checkSnapshotDependencies(project, extension)
      }
    }

    project.task('verifyNoSnapshotDependencies') {
      group 'Version'
      description 'Check that dependencies don\'t use SNAPSHOT versions'

      doLast {
        checkSnapshotDependencies(project, extension)
      }
    }

    // set version
    if (allowConfiguration) {
      project.afterEvaluate {
        // apply after evaluate to allow configuring settings
        applyVersion(it, extension)
      }
    }
    else {
      // directly apply with defaults
      applyVersion(project, extension)
    }
  }

  void applyVersion(Project project, VersionExtension extension) {
    if (project.version != Project.DEFAULT_VERSION) {
      throw new IllegalStateException("Version may not be configured if version plugin is applied")
    } else {
      project.version = determineVersion(
        project, extension.useVersionFile, extension.tagGlobPatterns, extension.versionFile, extension.gitDir, extension.verifyTag
        )
    }
  }

  /**
   * Check that there are no SNAPSHOT dependencies.
   *
   * @param project the project to check
   */
  void checkSnapshotDependencies(Project project, VersionExtension extension) {
    def matcher = { Dependency d ->
      // exclude project dependencies (we assume they are also managed by the plugin and will automatically change on release)
      // include if version is SNAPSHOT version
      !(d instanceof ProjectDependency) && d.version?.contains('SNAPSHOT')
    }
    def collector = { Dependency d -> "${d.group ?: ''}:${d.name}:${d.version ?: ''}" }

    def message = ""

    def snapshotDependencies = [] as Set
    project.configurations.each { cfg ->
      snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
    }
    if (extension.checkBuildscriptForSnapshotDependencies) {
      project.buildscript.configurations.each { cfg ->
        snapshotDependencies += cfg.dependencies?.matching(matcher)?.collect(collector)
      }
    }
    if (snapshotDependencies.size() > 0) {
      message += "\n\t${project.name}: ${snapshotDependencies}"
    }

    if (message) {
      message = "Snapshot dependencies detected: ${message}"
      throw new IllegalStateException(message)
    }
  }

  String determineVersion(Project project, boolean useVersionFile, Iterable<String> tagMatchPatterns, File versionFile, File gitDir, BiPredicate<String, String> verifyTag) {
    def releaseVersion = null
    def grgit = null

    if (useVersionFile) {
      // read version from file
      if (versionFile.exists()) {
        releaseVersion = versionFile.text.trim()

        // verify version
        if (releaseVersion) {
          def match = releaseVersion ==~ SEM_VER_REGEX
          if (!match) {
            throw new IllegalStateException("Provided version for last release is not a valid semantic version: $releaseVersion")
          }
        }
      }

      if (!releaseVersion) {
        // assume initial snapshot
        project.logger.info("Version file does not exist or contains no version, assuming ${DEFAULT_SNAPSHOT}")
        return DEFAULT_SNAPSHOT
      }
    } else {
      // use information from git to determine last version
      try {
        grgit = org.ajoberstar.grgit.Grgit.open(dir: gitDir)
      } catch (Exception e) {
        project.logger.warn("Could not open Git repository in $gitDir", e)
      }

      if (!grgit) {
        project.logger.info("No Git repository found, assuming ${DEFAULT_SNAPSHOT} as version")
        return DEFAULT_SNAPSHOT
      }

      def describe = grgit.describe(abbrev: 0, tags: true, match: tagMatchPatterns as List)
      if (!describe) {
        // nothing found
        project.logger.info("No tag found for determining version, assuming ${DEFAULT_SNAPSHOT}")
        return DEFAULT_SNAPSHOT
      }
      else {
        def matcher = describe =~ SEM_VER_EXTRACT_REGEX
        if (matcher) {
          releaseVersion = matcher[0][1]
        }
        else {
          throw new IllegalStateException("Cannot extract release version from tag: $describe")
        }
      }
    }

    def dirty = false
    def tagOnCurrentCommit = false
    if (grgit == null) {
      try {
        grgit = org.ajoberstar.grgit.Grgit.open(dir: gitDir)
      } catch (Exception e) {
        project.logger.warn("Could not open Git repository in $gitDir", e)
        grgit = null
      }
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
