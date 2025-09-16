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
package to.wetransform.gradle.version;

import static org.assertj.core.api.Assertions.*;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

public class VersionPluginTest {
  @Test
  public void pluginRegistersATask() {
    // Create a test project and apply the plugin
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("to.wetransform.semantic-release-version");

    // Verify that tasks exist
    assertThat(project.getTasks().findByName("showVersion")).isNotNull();
    assertThat(project.getTasks().findByName("setReleaseVersion")).isNull();
    assertThat(project.getTasks().findByName("verifyReleaseVersion")).isNotNull();

    // Verify that version is not set
    assertThat(project.getVersion()).isEqualTo(AbstractVersionPlugin.DEFAULT_SNAPSHOT);
  }
}
