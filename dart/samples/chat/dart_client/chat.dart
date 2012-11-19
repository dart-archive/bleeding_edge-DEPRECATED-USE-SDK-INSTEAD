// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library chat;
import 'dart:html';
import 'dart:json';

void main() {
  new Chat().start();
}

class Chat {
  void start() {
    Document doc = window.document;
    _joinButton = doc.query("#joinButton");
    _leaveButton = doc.query("#leaveButton");
    _postButton = doc.query("#postButton");
    _handleInput = doc.query("#handleInput");
    _joinSection = doc.query("#joinSection");
    _chatSection = doc.query("#chatSection");
    _messageInput = doc.query("#messageInput");
    _messages = doc.query("#messages");
    _statusText = doc.query("#statusText");
    _joinButton.on.click.add(handleJoin);
    _leaveButton.on.click.add(handleLeave);
    _postButton.on.click.add(handlePostMessage);
    _handleInput.on.keyDown.add(handleInputKeyDown);
    _messageInput.on.keyDown.add(messageInputKeyDown);
    uiJoin();
  }

  void handleJoin(Event e) {
    var handleValue = _handleInput.value;
    var joinRequest = new Map();
    joinRequest["request"] = "join";
    joinRequest["handle"] = handleValue;
    sendRequest("/join",
                joinRequest,
                (Map response) => onJoin(response),
                () => onJoinFailed());
    uiJoining();
  }

  void handleLeave(Event e) {
    var leaveRequest = new Map();
    leaveRequest["request"] = "leave";
    leaveRequest["sessionId"] = _session;
    sendRequest("/leave",
                leaveRequest,
                (Map response) => onLeave(response),
                () => onLeaveFailed());
    uiLeaving();
  }

  void handlePostMessage(Event e) {
    var messageText = _messageInput.value;
    _messageInput.value = "";
    var messageRequest = new Map();
    messageRequest["request"] = "message";
    messageRequest["sessionId"] = _session;
    messageRequest["message"] = messageText;
    sendRequest("/message",
                messageRequest,
                (Map response) => onMessagePost(response),
                () => onMessagePostFailed());
  }

  void handleInputKeyDown(UIEvent e) {
    if (e.keyCode == 13) handleJoin(e);
  }

  void messageInputKeyDown(UIEvent e) {
    if (e.keyCode == 13) handlePostMessage(e);
  }

  void pollServer() {
    var receiveRequest = new Map();
    receiveRequest["request"] = "receive";
    receiveRequest["sessionId"] = _session;
    receiveRequest["nextMessage"] = _nextMessage;
    receiveRequest["maxMessages"] = 10;
    _pollRequest = sendRequest("/receive",
                               receiveRequest,
                               (Map response) => onPoll(response),
                               () => onPollFailed());
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
      showStatus("$activeUsers active users (server has been running for "
                 "${formatUpTime(response["upTime"])})");

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

  HttpRequest sendRequest(String url, Map json, var onSuccess, var onError) {
    HttpRequest request = new HttpRequest();
    request.on.readyStateChange.add((Event event) {
      if (request.readyState != 4) return;
      if (request.status == 200) {
        onSuccess(JSON.parse(request.responseText));
      } else {
        onError();
      }
    });
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
    _messages.children.clear();
    showStatus("Welcome to dart chat sample. "
               "This chat service is build using Dart for both the server and the client. "
               "Enter your handle to join.");
    _handleInput.focus();
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
    _messageInput.focus();
    showStatus("Status...");
  }

  void uiAddMessage(Map message) {
    ParagraphElement p = new Element.tag('p');
    String formattedTime = formatMessageTime(message["received"]);
    String from = message["from"];
    StringBuffer text = new StringBuffer("$formattedTime $from ");
    if (message["type"] == "join") {
      text.add("joined");
    } else if (message["type"] == "message") {
      text.add(message["message"]);
    } else if (message["type"] == "leave") {
      text.add("left");
    } else {
      text.add("timeout");
    }
    p.text = text.toString();
    _messages.insertAdjacentElement('afterBegin', p);
    if (_messages.children.length > 20) {
      _messages.children.removeLast();
    }
  }

  String formatMessageTime(String received) {
    Date date = new Date.fromString(received);
    StringBuffer formattedTime = new StringBuffer();
    if (date.hour < 10) formattedTime.add("0");
    formattedTime.add(date.hour);
    formattedTime.add(":");
    if (date.minute < 10) formattedTime.add("0");
    formattedTime.add(date.minute);
    formattedTime.add(":");
    if (date.second < 10) formattedTime.add("0");
    formattedTime.add(date.second);
    return formattedTime.toString();
  }

  String formatUpTime(int upTime) {
    var upTime = (upTime ~/ 1000);
    int hours = (upTime ~/ (60 * 60));
    upTime = upTime % (60 * 60);
    int minutes = (upTime ~/ 60);
    upTime = upTime % 60;
    int seconds = upTime;
    StringBuffer formattedTime = new StringBuffer();
    if (hours < 10) formattedTime.add("0");
    formattedTime.add(hours);
    formattedTime.add(":");
    if (minutes < 10) formattedTime.add("0");
    formattedTime.add(minutes);
    formattedTime.add(":");
    if (seconds < 10) formattedTime.add("0");
    formattedTime.add(seconds);
    return formattedTime.toString();
  }

  void protocolError() {
    uiJoin();
    showStatus("Protocol error!");
  }

  void showStatus(status) {
    _statusText.text = status;
  }

  void showElement(Element element) {
    element.style.setProperty("display", "inline");
  }

  void hideElement(Element element) {
    element.style.setProperty("display", "none");
  }

  void enableButton(ButtonElement element) {
    element.disabled = false;
  }

  void disableButton(ButtonElement element) {
    element.disabled = true;
  }

  void write(String message) {
    Document doc = window.document;
    ParagraphElement p = new Element.tag('p');
    p.text = message;
    doc.body.children.add(p);
  }

  ButtonElement _joinButton;
  ButtonElement _leaveButton;
  ButtonElement _postButton;
  InputElement _handleInput;

  Element _joinSection;
  Element _chatSection;
  InputElement _messageInput;
  Element _messages;
  Element _statusText;

  String _session = null;
  int _nextMessage = 0;
  HttpRequest _pollRequest = null;

}
