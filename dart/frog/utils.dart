// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Collection<T> supports most of the ES 5 Array methods, but it's missing
// map and reduce.

// TODO(jmesserly): we might want a version of this that return an iterable,
// however JS, Python and Ruby versions are all eager.
List map(Iterable source, mapper(source)) {
  List result = new List();
  if (source is List) {
    List list = source; // TODO: shouldn't need this
    result.length = list.length;
    for (int i = 0; i < list.length; i++) {
      result[i] = mapper(list[i]);
    }
  } else {
    for (final item in source) {
      result.add(mapper(item));
    }
  }
  return result;
}

reduce(Iterable source, callback, [initialValue]) {
  final i = source.iterator();

  var current = initialValue;
  if (current == null && i.hasNext()) {
    current = i.next();
  }
  while (i.hasNext()) {
    current = callback(current, i.next());
  }
  return current;
}

List zip(Iterable left, Iterable right, mapper(left, right)) {
  List result = new List();
  var x = left.iterator();
  var y = right.iterator();
  while (x.hasNext() && y.hasNext()) {
    result.add(mapper(x, y));
  }
  if (x.hasNext() || y.hasNext()) {
    throw new IllegalArgumentException();
  }
  return result;
}

/** Sorts the map by the key. */
List orderValuesByKeys(Map map) {
  // TODO(jmesserly): it'd be nice to have SortedMap in corelib.
  final keys = map.getKeys();
  keys.sort((x, y) => x.compareTo(y));
  final values = [];
  for (var k in keys) {
    values.add(map[k]);
  }
  return values;
}

/** True if this is a triple quoted Dart string literal. */
bool isMultilineString(String text) {
  return text.startsWith('"""') || text.startsWith("'''");
}

bool isRawMultilineString(String text) {
  return text.startsWith('@"""') || text.startsWith("@'''");
}

// TODO(jmesserly): it'd be nice to deal with this in the tokenizer, rather than
// taking another pass over the string.
String parseStringLiteral(String lit) {
  if (lit.startsWith('@')) {
    if (isRawMultilineString(lit)) {
      return stripLeadingNewline(lit.substring(4, lit.length - 3));
    } else {
      return lit.substring(2, lit.length - 1);
    }
  } else if (isMultilineString(lit)) {
    lit = lit.substring(3, lit.length - 3).replaceAll('\\\$', '\$');
    return stripLeadingNewline(lit);
  } else {
    return lit.substring(1, lit.length - 1).replaceAll('\\\$', '\$');
  }
}

// The first newline in a multiline string is removed.
String stripLeadingNewline(String text) {
  if (text.startsWith('\n')) {
    return text.substring(1);
  } else if (text.startsWith('\r')) {
    if (text.startsWith('\r\n')) {
      return text.substring(2);
    } else {
      return text.substring(1);
    }
  } else {
    return text;
  }
}

/**
 * A [FixedCollection] is a collection of [length] items all of which have the
 * identical [value]
 */
class FixedCollection<E> implements Collection<E> {
  final E value;
  final int length;
  const FixedCollection(this.value, this.length);

  Iterator<E> iterator() => new FixedIterator<E>(value, length);
  void forEach(void f(E element)) { Collections.forEach(this, f); }
  Collection<E> filter(bool f(E element)) {
    return Collections.filter(this, new List<E>(), f);
  }
  bool every(bool f(E element)) => Collections.every(this, f);
  bool some(bool f(E element)) => Collections.some(this, f);
  bool isEmpty() => length == 0;
}

class FixedIterator<E> implements Iterator<E> {
  final E value;
  final int length;
  int _index = 0;
  FixedIterator(this.value, this.length);

  bool hasNext() => _index < length;
  E next() {
    _index++;
    return value;
  }
}
