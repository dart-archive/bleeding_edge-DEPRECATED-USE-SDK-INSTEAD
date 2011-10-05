// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * The interface [CurrencyExchange] defines the behavior of classes that can
 * convert from one currency to another.
 */
interface CurrencyExchange {
  int convert(int amount, String sourceCurrency, String targetCurrency);
}

