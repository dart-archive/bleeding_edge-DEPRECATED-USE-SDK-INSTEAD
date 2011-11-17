// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface Link<T> extends Iterable<T> factory LinkFactory {
  final T head;
  final Link<T> tail;

  Link(T head, [Link<T> tail]);
  Link.fromList(List<T> list);

  Link<T> prepend(T element);
  List<T> toList();
  bool isEmpty();
  Link<T> reverse();

  void printOn(StringBuffer buffer, [separatedBy]);
}

interface EmptyLink<T> extends Link<T> factory LinkTail<T> {
  const EmptyLink();
}

interface LinkBuilder<T> factory LinkBuilderImplementation<T> {
  LinkBuilder();

  Link<T> toLink();
  void addLast(T t);
}
