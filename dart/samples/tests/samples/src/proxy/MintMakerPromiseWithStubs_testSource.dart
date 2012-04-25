// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// IsolateStubs=MintMakerPromiseWithStubsTest.dart:Mint,Purse

#library("MintMakerPromiseWithStubsTest-generatedTest");
#import("dart:isolate");
#import("../../../../proxy/promise.dart");
#import("../../../../../lib/unittest/unittest.dart");

interface Mint default MintImpl {

  Mint();

  Purse createPurse(int balance);

}

interface Purse default PurseImpl {

  Purse();

  int queryBalance();
  Purse sproutPurse();
  Promise<int> deposit(int amount, Purse$Proxy source);

}

class MintImpl implements Mint {

  MintImpl() { }

  Purse createPurse(int balance) {
    PurseImpl purse = new PurseImpl();
    purse.init(this, balance);

    return purse;
  }

}

class PurseImpl implements Purse {

  PurseImpl() { }
  // TODO(benl): implement stub constructors.
  // Note that this constructor should _not_ be in the Purse interface,
  // only this isolate is trusted to construct purses.
  //PurseImpl(this._mint, this._balance) { }
  void init(Mint mint, int balance) {
    this._mint = mint;
    this._balance = balance;
  }

  int queryBalance() {
    return _balance;
  }

  Purse sproutPurse() {
    return _mint.createPurse(0);
  }

  Promise<int> deposit(int amount, Purse$Proxy proxy) {
    if (amount < 0) throw "Ha ha";
    // Because we are in the same isolate as the other purse, we can
    // retrieve the proxy's local PurseImpl object and act on it
    // directly. Further, a forged purse will not be convertible, and
    // so an attempt to use it will fail.
    Promise<int> balance = new Promise<int>();
    proxy.addCompleteHandler((_) {
      PurseImpl source = proxy.dynamic.local;
      if (source._balance < amount) throw "Not enough dough.";
      _balance += amount;
      source._balance -= amount;
      balance.complete(_balance);
    });
    return balance;
  }

  Mint _mint;
  int _balance;

}

_completesWithValue(Promise promise, var expected) {
  promise.then(expectAsync1((value) {
    Expect.equals(expected, value);
  }));
}

main() {
  test("MintMakerPromiseWithStubsTest", () {
    Mint$Proxy mint = new Mint$ProxyImpl.createIsolate();
    Purse$Proxy purse = mint.createPurse(100);
    _completesWithValue(purse.queryBalance(), 100);

    Purse$Proxy sprouted = purse.sproutPurse();
    _completesWithValue(sprouted.queryBalance(), 0);

    // FIXME(benl): We should not have to manually order the calls
    // like this.
    Promise<int> result = sprouted.deposit(5, purse);
    Promise p1 = result;
    _completesWithValue(result, 5);
    Promise<bool> p2 = new Promise<bool>();
    Promise<bool> p3 = new Promise<bool>();
    Promise<bool> p4 = new Promise<bool>();
    Promise<bool> p5 = new Promise<bool>();
    Promise<bool> p6 = new Promise<bool>();
    result.addCompleteHandler((unused) {
      Promise<int> bal1 = sprouted.queryBalance();
      _completesWithValue(bal1, 0 + 5);
      bal1.then(expectAsync1((unused_) => p2.complete(true)));
      Promise<int> bal2 = purse.queryBalance();
      _completesWithValue(bal2, 100 - 5);
      bal2.then(expectAsync1((unused_) => p3.complete(true)));

      result = sprouted.deposit(42, purse);
      _completesWithValue(result, 5 + 42);
      result.then(expectAsync1((unused__) => p4.complete(true)));
      result.addCompleteHandler((unused_) {
        Promise<int> bal3 = sprouted.queryBalance();
        _completesWithValue(bal3, 0 + 5 + 42);
        bal3.then(expectAsync1((unused___) => p5.complete(true)));
        Promise<int> bal4 = purse.queryBalance();
        _completesWithValue(bal4, 100 - 5 - 42);
        bal4.then(expectAsync1((unused___) => p6.complete(true)));
      });
    });
    Promise<bool> done = new Promise<bool>();
    done.waitFor([p1, p2, p3, p4, p5, p6], 6);
    done.then((_) {
      print("##DONE##");
    });
  });
}
