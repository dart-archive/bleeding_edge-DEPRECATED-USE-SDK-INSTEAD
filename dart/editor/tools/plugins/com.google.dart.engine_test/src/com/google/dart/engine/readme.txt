All of the tests in this project can be run using the standard JUnit 3 test runner (that is, they
are not required to be run as a JUnit plug-in test).

However, the analysis engine, and therefore these tests, needs to be able to find the Dart SDK. The
easiest way to make this happen is to configure the location of the SDK on the command line. This
can be done by including a VM argument of the following form:

    -Dcom.google.dart.sdk=

followed by the path to the root of the Dart SDK.