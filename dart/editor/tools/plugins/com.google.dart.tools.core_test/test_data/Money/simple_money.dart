// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Instances of the class [SimpleMoney] implement a monetary amount that has a
 * single currency.
 */
class SimpleMoney implements Money {
  final int amount;

  final String currency;

  SimpleMoney(int initialAmount, String initialCurrency) :
    amount = initialAmount,
    currency = initialCurrency {
  }

  Money operator +(Money money) {
    return money.addSimpleMoney(this);
  }

  bool operator ==(Money money) {
    if (money is SimpleMoney) {
      SimpleMoney simpleMoney = money;
      return simpleMoney.getCurrency() == currency && simpleMoney.getAmount() == amount;
    }
    return false;
  }

  Money addComplexMoney(ComplexMoney money) {
    return money.addSimpleMoney(this);
  }

  Money addSimpleMoney(SimpleMoney money) {
    if (money.getCurrency() == currency) {
      return new SimpleMoney(amount + money.getAmount(), currency);
    }
    Queue<SimpleMoney> amounts = new Queue<SimpleMoney>();
    amounts.addLast(money);
    amounts.addLast(this);
    return new ComplexMoney(amounts);
  }

  int getAmount() {
    return amount;
  }

  String getCurrency() {
    return currency;
  }
}
