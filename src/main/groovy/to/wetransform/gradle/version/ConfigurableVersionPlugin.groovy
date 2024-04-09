package to.wetransform.gradle.version

/**
 * Version plugin that allows customization through VersionExtension.
 */
class ConfigurableVersionPlugin extends AbstractVersionPlugin {
  ConfigurableVersionPlugin() {
    super(true)
  }
}
