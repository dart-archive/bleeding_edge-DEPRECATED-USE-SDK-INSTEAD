import 'package:polymer/builder.dart';
        
main(args) {
  build(entryPoints: ['web/{name.lower}.html'],
        options: parseOptions(args));
}
