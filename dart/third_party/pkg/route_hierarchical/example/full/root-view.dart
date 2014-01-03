library companyinfo;

import 'dart:html';
import 'package:web_ui/web_ui.dart';
import 'package:route_hierarchical/client.dart';

class RootView extends WebComponent {
  RouteHandle route;
  @observable String currentRouteName;

  inserted() {
    ['home', 'companyInfo', 'portfolio'].forEach((routeName) =>
        route.getRoute(routeName)
           .onRoute.listen((_) => currentRouteName = routeName));
  }

  removed() {
    route.discard();
  }
}