// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library searchable_list.searchable_list;

import 'package:polymer/polymer.dart';

@CustomTag('searchable-list')
class SearchableList extends PolymerElement {
  @observable bool applyAuthorStyles = true;
  @observable String searchParam;
  @published List<String> data = [];
  final List<String> results = toObservable([]);

  SearchableList.created() : super.created();

  attached() {
    super.attached();
    results.addAll(data);
    onPropertyChange(this, #searchParam, search);
  }

  search() {
    results.clear();
    String lower = searchParam.toLowerCase();
    results.addAll(data.where((d) => d.toLowerCase().contains(lower)));
  }
}
