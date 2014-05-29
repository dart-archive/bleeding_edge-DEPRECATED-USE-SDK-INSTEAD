All of the tests in this project can be run using the standard JUnit 3 test runner (that is, they
are not required to be run as a JUnit plug-in test).

However, the analysis engine, and therefore these tests, needs to be able to find the Dart SDK. The
easiest way to make this happen is to configure the location of the SDK on the command line. This
can be done by including a VM argument of the following form:

    -Dcom.google.dart.sdk=

followed by the path to the root of the Dart SDK.

Furthermore, in order to be able to execute the remote server tests, a path to the dart server is
computed from some dart svn root, this is passed with the following argument:

-Dcom.google.dart.svnRoot=
