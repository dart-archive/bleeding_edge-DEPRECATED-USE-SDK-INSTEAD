// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library observable;

import 'dart:collection';
import 'dart:collection-dev';

part 'ChangeEvent.dart';
part 'EventBatch.dart';

/**
 * An object whose changes are tracked and who can issue events notifying how it
 * has been changed.
 */
abstract class Observable {
  /** Returns a globally unique identifier for the object. */
  // TODO(sigmund): remove once dart supports maps with arbitrary keys.
  int get uid;

  /** Listeners on this model. */
  List<ChangeListener> get listeners;

  /** The parent observable to notify when this child is changed. */
  Observable get parent;

  /**
   * Adds a listener for changes on this observable instance. Returns whether
   * the listener was added successfully.
   */
  bool addChangeListener(ChangeListener listener);

  /**
   * Removes a listener for changes on this observable instance. Returns whether
   * the listener was removed successfully.
   */
  bool removeChangeListener(ChangeListener listener);
}


/** Common functionality for observable objects. */
class AbstractObservable implements Observable {

  /** Unique id to identify this model in an event batch. */
  final int uid;

  /** The parent observable to notify when this child is changed. */
  final Observable parent;

  /** Listeners on this model. */
  List<ChangeListener> listeners;

  /** Whether this object is currently observed by listeners or propagators. */
  bool get isObserved {
    for (Observable obj = this; obj != null; obj = obj.parent) {
      if (listeners.length > 0) {
        return true;
      }
    }
    return false;
  }

  AbstractObservable([Observable this.parent = null])
    : uid = EventBatch.genUid(),
      listeners = new List<ChangeListener>();

  bool addChangeListener(ChangeListener listener) {
    if (listeners.indexOf(listener, 0) == -1) {
      listeners.add(listener);
      return true;
    }

    return false;
  }

  bool removeChangeListener(ChangeListener listener) {
    // TODO(rnystrom): This is awkward without List.remove(e).
    if (listeners.indexOf(listener, 0) != -1) {
      bool found = false;
      listeners = listeners
          .where((e) => found || !(found = (e == listener)))
          .toList();
      return true;
    } else {
      return false;
    }
  }

  void recordPropertyUpdate(String propertyName, newValue, oldValue) {
    recordEvent(new ChangeEvent.property(
        this, propertyName, newValue, oldValue));
  }

  void recordListUpdate(int index, newValue, oldValue) {
    recordEvent(new ChangeEvent.list(
        this, ChangeEvent.UPDATE, index, newValue, oldValue));
  }

  void recordListInsert(int index, newValue) {
    recordEvent(new ChangeEvent.list(
        this, ChangeEvent.INSERT, index, newValue, null));
  }

  void recordListRemove(int index, oldValue) {
    recordEvent(new ChangeEvent.list(
        this, ChangeEvent.REMOVE, index, null, oldValue));
  }

  void recordGlobalChange() {
    recordEvent(new ChangeEvent.global(this));
  }

  void recordEvent(ChangeEvent event) {
    // Bail if no one cares about the event.
    if (!isObserved) {
      return;
    }

    if (EventBatch.current != null) {
      // Already in a batch, so just add it.
      assert (!EventBatch.current.sealed);
      // TODO(sigmund): measure the performance implications of this indirection
      // and consider whether caching the summary object in this instance helps.
      var summary = EventBatch.current.getEvents(this);
      summary.addEvent(event);
    } else {
      // Not in a batch, so create a one-off one.
      // TODO(rnystrom): Needing to do ignore and (null) here is awkward.
      EventBatch.wrap((ignore) { recordEvent(event); })(null);
    }
  }
}

/** A growable list that fires events when it's modified. */
class ObservableList<T>
    extends AbstractObservable
    implements List<T>, Observable {

  /** Underlying list. */
  // TODO(rnystrom): Make this final if we get list.remove().
  List<T> _internal;

  ObservableList([Observable parent = null])
    : super(parent), _internal = new List<T>();

  T operator [](int index) => _internal[index];

  void operator []=(int index, T value) {
    recordListUpdate(index, value, _internal[index]);
    _internal[index] = value;
  }

  int get length => _internal.length;

  void set length(int value) {
    _internal.length = value;
    recordGlobalChange();
  }

  void clear() {
    _internal.clear();
    recordGlobalChange();
  }

  List<T> get reversed => new ReversedListView<T>(this, 0, null);

  void sort([int compare(var a, var b)]) {
    if (compare == null) compare = Comparable.compare;
    _internal.sort(compare);
    recordGlobalChange();
  }

  void add(T element) {
    recordListInsert(length, element);
    _internal.add(element);
  }

  void addLast(T element) {
    add(element);
  }

  void addAll(Iterable<T> elements) {
    for (T element in elements) {
      add(element);
    }
  }

  int push(T element) {
    recordListInsert(length, element);
    _internal.add(element);
    return _internal.length;
  }

  T get first => _internal.first;
  T get last => _internal.last;
  T get single => _internal.single;

  T min([int compare(T a, T b)]) => _internal.min(compare);
  T max([int compare(T a, T b)]) => _internal.max(compare);

  T removeLast() {
    final result = _internal.removeLast();
    recordListRemove(length, result);
    return result;
  }

  T removeAt(int index) {
    int i = 0;
    T found = null;
    _internal = _internal.where((element) {
      if (i++ == index) {
        found = element;
        return false;
      }
      return true;
    }).toList();
    if (found != null) {
      recordListRemove(index, found);
    }
    return found;
  }

  int indexOf(T element, [int start = 0]) {
    return _internal.indexOf(element, start);
  }

  int lastIndexOf(T element, [int start = null]) {
    if (start == null) start = length - 1;
    return _internal.lastIndexOf(element, start);
  }

  bool removeFirstElement(T element) {
    // the removeAt above will record the event.
    return (removeAt(indexOf(element, 0)) != null);
  }

  int removeAllElements(T element) {
    int count = 0;
    for (int i = 0; i < length; i++) {
      if (_internal[i] == element) {
        // the removeAt above will record the event.
        removeAt(i);
        // adjust index since remove shifted elements.
        i--;
        count++;
      }
    }
    return count;
  }

  void copyFrom(List<T> src, int srcStart, int dstStart, int count) {
    Arrays.copy(src, srcStart, this, dstStart, count);
  }

  void setRange(int start, int length, List from, [int startFrom = 0]) {
    throw new UnimplementedError();
  }

  void removeRange(int start, int length) {
    throw new UnimplementedError();
  }

  void insertRange(int start, int length, [initialValue = null]) {
    throw new UnimplementedError();
  }

  List getRange(int start, int length) {
    throw new UnimplementedError();
  }

  bool contains(T element) {
    throw new UnimplementedError();
  }

  dynamic reduce(var initialValue,
                 dynamic combine(var previousValue, T element)) {
    throw new UnimplementedError();
  }


  // Iterable<T>:
  Iterator<T> get iterator => _internal.iterator;

  // Collection<T>:
  Iterable<T> where(bool f(T element)) => _internal.where(f);
  Iterable mappedBy(f(T element)) => _internal.mappedBy(f);
  bool every(bool f(T element)) => _internal.every(f);
  bool any(bool f(T element)) => _internal.any(f);
  void forEach(void f(T element)) { _internal.forEach(f); }
  String join([String separator]) => _internal.join(separator);
  T firstMatching(bool test(T value), {T orElse()}) {
    return _internal.firstMatching(test, orElse: orElse);
  }
  T lastMatching(bool test(T value), {T orElse()}) {
    return _internal.lastMatching(test, orElse: orElse);
  }
  T singleMatching(bool test(T value)) {
    return _internal.singleMatching(test);
  }
  T elementAt(int index) {
    return _internal.elementAt(index);
  }

  bool get isEmpty => length == 0;
}

// TODO(jmesserly): is this too granular? Other similar systems make whole
// classes observable instead of individual fields. The memory cost of having
// every field effectively boxed, plus having a listeners list is likely too
// much. Also, making a value observable necessitates adding ".value" to lots
// of places, and constructing all fields with the verbose
// "new ObservableValue<DataType>(myValue)".
/** A wrapper around a single value whose change can be observed. */
class ObservableValue<T> extends AbstractObservable {
  ObservableValue(T value, [Observable parent = null])
    : super(parent), _value = value;

  T get value => _value;

  void set value(T newValue) {
    // Only fire on an actual change.
    if (!identical(newValue, _value)) {
      final oldValue = _value;
      _value = newValue;
      recordPropertyUpdate("value", newValue, oldValue);
    }
  }

  T _value;
}
