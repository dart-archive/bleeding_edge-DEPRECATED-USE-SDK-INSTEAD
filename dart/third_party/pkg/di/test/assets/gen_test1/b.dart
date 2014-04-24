library lib_b;

import 'annotations.dart';
import 'b2.dart';
import 'common2.dart';

@Injectable()
class ServiceB {
  sayHi() {
    print('Hi ServiceB!');
  }
}