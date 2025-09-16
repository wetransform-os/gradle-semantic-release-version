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
