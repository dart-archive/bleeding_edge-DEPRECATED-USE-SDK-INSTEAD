// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library jsonp_sample;

import 'dart:html';
import 'dart:js';

const List<String> FIELDS = const ['name', 'description', 'size',
                                   'watchers', 'forks'];

TableElement table = querySelector('#repo-table');

addTableHeadRow() {
  var tr = new TableRowElement();
  for (var field in FIELDS) {
    tr.append(new Element.tag('th')..text = field);
  }
  table.querySelector('thead').append(tr);
}

addTableBodyRow(JsObject repo) {
  var tr = new TableRowElement();
  for (var field in FIELDS) {
    var td = new TableCellElement();
    if (field == 'name') {
      td.append(new AnchorElement()
          ..href = repo['html_url']
          ..text = repo[field]);
    } else {
      td.text = repo[field].toString();
    }
    tr.append(td);
  }
  table.querySelector('tbody').append(tr);
}

void main() {
  // Create a jsObject to handle the response.
  context['processData'] = (response) {
    addTableHeadRow();
    for (var repo in response['data']) {
      addTableBodyRow(repo);
    }
  };

  ScriptElement script = new Element.tag("script");
  script.src = "https://api.github.com/users/dart-lang/repos?callback=processData";
  document.body.children.add(script);
}
