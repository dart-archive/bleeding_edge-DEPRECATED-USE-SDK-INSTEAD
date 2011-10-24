Notes about Dart Editor workspace setup, development, build, and deploy.
    
====================================
  Building the Dart Editor
====================================

In the dart/editor/tools/features/com.google.dart.tools.deploy.feature_releng/build-settings
directory, copy the user.properties file to <username>.properties, where <username> is your
login name. Adjust the two properties in that file to point to:

  -the Dart Editor source directory (dart/editor)
  -a build output directory

Run the build_rcp.xml ant script in the com.google.dart.tools.deploy.feature_releng project
(ant -f build_rcp.xml). It will create Windows, Linux, and Mac builds in the 'out' directory
of the build directory specified above.
