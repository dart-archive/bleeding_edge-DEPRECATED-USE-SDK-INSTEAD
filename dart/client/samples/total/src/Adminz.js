// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

var restartz = document.getElementById("restartServer");
restartz.onclick = restartServer;

var stopz = document.getElementById("stopServer");
stopz.onclick = stopServer;

var adminzReq;
var adminzStatus  = document.getElementById("adminzStatus");

var statuzTimer;

function showStatus() {
  if (adminzReq.readyState == 4) {
    if (adminzReq.status == 200) {
      adminzStatus.value = adminzReq.responseText;
    } else {
      adminzStatus.value = "Error " + adminzReq.status;
    }
  }
  clearTimer();
  statuzTimer = window.setTimeout("adminzStatus.value=''", 2000);
}

function clearTimer() {
  if (statuzTimer) {
    window.clearTimeout(statuzTimer);
    statuzTimer = null;
  }
}

function restartServer(event) {
  clearTimer();
  adminzReq = new XMLHttpRequest();
  adminzReq.onreadystatechange = showStatus;
  adminzReq.open("POST", "/adm/restart");
  adminzStatus.value = "Restarting...";
  adminzReq.send();
}

function stopServer(event) {
  clearTimer();
  adminzReq = new XMLHttpRequest();
  adminzReq.onreadystatechange = showStatus;
  adminzReq.open("POST", "/adm/stop");
  adminzStatus.value = "Stopping...";
  adminzReq.send();
}
