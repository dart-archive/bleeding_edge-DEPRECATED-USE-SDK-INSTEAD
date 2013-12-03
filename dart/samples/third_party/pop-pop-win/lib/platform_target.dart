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

  void trackAnalyticsEvent(String category, String action, [String label,
                                                            int value]) {
    print('Analytics:: '
        'category: $category; action: $action; label: $label; value: $value');
  }

  bool get renderBig;

  bool get showAbout;

  void toggleAbout([bool value]);

  Stream get aboutChanged;
}

class _DefaultPlatform extends PlatformTarget {
  final Map<String, String> _values = new Map<String, String>();
  final StreamController _aboutController = new StreamController(sync: true);
  bool _about = false;

  _DefaultPlatform() : super.base();

  @override
  Future clearValues() => new Future(_values.clear);

  @override
  Future setValue(String key, String value) =>
      new Future(() { _values[key] = value; });

  @override
  Future<String> getValue(String key) => new Future(() => _values[key]);

  bool get renderBig => false;

  void toggleAbout([bool value]) {
    assert(_about != null);
    if(value == null) {
      value = !_about;
    }
    _about = value;
    _aboutController.add(null);
  }

  bool get showAbout => _about;

  Stream get aboutChanged => _aboutController.stream;
}
