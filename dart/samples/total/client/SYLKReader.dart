// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class SYLKReader extends Reader {

  SYLKReader() : super() { }

  void loadSpreadsheet(Spreadsheet spreadsheet, List<String> sylk) {
    int row, col;
    String contents;
    CellLocation lastLocation;

    for (int i = 0; i < sylk.length; i++) {
      List<String> parts = StringUtils.split(sylk[i], StringUtils.SEMICOLON);
      String cmd = parts[0];
      switch (cmd) {
      case "ID":
        break;
      case "B":
        for (int j = 1; j < parts.length; j++) {
          String part = parts[j];
          if (part.startsWith("X")) {
            col = Math.parseInt(part.substring(1, part.length));
            spreadsheet.setColumnCount(col);
          } else if (part.startsWith("Y")) {
            row = Math.parseInt(part.substring(1, part.length));
            spreadsheet.setRowCount(row);
          }
        }
        break;
      case "C":
        row = -1;
        col = -1;
        contents = "";
        for (int j = 1; j < parts.length; j++) {
          String part = parts[j];
          if (part.startsWith("Y")) {
            row = Math.parseInt(part.substring(1, part.length));
          } else if (part.startsWith("X")) {
            col = Math.parseInt(part.substring(1, part.length));
          } if (part.startsWith("K")) {
            contents = StringUtils.stripQuotes(part.substring(1, part.length));
          }
        }
        if (row == -1) {
          throw new RuntimeException("${i}: No row (Y) specified in ${sylk[i]}");
        }
        if (col == -1) {
          throw new RuntimeException("${i}: No col (X) specified in ${sylk[i]}");
        }
        lastLocation = new CellLocation(spreadsheet, new RowCol(row, col));
        spreadsheet.execute(new SetCellContentsCommand(lastLocation, contents));
        break;
      case "F":
        Cell lastCell = lastLocation.getCell();
        if (lastCell == null) {
          break;
        }
        Style style = lastCell.style;
        for (int j = 1; j < parts.length; j++) {
          String part = parts[j];
          if (part.startsWith("P")) {
            int formatIndex = Math.parseInt(part.substring(1, part.length));
            style = style.setNumericFormatByIndex(formatIndex);
          } else if (part.startsWith("S")) {
            switch (part[1]) {
            case "I"[0]:
              style = style.setTextFormatByIndex(Style.ITALIC);
              break;
            case "D"[0]:
              style = style.setTextFormatByIndex(Style.BOLD);
              break;
            }
          } else if (part.startsWith("F")) {
            // F;...;F<format><decimal_places><alignment>
            // Only handle alignment for now
            switch (part[3]) {
            case "L"[0]:
              style = style.setTextAlignment(Style.LEFT);
              break;
            case "C"[0]:
              style = style.setTextAlignment(Style.CENTER);
              break;
            case "R"[0]:
              style = style.setTextAlignment(Style.RIGHT);
              break;
            }
          }
        }
        spreadsheet.setCellStyle(lastLocation.rowCol, style);
        break;
      }
    }
  }

  void request(String name, void callback(String spreadsheet)) {
    String url = '/spreadsheet/get?name=$name';
    XMLHttpRequest req = new XMLHttpRequest();
    req.on.readyStateChange.add((Event event) {
        if (req.readyState != XMLHttpRequest.DONE) {
          return;
        }

        String text;
        if (req.status == 200 && req.responseText != null) {
          text = req.responseText;
        } else {
          text = 'ID;P';
        }

        try {
          callback(text);
        } catch (var e, var s) {
          print(e);
        }
      });

    req.open('GET', url, true);
    req.send();
  }
}
