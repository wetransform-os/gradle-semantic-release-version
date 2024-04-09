package to.wetransform.gradle.version

/**
 * Version plugin that does not allow configuration and uses defaults.
 */
class VersionPlugin extends AbstractVersionPlugin {
  VersionPlugin() {
    super(false)
  }
}
