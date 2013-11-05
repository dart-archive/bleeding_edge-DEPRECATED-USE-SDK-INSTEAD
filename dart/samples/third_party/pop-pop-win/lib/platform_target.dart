library ppw_platform;

import 'dart:async';

abstract class PlatformTarget {
  bool _initialized = false;

  factory PlatformTarget() => new _DefaultPlatform();

  PlatformTarget.base();

  bool get initialized => _initialized;

  void initialize() {
    assert(!_initialized);
    _initialized = true;
  }

  Future clearValues();

  Future setValue(String key, String value);

  Future<String> getValue(String key);

  void trackAnalyticsEvent(String category, String action, [String label, int value]) {
    print('Analytics:: category: $category; action: $action, label: $label, value: $value');
  }
}

class _DefaultPlatform extends PlatformTarget {
  final Map<String, String> _values = new Map<String, String>();

  _DefaultPlatform() : super.base();

  @override
  Future clearValues() {
    return new Future(_values.clear);
  }

  @override
  Future setValue(String key, String value) {
    return new Future(() {
      _values[key] = value;
    });
  }

  @override
  Future<String> getValue(String key) {
    return new Future(() {
      return _values[key];
    });
  }
}
