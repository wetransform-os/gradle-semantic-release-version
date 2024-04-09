package to.wetransform.gradle.version;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurableVersionPluginTest {
  @Test
  public void pluginRegistersATask() {
    // Create a test project and apply the plugin
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("to.wetransform.semantic-release-version-custom");

    // Verify that tasks exist
    assertThat(project.getTasks().findByName("showVersion")).isNotNull();
    assertThat(project.getTasks().findByName("setReleaseVersion")).isNotNull();
    assertThat(project.getTasks().findByName("verifyReleaseVersion")).isNotNull();

    // Verify that version is not set
    assertThat(project.getVersion()).isEqualTo(Project.DEFAULT_VERSION);

    //TODO test default version after evaluation
  }
}
