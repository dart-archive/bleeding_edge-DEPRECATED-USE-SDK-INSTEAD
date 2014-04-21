library pop_pop_win.platform;

import 'package:pop_pop_win/platform_target.dart';

PlatformTarget _platformImpl;

void initPlatform(PlatformTarget value) {
  assert(value != null);
  assert(!value.initialized);
  assert(_platformImpl == null);
  _platformImpl = value;
  _platformImpl.initialize();
}

PlatformTarget get targetPlatform {
  if (_platformImpl == null) {
    initPlatform(new PlatformTarget());
  }
  return _platformImpl;
}
