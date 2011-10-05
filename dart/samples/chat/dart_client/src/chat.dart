// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Chat {

  static void main(Window window) {
    new Chat(window).run();
  }

  Chat(this._window) {
  }

  void run() {
    _window.addEventListener(
        "DOMContentLoaded", void _(Event event) { ready(); } , false);
  }

  void ready() {
    HTMLDocument doc = _window.document;
    _joinButton = doc.getElementById("joinButton");
    _leaveButton = doc.getElementById("leaveButton");
    _postButton = doc.getElementById("postButton");
    _handleInput = doc.getElementById("handleInput");
    _joinSection = doc.getElementById("joinSection");
    _chatSection = doc.getElementById("chatSection");
    _messageInput = doc.getElementById("messageInput");
    _messages = doc.getElementById("messages");
    _statusText = doc.getElementById("statusText");
    _joinButton.onclick =  void _(Event e) { handleJoin(); };
    _leaveButton.onclick = void _(Event e) { handleLeave(); };
    _postButton.onclick = void _(Event e) { handlePostMessage(); };
    uiJoin();
  }

  void handleJoin() {
    var handleValue = _handleInput.value;
    var joinRequest = new Map();
    joinRequest["request"] = "join";
    joinRequest["handle"] = handleValue;
    sendRequest("/join",
                joinRequest,
                void _(Map response) { onJoin(response); },
                void _() { onJoinFailed(); });
    uiJoining();
  }

  void handleLeave() {
    var leaveRequest = new Map();
    leaveRequest["request"] = "leave";
    leaveRequest["sessionId"] = _session;
    sendRequest("/leave",
                leaveRequest,
                void _(Map response) { onLeave(response); },
                void _() { onLeaveFailed(); });
    uiLeaving();
  }

  void handlePostMessage() {
    var messageText = _messageInput.value;
    var messageRequest = new Map();
    messageRequest["request"] = "message";
    messageRequest["sessionId"] = _session;
    messageRequest["message"] = messageText;
    sendRequest("/message",
                messageRequest,
                void _(Map response) { onMessagePost(response); },
                void _() { onMessagePostFailed(); });
  }

  void pollServer() {
    var receiveRequest = new Map();
    receiveRequest["request"] = "receive";
    receiveRequest["sessionId"] = _session;
    receiveRequest["nextMessage"] = _nextMessage;
    receiveRequest["maxMessages"] = 10;
    _pollRequest = sendRequest("/receive",
                               receiveRequest,
                               void _(Map response) { onPoll(response); },
                               void _() { onPollFailed(); });
  }

  void onJoin(Map response) {
    if (response["response"] == "join") {
      _session = response["sessionId"];
      uiChatting();
    } else {
      protocolError();
    }

    // Start polling for updates.
    pollServer();
  }

  void onJoinFailed() {
    showStatus("Failed to connect. Please try again later.");
    uiJoin();
  }

  void onLeave(Map response) {
    if (response["response"] == "leave") {
      _session = null;
      uiJoin();
    } else {
      protocolError();
    }
    if (_pollRequest != null) {
      _pollRequest.abort();
    }
  }

  void onLeaveFailed() {
    showStatus("Failed to leave. Please try again later.");
    uiJoin();
  }

  void onMessagePost(Map response) {
    // Nothing to do. Messages posted will be delivered from the server.
  }

  void onMessagePostFailed() {
    showStatus("Failed to post message. Please try again later.");
    uiJoin();
  }

  void onPoll(Map response) {
    if (response["disconnect"]) {
      uiJoin();
    } else {
      List<Map<String, Object>> messages = response["messages"];
      for (var i = 0; i < messages.length; i++) {
        uiAddMessage(messages[i]);
      }

      int activeUsers = response["activeUsers"];
      showStatus("$activeUsers active users (server has been running for " +
                 formatUpTime(response["upTime"]) +
                 ")");

      int index = messages[messages.length - 1]["number"];
      _nextMessage = index + 1;

      // Continue to poll server while connected.
      _pollRequest = null;
      if (_session != null) {
        pollServer();
      }
    }
  }

  void onPollFailed() {
    showStatus("Failed to receive messages. Please try again later.");
    uiJoin();
  }

  XMLHttpRequest sendRequest(String url, Map json, var onSuccess, var onError) {
    XMLHttpRequest request = _window.createXMLHttpRequest();
    request.onreadystatechange = void _(Event event) {
      if (request.readyState != 4) return;
      if (request.status == 200) {
        onSuccess(JSON.parse(request.responseText));
      } else {
        onError();
      }
    };
    request.open("POST", url, true);
    request.setRequestHeader("Content-Type", "text/plain;charset=UTF-8");
    request.send(JSON.stringify(json));
    return request;
  }

  void uiJoin() {
    enableButton(_joinButton);
    showElement(_joinSection);
    hideElement(_chatSection);
    _nextMessage = 0;
    while (_messages.firstChild != null) {
      _messages.removeChild(_messages.firstChild);
    }
    showStatus("Welcome to dartty chat. " +
               "This chat service is build using Dart for both the server and the client. " +
               "Enter your handle to join.");
  }

  void uiJoining() {
    disableButton(_joinButton);
    showElement(_joinSection);
    hideElement(_chatSection);
    showStatus("Joining...");
  }

  void uiLeaving() {
    disableButton(_postButton);
    disableButton(_leaveButton);
    hideElement(_joinSection);
    showElement(_chatSection);
    showStatus("Leaving...");
  }

  void uiChatting() {
    disableButton(_joinButton);
    enableButton(_postButton);
    enableButton(_leaveButton);
    hideElement(_joinSection);
    showElement(_chatSection);
    showStatus("Status...");
  }

  void uiAddMessage(Map message) {
    HTMLParagraphElement p = _window.document.createElement('p');
    String formattedTime = formatMessageTime(message["received"]);
    String from = message["from"];
    String text = "$formattedTime $from ";
    if (message["type"] == "join") {
      text += "joined";
    } else if (message["type"] == "message") {
      text += message["message"];
    } else if (message["type"] == "left") {
      text += "left";
    } else {
      text += "timeout";
    }
    p.innerHTML = text;
    if (_messages.firstChild == null) {
      _messages.appendChild(p);
    } else {
      _messages.insertBefore(p, _messages.firstChild);
    }
    if (_messages.childNodes.length > 20) {
      _messages.removeChild(_messages.lastChild);
    }
  }

  String formatMessageTime(String received) {
    DateTime dateTime = new DateTime.fromString(received);
    String formattedTime = "";
    if (dateTime.time.hours < 10) formattedTime += "0";
    formattedTime += dateTime.time.hours.toString();
    formattedTime += ":";
    if (dateTime.time.minutes < 10) formattedTime += "0";
    formattedTime += dateTime.time.minutes.toString();
    formattedTime += ":";
    if (dateTime.time.seconds < 10) formattedTime += "0";
    formattedTime += dateTime.time.seconds.toString();
    return formattedTime;
  }

  String formatUpTime(int upTime) {
    upTime = (upTime / 1000).floor();
    int hours = (upTime / (60 * 60)).floor();
    upTime = upTime % (60 * 60);
    int minutes = (upTime / 60).floor();
    upTime = upTime % 60;
    int seconds = upTime;
    String formattedTime = "";
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

  void protocolError() {
    uiJoin();
    showStatus("Protocol error!");
  }

  void showStatus(status) {
    _statusText.innerText = status;
  }

  void showElement(Element element) {
    element.style.setProperty("display", "inline");
  }

  void hideElement(Element element) {
    element.style.setProperty("display", "none");
  }

  void enableButton(HTMLButtonElement element) {
    element.disabled = false;
  }

  void disableButton(HTMLButtonElement element) {
    element.disabled = true;
  }

  void write(String message) {
    HTMLDocument doc = _window.document;
    HTMLParagraphElement p = doc.createElement('p');
    p.innerText = message;
    doc.body.appendChild(p);
  }

  final Window _window;
  HTMLButtonElement _joinButton;
  HTMLButtonElement _leaveButton;
  HTMLButtonElement _postButton;
  HTMLInputElement _handleInput;

  HTMLElement _joinSection;
  HTMLElement _chatSection;
  HTMLInputElement _messageInput;
  HTMLElement _messages;
  HTMLElement _statusText;

  String _session = null;
  int _nextMessage = 0;
  XMLHttpRequest _pollRequest = null;

}
