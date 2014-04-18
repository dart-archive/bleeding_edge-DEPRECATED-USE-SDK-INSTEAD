

/**
 * Instances of the class `SingleMapIterator` implement an iterator that can be used to access
 * the entries in a single map.
 */
class SingleMapIterator<K, V> implements MapIterator<K, V> {
  /**
   * Returns a new [SingleMapIterator] instance for the given [Map].
   */
  static SingleMapIterator forMap(Map map) => new SingleMapIterator(map);

  /**
   * The [Map] containing the entries to be iterated over.
   */
  final Map<K, V> _map;

  /**
   * The iterator used to access the entries.
   */
  Iterator<K> _keyIterator;

  /**
   * The current key, or `null` if there is no current key.
   */
  K _currentKey;

  /**
   * The current value.
   */
  V _currentValue;

  /**
   * Initialize a newly created iterator to return the entries from the given map.
   *
   * @param map the map containing the entries to be iterated over
   */
  SingleMapIterator(this._map) {
    this._keyIterator = _map.keys.iterator;
  }

  @override
  K get key {
    if (_currentKey == null) {
      throw new NoSuchElementException();
    }
    return _currentKey;
  }

  @override
  V get value {
    if (_currentKey == null) {
      throw new NoSuchElementException();
    }
    return _currentValue;
  }

  @override
  bool moveNext() {
    if (_keyIterator.moveNext()) {
      _currentKey = _keyIterator.current;
      _currentValue = _map[_currentKey];
      return true;
    } else {
      _currentKey = null;
      return false;
    }
  }

  @override
  void set value(V newValue) {
    if (_currentKey == null) {
      throw new NoSuchElementException();
    }
    _currentValue = newValue;
    _map[_currentKey] = newValue;
  }
}
