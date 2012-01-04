// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Copy of MintMakerRpcTest (under tests/isolate/src/) but rewritten using
// await.

#import('../../../await/samples/mintmaker/mintmaker.dart');

main() {
  print("starting test");
  MintProxy mint = new MintIsolate().open();

  PurseProxy purse1 = await mint.createPurse(100);
  Expect.equals(100, await purse1.queryBalance());
  PurseProxy purse2 = await mint.createPurse(0);
  Expect.equals(0, await purse2.queryBalance());

  Expect.equals(0 + 5, await purse2.deposit(5, purse1));
  Expect.equals(100 - 5, await purse1.queryBalance());

  int balance2 = await purse2.deposit(42, purse1);
  int balance1 = await purse1.queryBalance();
  Expect.equals(0 + 5 + 42, balance2);
  Expect.equals(100 - 5 - 42, balance1);

  // Now try to deposit more money into purse1 than
  // is currently in purse2.  Make sure we get an exception
  // doing this.
  bool exceptionThrown = false;
  try {
    await purse1.deposit(1000, purse2);
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
  Expect.equals(balance1, await purse1.queryBalance());
  Expect.equals(balance2, await purse2.queryBalance());

  // OK, we're all done now, close down the mint isolate
  print(await mint.close());
  print("exit main");
}
