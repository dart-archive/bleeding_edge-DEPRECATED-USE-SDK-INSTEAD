// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

var session = null;
var nextMessage = 0;
var pollRequest = null;

function handleLoad() {
  if (document.documentMode) {
    if (document.documentMode < 8) {
      alert("IE8 or higher required!");
    }
  }
  uiJoin();
}

function handleJoin() {
  var handleValue = document.getElementById("handleInput").value;
  var joinRequest = {
     request: "join",
     handle: handleValue
  }
  sendRequest("/join", joinRequest, onJoin, onJoinFailed);
  uiJoining();
}

function handleLeave() {
  var leaveRequest = {
    request: "leave",
    sessionId: session
  }
  sendRequest("/leave", leaveRequest, onLeave, onLeaveFailed);
  uiLeaving();
}

function handlePostMessage() {
  var messageText = document.getElementById("messageInput").value;
  var messageRequest = {
     request: "message",
     sessionId: session,
     message: messageText
  }
  sendRequest("/message", messageRequest, onMessagePost, onMessagePostFailed);
}

function pollServer() {
  var receiveRequest = {
     request: "receive",
     sessionId: session,
     nextMessage: nextMessage,
     maxMessages: 10
  }
  pollRequest = sendRequest("/receive", receiveRequest, onPoll, onPollFailed);
}

function onJoin(response) {
  if (response.response == "join") {
    session = response.sessionId;
    uiChatting();
  } else {
    protocolError();
  }

  // Start polling for updates.
  pollServer();
}

function onJoinFailed() {
  showStatus("Failed to connect. Please try again later.");
  uiJoin();
}

function onLeave(response) {
  if (response.response == "leave") {
    session = null;
    uiJoin();
  } else {
    protocolError();
  }
  if (pollRequest != null) {
    var tmp = pollRequest
    pollRequest = null;
    tmp.abort();
  }
}

function onLeaveFailed() {
  showStatus("Failed to leave. Please try again later.");
  uiJoin();
}

function onMessagePost(response) {
  // Nothing to do. Messages posted will be delivered from the server.
}

function onMessagePostFailed() {
  showStatus("Failed to post message. Please try again later.");
  uiJoin();
}

function onPoll(response) {
  if (response.disconnect) {
    uiJoin();
  } else {
    var messages = response.messages;
    for (var i = 0; i < messages.length; i++) {
      uiAddMessage(messages[i]);
    }
    showStatus(response.activeUsers +
               " active users (server has been running for " +
               formatUpTime(response.upTime) +
               ")");
    nextMessage = messages[messages.length - 1].number + 1;

    // Continue to poll server while connected.
    pollRequest = null;
    if (session != null) {
      pollServer();
    }
  }
}

function onPollFailed() {
  if (pollRequest != null) {
    showStatus("Failed to receive messages. Please try again later.");
    uiJoin();
  }
}

function sendRequest(url, json, onSuccess, onError) {
  request = new XMLHttpRequest();
  request.onreadystatechange = function () {
    if (this.readyState != 4) return;
    if (this.status == 200) {
      onSuccess(JSON.parse(this.responseText));
    } else {
      onError();
    }
  };
  request.open("POST", url);
  request.setRequestHeader("Content-Type", "text/plain;charset=UTF-8");
  request.send(JSON.stringify(json));
  return request;
}

function uiJoin() {
  enableButton("joinButton");
  showElement("joinSection");
  hideElement("chatSection");
  nextMessage = 0;
  var messages = document.getElementById("messages");
  while (messages.firstChild) {
    messages.removeChild(messages.firstChild);
  }
  showStatus("Welcome to dartty chat. " +
             "This chat service is build using server side Dart. " +
             "Enter your handle to join.");
}

function uiJoining() {
  disableButton("joinButton");
  showElement("joinSection");
  hideElement("chatSection");
  showStatus("Joining...");
}

function uiLeaving() {
  disableButton("postButton");
  disableButton("leaveButton");
  hideElement("joinSection");
  showElement("chatSection");
  showStatus("Leaving...");
}

function uiChatting() {
  disableButton("joinButton");
  enableButton("postButton");
  enableButton("leaveButton");
  hideElement("joinSection");
  showElement("chatSection");
  showStatus("Status...");
}

function uiAddMessage(message) {
  var messages = document.getElementById("messages");
  p = window.document.createElement('p');
  var formattedTime = formatMessageTime(message["received"]);
  var text = formattedTime + " " + message["from"] + " ";
  if (message["type"] == "join") {
    text += "joined";
  } else if (message["type"] == "message") {
    text += message["message"];
  } else if (message["type"] == "left") {
    text += "left";
  } else {
    text += "timeout";
  }
  p.innerHtml = text;
  if (messages.firstChild == null) {
    messages.appendChild(p);
  } else {
    messages.insertBefore(p, messages.firstChild);
  }
  if (messages.childNodes.length > 20) {
    messages.removeChild(messages.lastChild);
  }
}

function formatMessageTime(received) {
  var date = new Date(received);
  var formattedTime = "";
  if (date.getHours() < 10) formattedTime += "0";
  formattedTime += date.getHours().toString();
  formattedTime += ":";
  if (date.getMinutes() < 10) formattedTime += "0";
  formattedTime += date.getMinutes().toString();
  formattedTime += ":";
  if (date.getSeconds() < 10) formattedTime += "0";
  formattedTime += date.getSeconds().toString();
  return formattedTime;
}

function formatUpTime(upTime) {
  upTime = Math.floor(upTime / 1000);
  var hours = Math.floor(upTime / (60 * 60));
  upTime = upTime % (60 * 60);
  var minutes = Math.floor(upTime / 60);
  upTime = upTime % 60;
  var seconds = upTime;
  var formattedTime = "";
  if (hours < 10) formattedTime += "0";
  formattedTime += hours;
  formattedTime += ":";
  if (minutes < 10) formattedTime += "0";
  formattedTime += minutes;
  formattedTime += ":";
  if (seconds < 10) formattedTime += "0";
  formattedTime += seconds;
  return formattedTime;
}

function protocolError() {
  uiJoin();
  showStatus("Protocol error!");
}

function showStatus(status) {
  var statusText = document.getElementById("statusText");
  statusText.innerText = status;
}

function showElement(id) {
  var element = document.getElementById(id);
  element.style.display = 'inline';
}

function hideElement(id) {
  var element = document.getElementById(id);
  element.style.display = 'none';
}

function enableButton(id) {
  var element = document.getElementById(id);
  element.disabled = false;
}

function disableButton(id) {
  var element = document.getElementById(id);
  element.disabled = true;
}

// Debugging utility functions.
function writeReadyState(request) {
  p = window.document.createElement('p');
  var readyStateName;
  switch (request.readyState) {
    case 0: readyStateName = "UNSENT"; break;
    case 1: readyStateName = "OPENED"; break;
    case 2: readyStateName = "HEADERS_RECEIVED"; break;
    case 3: readyStateName = "LOADING"; break;
    case 4: readyStateName = "DONE"; break;
  }
  p.innerHtml =
      "Ready state " + readyStateName + " (" + request.readyState + ")";
  window.document.body.appendChild(p);
}
