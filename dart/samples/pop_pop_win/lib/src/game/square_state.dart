// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
part of pop_pop_win.game;

class SquareState {
  static const SquareState hidden = const SquareState._internal("hidden");
  static const SquareState revealed = const SquareState._internal("revealed");
  static const SquareState flagged = const SquareState._internal("flagged");
  static const SquareState bomb = const SquareState._internal("bomb");
  static const SquareState safe = const SquareState._internal('safe');
  final String name;

  const SquareState._internal(this.name);

  String toString() => 'SquareState: $name';
}
