part of pop_pop_win.html;

class GameStorage {
  static const _gameCountKey = 'gameCount';
  final EventHandle _bestTimeUpdated = new EventHandle();
  final Map<String, String> _cache = new Map<String, String>();

  Future<int> get gameCount => _getIntValue(_gameCountKey);

  Stream get bestTimeUpdated => _bestTimeUpdated.stream;

  void recordState(GameState state) {
    assert(state != null);
    _incrementIntValue(state.name);
  }

  Future<bool> updateBestTime(Game game) {
    assert(game != null);
    assert(game.state == GameState.won);

    final w = game.field.width;
    final h = game.field.height;
    final m = game.field.bombCount;
    final duration = game.duration.inMilliseconds;

    final key = _getKey(w, h, m);

    return _getIntValue(key, null)
        .then((int currentScore) {
          if(currentScore == null || currentScore > duration) {
            _setIntValue(key, duration);
            _bestTimeUpdated.add(null);
            return true;
          } else {
            return false;
          }
        });
  }

  Future<int> getBestTimeMilliseconds(int width, int height, int bombCount) {
    final key = _getKey(width, height, bombCount);
    return _getIntValue(key, null);
  }

  Future reset() {
    _cache.clear();
    return targetPlatform.clearValues();
  }

  Future<int> _getIntValue(String key, [int defaultValue = 0]) {
    assert(key != null);
    if (_cache.containsKey(key)) {
      return new Future.value(_parseValue(_cache[key], defaultValue));
    }

    return targetPlatform.getValue(key)
        .then((String strValue) {
          _cache[key] = strValue;
          return _parseValue(strValue, defaultValue);
        });
  }

  Future _setIntValue(String key, int value) {
    assert(key != null);
    _cache.remove(key);
    String val = (value == null) ? null : value.toString();
    return targetPlatform.setValue(key, val);
  }

  Future _incrementIntValue(String key) {
    return _getIntValue(key)
        .then((int val) {
          return _setIntValue(key, val + 1);
        });
  }

  static String _getKey(int w, int h, int m) => "w$w-h$h-m$m";

  static int _parseValue(String value, int defaultValue) {
    if(value == null) {
      return defaultValue;
    } else {
      return int.parse(value);
    }
  }

}
