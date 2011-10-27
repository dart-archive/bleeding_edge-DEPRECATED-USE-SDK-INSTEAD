// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("total:server");

#import("../../chat/chat_server_lib.dart");
#import("../../chat/http.dart");
#import("Dartc.dart");
#source("GetSpreadsheet.dart");
#source("SYLKProducer.dart");

void main() {
  final String host = '0.0.0.0';
  final int port = 9090;
  
  new ServerMain.start(new TotalServer(), host, port);
}

class TotalServer extends IsolatedServer {
  final String CLIENT_DIR = '../../../client/samples/total/src';
  final String OUTPUT_DIR = 'out';

  TotalServer() : super() {
    addHandler("/",
               (HTTPRequest request, HTTPResponse response)
               => redirectPageHandler(request, response, "Total.html"));
    addHandler("/Total.html",
               (HTTPRequest request, HTTPResponse response) 
               => fileHandler(request, response, '${CLIENT_DIR}/Total.html'));
    addHandler("/Total.dart.app.js",
               (HTTPRequest request, HTTPResponse response) 
               => compileAndServe(request, response, 'Total.dart'));
    addHandler("/total.css",
               (HTTPRequest request, HTTPResponse response)
               => fileHandler(request, response, '${CLIENT_DIR}/total.css'));
    addHandler("/favicon.png",  (HTTPRequest request, HTTPResponse response)
               => redirectPageHandler(request, response, "img/favicon.png"));
    for (String fileName in [
             '123.png',
             'fake-profile-photo.png',
             'fake-sandbar-controls.png',
             'favicon.png',
             'graphobject.png',
             'inner-menu-bg.png',
             'objectbarbg.png',
             'tableobject.png',]) {
      addHandler("/img/$fileName",  (HTTPRequest request, HTTPResponse response)
                 => fileHandler(request, response, "${CLIENT_DIR}/img/$fileName"));
    }
    addHandler('/spreadsheet/get', GetSpreadsheet.getSample);
    addHandler('/spreadsheet/list', GetSpreadsheet.listSamples);

    addHandler("/adm/Adminz.js",
               (HTTPRequest request, HTTPResponse response)
                 => fileHandler(request, response, "${CLIENT_DIR}/Adminz.js"));
    addHandler("/adm/stop", stopServer);
    addHandler("/adm/restart", restartServer);
  }

  void restartServer(HTTPRequest request, HTTPResponse response) {
    writeData(request, response, 'Restarting, KBBS', 'text/plain');
    stop();
    print("GRACEFUL RESTART!! TODO: make server exit gracefully");
    throw "GRACEFUL RESTART!! TODO: make server exit gracefully";
  }

  void stopServer(HTTPRequest request, HTTPResponse response) {
    writeData(request, response, 'Exiting, KTHXBYE', 'text/plain');
    stop();
    print("GRACEFUL EXIT!! TODO: make server exit gracefully");
    throw "GRACEFUL EXIT!! TODO: make server exit gracefully";
  }

  void writeData(HTTPRequest request, HTTPResponse response, String message, String mimeType) {
    response.setHeader("Content-Type", mimeType);

    List<int> buffer = message.charCodes();
    response.writeList(buffer, 0, buffer.length, null);
    response.writeDone();
  }

  void compileAndServe(HTTPRequest request, HTTPResponse response, String fileName) {
    Dartc dartc = new Dartc('${CLIENT_DIR}/${fileName}');
    dartc.work = OUTPUT_DIR;
    dartc.out = '${OUTPUT_DIR}/${fileName}.app.js';
    dartc.compile((int exitCode, String errorOutput) {
        if (exitCode == 0) {
          fileHandler(request, response, "${OUTPUT_DIR}/${fileName}.app.js"); 
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

