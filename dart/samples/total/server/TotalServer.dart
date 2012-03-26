// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("total:server");

#import("dart:io");
#import("DartCompiler.dart");
#source("GetSpreadsheet.dart");
#source("SYLKProducer.dart");

void main() {
  final String host = '0.0.0.0';
  final int port = 9090;
  
  new ServerMain.start(new TotalServer(), host, port);
}

class TotalServer extends IsolatedServer {
  final String CLIENT_DIR = '../client';
  final String OUTPUT_DIR = 'out';
  static final List<String> dartResources = const<String>[
    'CellContents.dart',
    'Cell.dart',
    'CellLocation.dart',
    'CellRange.dart',
    'Chart.dart',
    'ClientChart.dart',
    'ColorPicker.dart',
    'Command.dart',
    'ContextMenuBuilder.dart',
    'ContextMenu.dart',
    'CopyPasteManager.dart',
    'CssStyles.dart',
    'CSVReader.dart',
    'DateTimeUtils.dart',
    'DomUtils.dart',
    'Exceptions.dart',
    'Formats.dart',
    'Formula.dart',
    'Functions.dart',
    'GeneralCommand.dart',
    'HtmlTable.dart',
    'HtmlUtils.dart',
    'IdGenerator.dart',
    'IndexedValue.dart',
    'InnerMenuView.dart',
    'Logger.dart',
    'Parser.dart',
    'Picker.dart',
    'PopupHandler.dart',
    'Reader.dart',
    'RowCol.dart',
    'RowColStyle.dart',
    'Scanner.dart',
    'SelectionManager.dart',
    'ServerChart.dart',
    'Spreadsheet.dart',
    'SpreadsheetLayout.dart',
    'SpreadsheetListener.dart',
    'SpreadsheetPresenter.dart',
    'StringUtils.dart',
    'Style.dart',
    'SYLKReader.dart',
    'Total.dart',
    'TotalLib.dart',
    'UndoableAction.dart',
    'UndoStack.dart',
    'Value.dart',
    'ValuePicker.dart',
    'ZoomTracker.dart'];

  static final List<String> imageResources = const<String>[
    '123.png',
    'fake-profile-photo.png',
    'fake-profile-controls.png',
    'favicon.png',
    'graphobject.png',
    'inner-menu-bg.png',
    'objectbarbg.png',
    'tableobject.png'];

  TotalServer() : super() {
    addHandler("/",
               (HttpRequest request, HttpResponse response)
               => redirectPageHandler(request, response, "Total.html"));
    addHandler("/Total.html",
               (HttpRequest request, HttpResponse response) 
               => fileHandler(request, response, '${CLIENT_DIR}/Total.html'));
    addHandler("/dart.js",
      (HttpRequest request, HttpResponse response) 
      => fileHandler(request, response, '${OUTPUT_DIR}/dart.js'));
    addHandler("/Total.dart.js",
               (HttpRequest request, HttpResponse response) 
               => compileAndServe(request, response, 'Total.dart'));
    addHandler("/total.css",
               (HttpRequest request, HttpResponse response)
               => fileHandler(request, response, '${CLIENT_DIR}/total.css'));
    addHandler("/favicon.png",  (HttpRequest request, HttpResponse response)
               => redirectPageHandler(request, response, "img/favicon.png"));
    for (String fileName in dartResources) {
      addHandler("/$fileName",  
                (HttpRequest request, HttpResponse response)
                  => fileHandler(request, response, 
                                 "${CLIENT_DIR}/$fileName"));
    }
    for (String fileName in imageResources) {
      addHandler("/img/$fileName",  
                (HttpRequest request, HttpResponse response)
                  => fileHandler(request, response, 
                                 "${CLIENT_DIR}/img/$fileName"));
    }
    addHandler('/spreadsheet/get', GetSpreadsheet.getSample);
    addHandler('/spreadsheet/list', GetSpreadsheet.listSamples);

    addHandler("/adm/Adminz.js",
               (HttpRequest request, HttpResponse response)
                 => fileHandler(request, response, "${CLIENT_DIR}/Adminz.js"));
    addHandler("/adm/stop", stopServer);
    addHandler("/adm/restart", restartServer);
  }

  void restartServer(HttpRequest request, HttpResponse response) {
    writeData(request, response, 'Restarting, KBBS', 'text/plain');
    stop();
    print("GRACEFUL RESTART!!");
    exit(42);
  }

  void stopServer(HttpRequest request, HttpResponse response) {
    writeData(request, response, 'Exiting, KTHXBYE', 'text/plain');
    stop();
    print("GRACEFUL EXIT!!");
    exit(0);
  }

  void writeData(HttpRequest request, HttpResponse response, String message, String mimeType) {
    response.setHeader("Content-Type", mimeType);

    List<int> buffer = message.charCodes();
    response.writeList(buffer, 0, buffer.length, null);
    response.writeDone();
  }

  void compileAndServe(HttpRequest request, HttpResponse response, String fileName) {
    DartCompiler compiler = new DartCompiler('${CLIENT_DIR}/${fileName}');
    compiler.out = '${OUTPUT_DIR}/${fileName}.js';
    compiler.compile((int exitCode, String errorOutput) {
        if (exitCode == 0) {
          fileHandler(request, response, "${OUTPUT_DIR}/${fileName}.js"); 
        } else {
          print(errorOutput);
          errorOutput = errorOutput.replaceAll('&','&amp;');
          errorOutput = errorOutput.replaceAll('<','&lt;');
          errorOutput = errorOutput.replaceAll('>','&gt;');
          errorOutput = errorOutput.replaceAll('"', '&quot;');
          errorOutput = errorOutput.replaceAll("'", '&#39;');
          errorOutput = errorOutput.replaceAll('\r\n','<br>');
          errorOutput = errorOutput.replaceAll('\r','<br>');
          errorOutput = errorOutput.replaceAll('\n','<br>');
          // TODO: make error output use a Dart util to escape HTML
          String errorScript ='''
var errorSpan = document.createElement("span");
errorSpan.id = "errorSpan";
errorSpan.innerHTML = "${errorOutput}";

var errorDiv = document.createElement("div");
errorDiv.id = "errorContainer";
errorDiv.className = "errorMessage";
errorDiv.appendChild(errorSpan);

document.body.appendChild(errorDiv);
''';
          writeData(request, response, errorScript, "application/javascript");
        }
    });
  }
}

