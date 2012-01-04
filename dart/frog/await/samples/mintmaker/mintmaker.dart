// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Copied from MintMakerRpcTest (under tests/isolate/src/).

#library('mintmaker');

class Mint {
  Mint();
  Purse createPurse(int initialBalance) {
    return new Purse(initialBalance);
  }
}

class Purse {
  int _balance;

  Purse(this._balance) {}

  int queryBalance() {
    return _balance;
  }

  int deposit(int amount, Purse source) {
    if (source._balance < amount) {
      throw "OverdraftException";
    }
    source._balance -= amount;
    _balance += amount;
    return _balance;
  }
}


class MintProxy extends RpcProxy {
  MintProxy(Future<SendPort> sendPort) : super(sendPort) { }
  Future<PurseProxy> createPurse(int initialBalance) {
    return sendCommand("createPurse", [initialBalance], (sendPort) {
      Completer<SendPort> completer = new Completer();
      completer.complete(sendPort);
      return new PurseProxy(completer.future);
    });
  }
  Future<String> close() {
    return sendCommand("close", null, null);
  }
}

class MintReceiver extends RpcReceiver<Mint> {
  MintReceiver(ReceivePort receivePort) : super(new Mint(), receivePort) {}
  Object receiveCommand(String command, List args) {
    switch(command) {
      case "createPurse":
        int balance = args[0];
        Purse purse = target.createPurse(balance);
        return new PurseReceiver(purse, new ReceivePort());
      case "close":
        RpcReceiver.closeAll();
        return "close command processed";
      default:
          throw "MintReceiver unrecognized command";
      }
  }
}

class PurseProxy extends RpcProxy {
  PurseProxy(Future<SendPort> sendPort) : super(sendPort) { }
  Future<int> queryBalance() {
    return sendCommand("queryBalance", null, null);
  }
  Future<int> deposit(int amount, PurseProxy from) {
    return sendCommand("deposit", [amount, from], null);
  }
}

class PurseReceiver extends RpcReceiver<Purse> {

  PurseReceiver(Purse purse, ReceivePort receivePort) : super(purse, receivePort) {}

  Object receiveCommand(String command, List args) {
    switch(command) {
      case "queryBalance":
        return target.queryBalance();
      case "deposit":
        int amount = args[0];
        Purse fromPurse = args[1];
        return target.deposit(amount, fromPurse);
      default:
        throw "PurseReceiver unrecognized command";
    }
  }
}

class MintIsolate extends Isolate {
  MintIsolate() : super.light() {}

  MintProxy open() {
    return new MintProxy(spawn());
  }

  void main() {
    ReceivePort receivePort = port;
    new MintReceiver(receivePort);
  }
}
