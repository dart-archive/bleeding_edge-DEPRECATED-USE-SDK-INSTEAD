library ppw_platform_web;

import 'dart:async';
import 'dart:html';
import 'dart:js' as js;
import 'package:poppopwin/platform_target.dart';

class PlatformWeb extends PlatformTarget {

  PlatformWeb() : super.base();

  @override
  Future clearValues() {
    window.localStorage.clear();
    return new Future.value(null);
  }

  @override
  Future setValue(String key, String value) {
    window.localStorage[key] = value;
    return new Future.value(null);
  }

  @override
  Future<String> getValue(String key) =>
      new Future.value(window.localStorage[key]);

  @override
  void trackAnalyticsEvent(String category, String action, [String label, int value]) {
    var args = ['send', 'event', category, action];
    if(label != null) {
      args.add(label);
    }

    if(value != null) {
      assert(label != null);
      args.add(value);
    }

    js.context.callMethod('ga', args);
  }
}
