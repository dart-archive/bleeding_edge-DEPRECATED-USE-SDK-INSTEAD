import 'dart:io';
import 'package:web_ui/component_build.dart';

// Ref: http://www.dartlang.org/articles/dart-web-components/tools.html
main() {
  build(new Options().arguments, ['example/full/index.html']);
}
