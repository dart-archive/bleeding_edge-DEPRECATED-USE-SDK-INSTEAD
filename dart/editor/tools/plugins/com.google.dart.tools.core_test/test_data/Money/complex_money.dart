// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
part of Money;

/**
 * Instances of the class [ComplexMoney] implement a monetary amount that is
 * composed from multiple currencies.
 */
class ComplexMoney implements Money {
  Queue<SimpleMoney> amounts;

  ComplexMoney(Queue<SimpleMoney> initialAmounts) {
    amounts = new Queue<SimpleMoney>();
    for (SimpleMoney amount in initialAmounts) {
      amounts.addLast(amount);
    }
  }

  Money operator +(Money money) {
    return money.addComplexMoney(this);
  }

  bool operator ==(Money money) {
    if (money is ComplexMoney) {
      ComplexMoney complexMoney = money;
      for (SimpleMoney amount in amounts) {
        if (!complexMoney.contains(amount)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  Money addComplexMoney(ComplexMoney money) {
    ComplexMoney result = money;
    for (SimpleMoney amount in amounts) {
      result = result + amount;
    }
    return result;
  }

  Money addSimpleMoney(SimpleMoney money) {
    Queue<SimpleMoney> newAmounts = new Queue<SimpleMoney>();
    String currency = money.getCurrency();
    bool needsAdded = true;
    for (SimpleMoney amount in amounts) {
      if (amount.getCurrency() == currency) {
        newAmounts.addLast(amount + money);
        needsAdded = false;
      } else {
        newAmounts.addLast(amount);
      }
    }
    if (needsAdded) {
      newAmounts.addLast(money);
    }
    return new ComplexMoney(newAmounts);
  }

  bool contains(SimpleMoney money) {
    for (SimpleMoney amount in amounts) {
      if (amount == money) {
        return true;
      }
    }
    return false;
  }
}
