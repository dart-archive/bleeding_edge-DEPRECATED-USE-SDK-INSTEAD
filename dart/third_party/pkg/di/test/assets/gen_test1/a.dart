library lib_a;

import 'annotations.dart';
import 'a2.dart';
import 'common1.dart';

@Injectable()
class ServiceA {
  sayHi() {
    print('Hi ServiceA!');
  }
}