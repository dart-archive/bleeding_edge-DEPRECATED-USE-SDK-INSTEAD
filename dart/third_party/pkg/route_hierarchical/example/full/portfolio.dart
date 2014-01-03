library portfolio;

import 'dart:html';

import 'package:web_ui/web_ui.dart';
import 'package:route_hierarchical/client.dart';

import 'index.dart';
import 'data.dart' as data;


class PortfolioComponent extends WebComponent {
  RouteHandle route;
  RouteHandle companyRoute;
  @observable var tabs = toObservable([]);
  @observable var activeTab;
  @observable var companies;
  @observable String searchQuery;

  inserted() {
    tabs.add({
      'name': 'Portfolio',
      'link': router.url('list', startingFrom: route)
    });

    route.getRoute('list').onRoute.listen((RouteEvent e) {
      searchQuery =
          e.parameters.containsKey('query') ? e.parameters['query'] : '';
      activeTab = tabs[0];
      if (e.path != '/list') {
        router.go('list', {}, startingFrom: route, replace: true);
      } else {
        companies = null;
        data.fetchCompanies(searchQuery).then((result) => companies = result);
      }
    });
    companyRoute = route.getRoute('company');
    companyRoute.onRoute.listen(showCompanyTab);
  }

  removed() {
    route.discard();
  }

  void showCompanyTab(RouteEvent e) {
    var tokenInt = int.parse(e.parameters['tabId']);
    // If it's one of the current tabs, we show that tab
    for (var tab in tabs) {
      if (tab['userValue'] != null && tab['userValue']['id'] == tokenInt) {
        activeTab = tab;
        return;
      }
    }

    // Otherwise we try to load the company
    var newTab = toObservable({
      'name': 'Loading...',
    });
    tabs.add(newTab);
    activeTab = tabs[tabs.length - 1];
    data.fetchCompany(tokenInt).then((company) {
      if (company != null) {
        newTab['name'] = company['name'];
        newTab['userValue'] = company;
        newTab['link'] = companyLink(company);
      } else {
        // TODO: show a message that company id is invalid or something
      }
    });
  }

  void openCompany(company, MouseEvent e) {
    if (e != null) {
      e.preventDefault();
    }
    // set new history token...
    router.go('company', {
      'tabId': '${company['id']}'
    }, startingFrom: route);
  }

  String companyLink(company) {
    return router.url('company',
        parameters: {'tabId': '${company['id']}'}, startingFrom: route);
  }

  String activeClass(tab) {
    if (tab == activeTab) {
      return "active";
    }
    return "";
  }

  void search() {
    router.go('list', {'list.query': searchQuery}, startingFrom: route);
  }
}