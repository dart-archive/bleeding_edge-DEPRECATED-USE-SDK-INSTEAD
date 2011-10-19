// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class CSVReader extends Reader {

  CSVReader() : super() { }

  void loadSpreadsheet(Spreadsheet spreadsheet, List<String> csv) {
    Style defaultFormat = new Style();
    // Hardcode $1000.12 format for now
    Style currencyFormat = defaultFormat.setNumericFormatByIndex(4);
    // Use % format for the interest rate
    Style percentageFormat = defaultFormat.setNumericFormatByIndex(8);
    for (int row = 0; row < csv.length; row++) {
      List<String> cells = StringUtils.split(csv[row], ",".charCodeAt(0));
      for (int col = 0; col < cells.length; col++) {
        String formula = cells[col];
        RowCol rowCol = new RowCol(row + 1, col + 1);
        CellLocation location = new CellLocation(spreadsheet, rowCol);
        spreadsheet.execute(new SetCellContentsCommand(location, formula));
        if ((row == 1 && (col == 0 || col == 3)) || (row > 2 && col > 0)) {
          spreadsheet.setCellStyle(location.rowCol, currencyFormat);
        } else if (row == 2 && col == 2) {
          spreadsheet.setCellStyle(location.rowCol, percentageFormat);
        } else {
          spreadsheet.setCellStyle(location.rowCol,  defaultFormat);
        }
      }
    }
  }

  List<String> makeExample(String id) {
    int years = 30;
    int payments = years * 12;
    List<String> csv = new List<String>(payments + 4);
    csv[0] = "\"Loan Amount\",\"Interest Rate\",\"Years\",\"Monthly Payment\"";
    csv[1] = "375000,.05375,${years},\"=ROUND((R2C1*((R2C2/12)/(1-POWER((1+(R2C2/12)),"
        + "(-12*R2C3))))),2)\"";
    csv[2] = "\"Payment #\",\"Balance\",\"Interest\",\"Principal\"";
    csv[3] = "1,\"=R2C1\",\"=(RC[-1]*R2C2)/12\",\"=R2C4-RC[-1]\"";
    for (int i = 4; i < payments + 3; i++) {
      csv[i] = "\"=R[-1]C+1\",\"=R[-1]C-R[-1]C[2]\",\"=(RC[-1]*R2C2)/12\",\"=R2C4-RC[-1]\"";
    }
    csv[payments + 3] = "\"Totals\",\"=C364+D364\",\"=SUM(C4:C363)\",\"=SUM(D4:D363)\"";
    return csv;
  }
}
