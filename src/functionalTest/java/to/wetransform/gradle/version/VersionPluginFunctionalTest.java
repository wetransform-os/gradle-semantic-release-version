package to.wetransform.gradle.version;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

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
      .withArguments("greet")
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
