// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library searchable_list.demo_app;

import 'package:polymer/polymer.dart';

@CustomTag('demo-app')
class DemoApp extends PolymerElement {
  @observable bool applyAuthorStyles = true;
  List<String> fruits = const [
      'Apple', 'Apricot', 'Avocado',
      'Banana', 'Blackberry', 'Blackcurrant', 'Blueberry',
      'Currant', 'Cherry', 'Clementine', 'Date', 'Durian', 'Fig',
      'Gooseberry', 'Grape', 'Grapefruit', 'Guava', 'Huckleberry',
      'Kiwi', 'Lemon', 'Lime', 'Lychee', 'Mandarin', 'Mango',
      'Cantaloupe', 'Honeydew melon', 'Nectarine', 'Orange',
      'Peach', 'Pear', 'Plum', 'Pineapple', 'Pomegranate',
      'Raspberry', 'Redcurrant', 'Star fruit', 'Strawberry',
      'Tangerine', 'Tomato', 'Watermelon'];

  DemoApp.created() : super.created();
}
