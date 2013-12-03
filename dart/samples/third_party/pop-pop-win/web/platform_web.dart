library ppw_platform_web;

import 'dart:async';
import 'dart:html';
import 'dart:js' as js;
import 'package:poppopwin/platform_target.dart';

class PlatformWeb extends PlatformTarget {
  static const String _BIG_HASH = '#big';
  static const String _ABOUT_HASH = '#about';

  final StreamController _aboutController = new StreamController(sync: true);

  PlatformWeb() : super.base() {
    window.onPopState.listen((args) => _processUrlHash());
  }

  @override
  Future clearValues() {
    window.localStorage.clear();
    return new Future.value();
  }

  @override
  Future setValue(String key, String value) {
    window.localStorage[key] = value;
    return new Future.value();
  }

  @override
  Future<String> getValue(String key) =>
      new Future.value(window.localStorage[key]);

  @override
  void trackAnalyticsEvent(String category, String action, [String label,
                                                            int value]) {
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

  bool get renderBig => _urlHash == _BIG_HASH;

  bool get showAbout => _urlHash == _ABOUT_HASH;

  Stream get aboutChanged => _aboutController.stream;

  void toggleAbout([bool value]) {
    final Location loc = window.location;
    // ensure we treat empty hash like '#', which makes comparison easy later
    final hash = loc.hash.length == 0 ? '#' : loc.hash;

    final isOpen = hash == _ABOUT_HASH;
    if(value == null) {
      // then toggle the current value
      value = !isOpen;
    }

    var targetHash = value ? _ABOUT_HASH : '#';
    if(targetHash != hash) {
      loc.assign(targetHash);
    }
    _aboutController.add(null);
  }

  String get _urlHash => window.location.hash;

  void _processUrlHash() {
    final Location loc = window.location;
    final hash = loc.hash;
    final href = loc.href;

    final History history = window.history;
    switch(hash) {
      case "#reset":
        assert(href.endsWith(hash));
        var newLoc = href.substring(0, href.length - hash.length);

        window.localStorage.clear();

        loc.replace(newLoc);
        break;
      case _BIG_HASH:
        loc.reload();
        break;
      case _ABOUT_HASH:
        _aboutController.add(null);
        break;
    }
  }
}
