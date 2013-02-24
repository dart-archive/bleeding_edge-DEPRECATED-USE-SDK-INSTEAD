This plugin is used to reference third party libraries in the top-level dart/third_party directory.
It (and the com.google.dart.thirdparty_test plugin) should be the only place that reference the
third_party directory, and use the Eclipse path variable DART_TRUNK to create Eclipse symlinks.

This plugin should not contain any libraries that are test specific.

When adding a library to this plugin, you should:
- create a new linked folder in the lib folder, and use the DART_TRUNK path variable to link
  it to the associated third_party directory
- add the jar to the plugin classpath using the manifest.mf editor's runtime tab
- export the relevant packages (again using the runtime tab)
- update the build files to remove the symlink during the build. See
    - com.google.dart.eclipse.feature_releng/build.xml
    - com.google.dart.tools.deploy.feature_releng/build_rcp.xml
    - com.google.dart.tools.tests.feature_releng/buildTests.xml
