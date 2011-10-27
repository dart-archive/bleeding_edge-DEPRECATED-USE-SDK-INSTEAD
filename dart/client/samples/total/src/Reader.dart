// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Reader {
  Reader() { }

  void loadFromString(Spreadsheet spreadsheet, String data) {
    List<String> parts = StringUtils.split(data, StringUtils.NEWLINE);
    loadSpreadsheet(spreadsheet, parts);
  }

  void loadSpreadsheet(Spreadsheet spreadsheet, List<String> data) { }
}

