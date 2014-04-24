
#!/bin/sh
set -e

echo "run type factories generator for tests"
./test_tf_gen.sh

echo "run tests in dart"
dart --checked test/main.dart
dart --checked test/generator_test.dart
dart --checked test/injector_generator_spec.dart

echo "run dart2js on tests"
mkdir -p out
dart2js --minify -c test/main.dart -o out/main.dart.js

echo "run tests in node"
node out/main.dart.js

echo "run transformer tests"
pub build --mode=debug test

echo "running transformer test (uncompiled, Dynamic DI)"
dart --checked test/auto_injector_test.dart

echo "running transformer test (Static DI, Dart VM)"
dart --checked build/test/auto_injector_test.dart

echo "running transformer test (Static DI, dart2js)"
# TODO(blois) dart2js compilation is not picking up transformed files, so
# run dart2js manually. dartbug.com/17198
dart2js -c build/test/auto_injector_test.dart -o build/test/auto_injector_test.dart.js;
node build/test/auto_injector_test.dart.js
