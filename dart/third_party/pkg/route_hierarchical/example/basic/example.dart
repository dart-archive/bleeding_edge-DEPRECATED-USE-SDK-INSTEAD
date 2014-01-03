library example;

import 'dart:html';

import 'package:logging/logging.dart';
import 'package:route_hierarchical/client.dart';

main() {
  new Logger('')
    ..level = Level.FINEST
    ..onRecord.listen((r) => print('[${r.level}] ${r.message}'));

  query('#warning').remove();

  var router = new Router(useFragment: true);
  router.root
    ..addRoute(name: 'one', defaultRoute: true, path: '/one', enter: showOne)
    ..addRoute(name: 'two', path: '/two', enter: showTwo);

  query('#linkOne').attributes['href'] = router.url('one');
  query('#linkTwo').attributes['href'] = router.url('two');

  router.listen();
}

void showOne(RouteEvent e) {
  print("showOne");
  query('#one').classes.add('selected');
  query('#two').classes.remove('selected');
}

void showTwo(RouteEvent e) {
  print("showTwo");
  query('#one').classes.remove('selected');
  query('#two').classes.add('selected');
}
