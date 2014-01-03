import 'package:logging/logging.dart';
import 'package:route_hierarchical/client.dart';

Router router;
String homeLink;
String portfolioLink;

void main() {
  new Logger('')
      ..level = Level.FINEST
      ..onRecord.listen((r) =>
          print('${new DateTime.now()}: [${r.level}] ${r.message}'));

  router = new Router(useFragment: true);
  router.root
    ..addRoute(
        name: 'home',
        defaultRoute: true,
        path: '/home')
    ..addRoute(
        name: 'companyInfo',
        path: '/companyInfo',
        mount: (child) => child
          ..addRoute(
              name: 'companyId',
              path: '/:companyId',
              mount: new _CompanyInfoRoutable())
          ..addRoute(
              name: 'invalid',
              path: '/invalid',
              defaultRoute: true))
    ..addRoute(
        name: 'portfolio',
        path: '/portfolio',
        mount: (Route child) => child
          ..addRoute(
              name: 'list',
              path: '/list',
              defaultRoute: true)
          ..addRoute(
              name: 'company',
              path: '/:tabId',
              mount: new _CompanyInfoRoutable()));
  router.listen();

  homeLink = router.url('home');
  portfolioLink = router.url('portfolio');
}

class _CompanyInfoRoutable implements Routable {
  void configureRoute(Route route) {
    route
      ..addRoute(
          name: 'info',
          defaultRoute: true,
          path: '/info')
      ..addRoute(
          name: 'activities',
          path: '/activities')
      ..addRoute(
          name: 'notes',
          path: '/notes');
  }
}