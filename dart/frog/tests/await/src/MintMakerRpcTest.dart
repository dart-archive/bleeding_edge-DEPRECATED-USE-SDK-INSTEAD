// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Copy of MintMakerRpcTest (under tests/isolate/src/) but rewritten using
// await.

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


main() {
  print("starting test");
  MintProxy mint = new MintIsolate().open();
  PurseProxy purse1 = await mint.createPurse(100);
  int balance = await purse1.queryBalance();
  Expect.equals(100, balance);

  PurseProxy purse2 = await mint.createPurse(0);
  balance = await purse2.queryBalance();
  Expect.equals(0, balance);

  balance = await purse2.deposit(5, purse1);
  Expect.equals(0 + 5, balance);

  balance = await purse1.queryBalance();
  Expect.equals(100 - 5, balance);

  balance = await purse2.deposit(42, purse1);
  int balance2 = balance;
  Expect.equals(0 + 5 + 42, balance2);

  balance = await purse1.queryBalance();
  int balance1 = balance;
  Expect.equals(100 - 5 - 42, balance);

  // Now try to deposit more money into purse1 than
  // is currently in purse2.  Make sure we get an exception
  // doing this.
  bool exceptionThrown = false;
  try {
    Future<int> badBalance = purse1.deposit(1000, purse2);
    // TODO(sigmund): combine with previous line when transformation supports
    // normalization.
    await badBalance;
  } catch (exception) {
    if (exception.toString() == "OverdraftException") {
      print("Correctly detected overdraft.");
      exceptionThrown = true;
    } else {
      throw exception;
    }
  }
  if (!exceptionThrown) {
    // Should never arrive here because there are
    // insufficient funds to actually do the deposit
    Expect.fail("did not detect overdraft");
  }

  // Check that the balance in each purse is unchanged from
  // before we attempted to do the overdraft.
  balance = await purse1.queryBalance();
  Expect.equals(balance, balance1);

  balance = await purse2.queryBalance();
  Expect.equals(balance, balance2);

  // OK, we're all done now, close down the mint isolate
  final reply = await mint.close();
  print(reply);
  print("exit main");
}
