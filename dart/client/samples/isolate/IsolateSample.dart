// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * These are the messages we are going to send to the isolates
 * that we create.
 */
class MessageId {
  static final INIT = "init";
  static final GREETING = "greeting";
}

/**
 * This is a simple sample application showing how to create two isolates
 * and send messages to them, and receive replies back.
 */
class IsolateSample  {

  /**
   * map from isolate name to the port used to send messages to that
   * isolate.
   */
  final Map<String, SendPort> ports;

  /**
   * Port that the main isolate will use to receive occasional
   * "chirp" messages that are sent when the user presses the "chirp"
   * button on one of the isolates.
   */
  final ReceivePort chirpPort;

  IsolateSample() : ports = new Map(), chirpPort = new ReceivePort() {}

  /**
   * Create a new isolate with a given name.  (In this sample app
   * we only have two isolates, and they are named 'A' and 'B').
   * Note, isolates aren't normally named, but it's useful to give
   * them names in this app so we can show which isolate is doing
   * what.
   */
  void createIsolate(String name) {
    new DemoIsolate().spawn().then((SendPort port) {
      var message = { "id" : MessageId.INIT, "args" : [name, chirpPort] };
      port.call(message);
      ports[name] = port;
    });
  }

  /**
   * DOM is loaded, so do initialization and set up event handlers.
   */
  void ready() {

    document.query("#appTitle").text = "Hello, isolates.";
    document.query("#vmStatus").text = isVm().toString();

    Element replyElement =
        document.query(".isolateMain .replyText");

    createIsolate("A");
    createIsolate("B");

    for (Element element in document.queryAll(".sendButton")) {
      element.on.click.add((Event e) {
        replyElement.text = "waiting for reply...";

        // get the last letter on the button (assuming the button text is, for
        // example, "send message to A".
        String buttonText = e.currentTarget.dynamic.attributes["value"];
        String isolateName = buttonText[buttonText.length - 1];
        String greeting = document.query("#greetingText").dynamic.value;
        var message = { "id": MessageId.GREETING, "args" : [ greeting ] };
        ports[isolateName].call(message).receive(
            (var message, SendPort replyTo) {
              replyElement.text = message;
        });
      });
    }

    chirpPort.receive((var message, SendPort replyTo) {
      replyElement.text = message;
    });
  }

  static void main() {
    Dom.ready(void _() { new IsolateSample().ready(); });
  }

  // TODO(mattsh) get this off the System object once it's available
  // see http://b/issue?id=5215916
  static bool isVm() {
    return 1234567890123456789 % 2 > 0;
  }
}

/**
 * Each instance of this class runs in a separate isolate, so it doesn't
 * share any program state with the main isolate.  In this app it
 * runs on the main UI thread (so it can interact with the window and
 * document).
 */
class DemoIsolate extends Isolate {

  Element div;
  String isolateName;
  SendPort chirpPort;

  DemoIsolate() : super.light() {}

  void main() {
    this.port.receive((message, SendPort replyTo) {
      switch(message["id"]) {
        case MessageId.INIT:
          init(message["args"][0], message["args"][1]);
          break;
        case MessageId.GREETING:
          greeting(message["args"][0], replyTo);
          break;
      }
    });
  }

  void init(String isolateName, SendPort chirpPort) {
    this.isolateName = isolateName;
    this.chirpPort = chirpPort;
    div = document.createElement("div");
    div.classes = ["isolate", "isolate${isolateName}"];
    div.innerHTML = document.query("#isolateTemplate").
        firstElementChild.dynamic.innerHTML;
    div.query(".isolateName").text = isolateName;
    document.query("#isolateParent").nodes.add(div);
    div.query(".chirpButton").on.click.add(
        void _(Event) { chirpPort.call(
              "this is a chirp message from isolate " + isolateName);
    }, false);
  }

  /**
   * Display the message we received, and send back a simple reply (unless
   * the user has unchecked the reply checkbox).
   */
  void greeting(String message, SendPort replyTo) {
      div.query(".messageBox").dynamic.innerHTML =
        "received message: <span class='messageText'>'${message}'</span>";
      if (div.query(".replyCheckbox").dynamic.checked) {
        InputElement element = div.query(".delayTextbox");
        int millis = Math.parseInt(element.value);
        window.setTimeout(() {
          replyTo.send("this is a reply from isolate '${isolateName}'", null);
        }, millis);
      }
  }
}
