
#!/bin/sh
set -e

echo "run type factories generator for tests"
./test_tf_gen.sh

echo "run tests in dart"
dart --checked test/main.dart
dart --checked test/generator_test.dart

echo "run dart2js on tests"
mkdir -p out
dart2js --minify -c test/main.dart -o out/main.dart.js

echo "run tests in node"
node out/main.dart.js
