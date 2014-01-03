library companyinfo;

import 'dart:async';
import 'dart:html';

import 'package:route_hierarchical/client.dart';
import 'package:web_ui/web_ui.dart';

import 'index.dart';


class CompanyInfoComponent extends WebComponent {
  var company;
  RouteHandle route;
  @observable String section;
  @observable String infoUrl;
  @observable String activitiesUrl;
  @observable String notesUrl;

  inserted() {
    route.getRoute('info').onRoute.listen((_) => showSection('info'));
    route.getRoute('activities')
        .onRoute.listen((_) => showSection('activities'));
    route.getRoute('notes')
      ..onRoute.listen((_) => showSection('notes'))
      ..onLeave.listen(notesLeave);

    infoUrl = router.url('info', startingFrom: route);
    activitiesUrl = router.url('activities', startingFrom: route);
    notesUrl = router.url('notes', startingFrom: route);
  }

  removed() {
    route.discard();
  }

  notesLeave(RouteEvent e) {
    e.allowLeave(new Future.value(window.confirm('Are you sure you want ' +
                                                 'to leave?')));
  }

  showSection(section) {
    this.section = section;
  }

  gotoSection(section, e) {
    if (e != null) {
      e.preventDefault();
    }
    router.go(section, {}, startingFrom: route).then((allowed) {
      if (allowed) {
        showSection(section);
      }
    });
  }

  String activeClass(sect) {
    if (sect == section) {
      return "active";
    }
    return "";
  }
}