library ppw_platform_web;

import 'dart:async';
import 'dart:html';
import 'package:pop_pop_win/platform_target.dart';

class PlatformWeb extends PlatformTarget {
  static const String _ABOUT_HASH = '#about';
  bool _sizeAccessed = false;

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

  int get size {
    _sizeAccessed = true;
    var hash = (_urlHash == null) ? '7' : _urlHash;
    hash = hash.replaceAll('#', '');
    return int.parse(hash, onError: (e) => 7);
  }

  bool get showAbout => _urlHash == _ABOUT_HASH;

  Stream get aboutChanged => _aboutController.stream;

  void toggleAbout([bool value]) {
    var loc = window.location;
    // ensure we treat empty hash like '#', which makes comparison easy later
    var hash = loc.hash.length == 0 ? '#' : loc.hash;

    var isOpen = hash == _ABOUT_HASH;
    if (value == null) {
      // then toggle the current value
      value = !isOpen;
    }

    var targetHash = value ? _ABOUT_HASH : '#';
    if (targetHash != hash) {
      loc.assign(targetHash);
    }
    _aboutController.add(null);
  }

  String get _urlHash => window.location.hash;

  void _processUrlHash() {
    var loc = window.location;
    var hash = loc.hash;
    var href = loc.href;

    switch (hash) {
      case "#reset":
        assert(href.endsWith(hash));
        var newLoc = href.substring(0, href.length - hash.length);

        window.localStorage.clear();

        loc.replace(newLoc);
        break;
      case _ABOUT_HASH:
        _aboutController.add(null);
        break;
      default:
        if (hash.isNotEmpty && _sizeAccessed) {
          loc.reload();
        }
        break;
    }
  }
}
