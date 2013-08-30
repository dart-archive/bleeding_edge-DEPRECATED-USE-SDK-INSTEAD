import 'dart:io';
import 'package:polymer/component_build.dart';     
        
main() {     
  build(new Options().arguments, ['web/{name.lower}.html']);
}