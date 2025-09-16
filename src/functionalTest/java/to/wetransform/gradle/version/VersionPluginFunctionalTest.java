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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

public class VersionPluginFunctionalTest {
  @Test
  public void canRunTask() throws IOException {
    // Setup the test build
    File projectDir = new File("build/functionalTest");
    Files.createDirectories(projectDir.toPath());
    writeString(new File(projectDir, "settings.gradle"), "");
    writeString(new File(projectDir, "build.gradle"),
      "plugins {" +
        "  id('to.wetransform.semantic-release-version')" +
        "}");

    // Run the build
    BuildResult result = GradleRunner.create()
      .forwardOutput()
      .withPluginClasspath()
      .withArguments("showVersion")
      .withProjectDir(projectDir)
      .build();

    // Verify the result
    assertThat(result.getOutput().contains("Version: 1.0.0-SNAPSHOT")).isTrue();
  }

  private void writeString(File file, String string) throws IOException {
    try (Writer writer = new FileWriter(file)) {
      writer.write(string);
    }
  }
}
