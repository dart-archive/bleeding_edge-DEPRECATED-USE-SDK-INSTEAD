library perf_api.console_impl;

import 'dart:html' as dom;
import 'dart:collection';
import 'perf_api.dart';

/**
 * Simple window.console based implementation.
 */
class ConsoleProfiler extends Profiler {
  int _timerIds = 0;
  Map<int, String> _timers = new Map<int, String>();
  Map<int, String> _timerNames = new LinkedHashMap<int, String>();
  final dom.Window window;

  ConsoleProfiler() :this.window = dom.window;

  ConsoleProfiler.forWindow(this.window);

  dynamic startTimer(String name, [dynamic extraData]) {
    var timerId = _timerIds++;
    _timers[timerId] = _timerName(name, extraData);
    _timerNames[timerId] = name;
    window.console.time(_timerStr(timerId, _timers[timerId]));
    return timerId;
  }

  String _timerName(String name, dynamic extraData) =>
      '$name${_stringifyExtraData(extraData)}';

  String _stringifyExtraData(extraData) =>
      (extraData == null || extraData is! String) ? '' : ' $extraData';

  String _timerStr(id, name) => '${name} ($id)';

  void stopTimer(dynamic idOrName) {
    int timerId;
    if (idOrName is int) {
      timerId = idOrName;
    } else {
      // TODO: change this to use a multimap.
      for (var id in _timerNames.keys) {
        if (_timerNames[id] == idOrName) {
          timerId = id;
          break;
        }
      }
    }
    if (timerId == null) {
      throw new ProfilerError('Unable for find timer for $idOrName');
    }
    window.console.timeEnd(_timerStr(timerId, _timers[timerId]));
    _timerNames.remove(timerId);
    _timers.remove(timerId);
  }

  void markTime(String name, [dynamic extraData]) {
    window.console.timeStamp(_timerName(name, extraData));
  }
}