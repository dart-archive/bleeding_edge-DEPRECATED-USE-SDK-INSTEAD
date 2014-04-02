// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(srdjan): fix limitations.
// - shift amount must be a Smi.
class _IntegerImplementation {
  factory _IntegerImplementation._uninstantiable() {
    throw new UnsupportedError(
        "_IntegerImplementation can only be allocated by the VM");
  }

  Type get runtimeType => int;

  num operator +(num other) {
    return other._addFromInteger(this);
  }
  num operator -(num other) {
    return other._subFromInteger(this);
  }
  num operator *(num other) {
    return other._mulFromInteger(this);
  }
  num operator ~/(num other) {
    if ((other is int) && (other == 0)) {
      throw const IntegerDivisionByZeroException();
    }
    return other._truncDivFromInteger(this);
  }
  num operator /(num other) {
    return this.toDouble() / other.toDouble();
  }
  num operator %(num other) {
    if ((other is int) && (other == 0)) {
      throw const IntegerDivisionByZeroException();
    }
    return other._moduloFromInteger(this);
  }
  int operator -() {
    return 0 - this;
  }
  int operator &(int other) {
    return other._bitAndFromInteger(this);
  }
  int operator |(int other) {
    return other._bitOrFromInteger(this);
  }
  int operator ^(int other) {
    return other._bitXorFromInteger(this);
  }
  num remainder(num other) {
    return other._remainderFromInteger(this);
  }
  int _bitAndFromInteger(int other) native "Integer_bitAndFromInteger";
  int _bitOrFromInteger(int other) native "Integer_bitOrFromInteger";
  int _bitXorFromInteger(int other) native "Integer_bitXorFromInteger";
  int _addFromInteger(int other) native "Integer_addFromInteger";
  int _subFromInteger(int other) native "Integer_subFromInteger";
  int _mulFromInteger(int other) native "Integer_mulFromInteger";
  int _truncDivFromInteger(int other) native "Integer_truncDivFromInteger";
  int _moduloFromInteger(int other) native "Integer_moduloFromInteger";
  int _remainderFromInteger(int other) {
    return other - (other ~/ this) * this;
  }
  int operator >>(int other) {
    return other._shrFromInt(this);
  }
  int operator <<(int other) {
    return other._shlFromInt(this);
  }
  bool operator <(num other) {
    return other > this;
  }
  bool operator >(num other) {
    return other._greaterThanFromInteger(this);
  }
  bool operator >=(num other) {
    return (this == other) ||  (this > other);
  }
  bool operator <=(num other) {
    return (this == other) || (this < other);
  }
  bool _greaterThanFromInteger(int other)
      native "Integer_greaterThanFromInteger";
  bool operator ==(other) {
    if (other is num) {
      return other._equalToInteger(this);
    }
    return false;
  }
  bool _equalToInteger(int other) native "Integer_equalToInteger";
  int abs() {
    return this < 0 ? -this : this;
  }
  int get sign {
    return (this > 0) ? 1 : (this < 0) ? -1 : 0;
  }
  bool get isEven => ((this & 1) == 0);
  bool get isOdd => !isEven;
  bool get isNaN => false;
  bool get isNegative => this < 0;
  bool get isInfinite => false;
  bool get isFinite => true;

  int toUnsigned(int width) {
    return this & ((1 << width) - 1);
  }

  int toSigned(int width) {
    // The value of binary number weights each bit by a power of two.  The
    // twos-complement value weights the sign bit negatively.  We compute the
    // value of the negative weighting by isolating the sign bit with the
    // correct power of two weighting and subtracting it from the value of the
    // lower bits.
    int signMask = 1 << (width - 1);
    return (this & (signMask - 1)) - (this & signMask);
  }

  int compareTo(num other) {
    final int EQUAL = 0, LESS = -1, GREATER = 1;
    if (other is double) {
      // TODO(floitsch): the following locals should be 'const'.
      int MAX_EXACT_INT_TO_DOUBLE = 9007199254740992;  // 2^53.
      int MIN_EXACT_INT_TO_DOUBLE = -MAX_EXACT_INT_TO_DOUBLE;
      double d = other;
      if (d.isInfinite) {
        return d == double.NEGATIVE_INFINITY ? GREATER : LESS;
      }
      if (d.isNaN) {
        return LESS;
      }
      if (MIN_EXACT_INT_TO_DOUBLE <= this && this <= MAX_EXACT_INT_TO_DOUBLE) {
        // Let the double implementation deal with -0.0.
        return -(d.compareTo(this.toDouble()));
      } else {
        // If abs(other) > MAX_EXACT_INT_TO_DOUBLE, then other has an integer
        // value (no bits below the decimal point).
        other = d.toInt();
      }
    }
    if (this < other) {
      return LESS;
    } else if (this > other) {
      return GREATER;
    } else {
      return EQUAL;
    }
  }

  int round() { return this; }
  int floor() { return this; }
  int ceil() { return this; }
  int truncate() { return this; }

  double roundToDouble() { return this.toDouble(); }
  double floorToDouble() { return this.toDouble(); }
  double ceilToDouble() { return this.toDouble(); }
  double truncateToDouble() { return this.toDouble(); }

  num clamp(num lowerLimit, num upperLimit) {
    if (lowerLimit is! num) throw new ArgumentError(lowerLimit);
    if (upperLimit is! num) throw new ArgumentError(upperLimit);

    // Special case for integers.
    if (lowerLimit is int && upperLimit is int) {
      if (lowerLimit > upperLimit) {
        throw new ArgumentError(lowerLimit);
      }
      if (this < lowerLimit) return lowerLimit;
      if (this > upperLimit) return upperLimit;
      return this;
    }
    // Generic case involving doubles.
    if (lowerLimit.compareTo(upperLimit) > 0) {
      throw new ArgumentError(lowerLimit);
    }
    if (lowerLimit.isNaN) return lowerLimit;
    // Note that we don't need to care for -0.0 for the lower limit.
    if (this < lowerLimit) return lowerLimit;
    if (this.compareTo(upperLimit) > 0) return upperLimit;
    return this;
  }

  int toInt() { return this; }
  double toDouble() { return new _Double.fromInteger(this); }

  String toStringAsFixed(int fractionDigits) {
    return this.toDouble().toStringAsFixed(fractionDigits);
  }
  String toStringAsExponential([int fractionDigits]) {
    return this.toDouble().toStringAsExponential(fractionDigits);
  }
  String toStringAsPrecision(int precision) {
    return this.toDouble().toStringAsPrecision(precision);
  }

  static const _digits = "0123456789abcdefghijklmnopqrstuvwxyz";

  String toRadixString(int radix) {
    if (radix is! int || radix < 2 || radix > 36) {
      throw new ArgumentError(radix);
    }
    if (radix & (radix - 1) == 0) {
      return _toPow2String(this, radix);
    }
    if (radix == 10) return this.toString();
    final bool isNegative = this < 0;
    int value = isNegative ? -this : this;
    List temp = new List();
    do {
      int digit = value % radix;
      value ~/= radix;
      temp.add(_digits.codeUnitAt(digit));
    } while (value > 0);
    if (isNegative) temp.add(0x2d);  // '-'.

    _OneByteString string = _OneByteString._allocate(temp.length);
    for (int i = 0, j = temp.length; j > 0; i++) {
      string._setAt(i, temp[--j]);
    }
    return string;
  }

  static String _toPow2String(value, radix) {
    if (value == 0) return "0";
    assert(radix & (radix - 1) == 0);
    var negative = value < 0;
    var bitsPerDigit = radix.bitLength - 1;
    var length = 0;
    if (negative) {
      value = -value;
      length = 1;
    }
    // Integer division, rounding up, to find number of _digits.
    length += (value.bitLength + bitsPerDigit - 1) ~/ bitsPerDigit;
    _OneByteString string = _OneByteString._allocate(length);
    string._setAt(0, 0x2d);  // '-'. Is overwritten if not negative.
    var mask = radix - 1;
    do {
      string._setAt(--length, _digits.codeUnitAt(value & mask));
      value >>= bitsPerDigit;
    } while (value > 0);
    return string;
  }

  _leftShiftWithMask32(count, mask)  native "Integer_leftShiftWithMask32";
}

class _Smi extends _IntegerImplementation implements int {
  factory _Smi._uninstantiable() {
    throw new UnsupportedError(
        "_Smi can only be allocated by the VM");
  }
  int get _identityHashCode {
    return this;
  }
  int operator ~() native "Smi_bitNegate";
  int get bitLength native "Smi_bitLength";

  int _shrFromInt(int other) native "Smi_shrFromInt";
  int _shlFromInt(int other) native "Smi_shlFromInt";

  /**
   * The digits of '00', '01', ... '99' as a single array.
   *
   * Get the digits of `n`, with `0 <= n < 100`, as
   * `_digitTable[n * 2]` and `_digitTable[n * 2 + 1]`.
   */
  static const _digitTable = const [
    0x30, 0x30, 0x30, 0x31, 0x30, 0x32, 0x30, 0x33,
    0x30, 0x34, 0x30, 0x35, 0x30, 0x36, 0x30, 0x37,
    0x30, 0x38, 0x30, 0x39, 0x31, 0x30, 0x31, 0x31,
    0x31, 0x32, 0x31, 0x33, 0x31, 0x34, 0x31, 0x35,
    0x31, 0x36, 0x31, 0x37, 0x31, 0x38, 0x31, 0x39,
    0x32, 0x30, 0x32, 0x31, 0x32, 0x32, 0x32, 0x33,
    0x32, 0x34, 0x32, 0x35, 0x32, 0x36, 0x32, 0x37,
    0x32, 0x38, 0x32, 0x39, 0x33, 0x30, 0x33, 0x31,
    0x33, 0x32, 0x33, 0x33, 0x33, 0x34, 0x33, 0x35,
    0x33, 0x36, 0x33, 0x37, 0x33, 0x38, 0x33, 0x39,
    0x34, 0x30, 0x34, 0x31, 0x34, 0x32, 0x34, 0x33,
    0x34, 0x34, 0x34, 0x35, 0x34, 0x36, 0x34, 0x37,
    0x34, 0x38, 0x34, 0x39, 0x35, 0x30, 0x35, 0x31,
    0x35, 0x32, 0x35, 0x33, 0x35, 0x34, 0x35, 0x35,
    0x35, 0x36, 0x35, 0x37, 0x35, 0x38, 0x35, 0x39,
    0x36, 0x30, 0x36, 0x31, 0x36, 0x32, 0x36, 0x33,
    0x36, 0x34, 0x36, 0x35, 0x36, 0x36, 0x36, 0x37,
    0x36, 0x38, 0x36, 0x39, 0x37, 0x30, 0x37, 0x31,
    0x37, 0x32, 0x37, 0x33, 0x37, 0x34, 0x37, 0x35,
    0x37, 0x36, 0x37, 0x37, 0x37, 0x38, 0x37, 0x39,
    0x38, 0x30, 0x38, 0x31, 0x38, 0x32, 0x38, 0x33,
    0x38, 0x34, 0x38, 0x35, 0x38, 0x36, 0x38, 0x37,
    0x38, 0x38, 0x38, 0x39, 0x39, 0x30, 0x39, 0x31,
    0x39, 0x32, 0x39, 0x33, 0x39, 0x34, 0x39, 0x35,
    0x39, 0x36, 0x39, 0x37, 0x39, 0x38, 0x39, 0x39
  ];

  // Powers of 10 above 1000000 are indistinguishable.
  static const int _POW_10_7  = 10000000;
  static const int _POW_10_8  = 100000000;
  static const int _POW_10_9  = 1000000000;
  static const int _POW_10_10 = 10000000000;

  // Find the number of decimal digits in a positive smi.
  static int _positiveBase10Length(var smi) {
    // A positive smi has length <= 19 if 63-bit,  <=10 if 31-bit.
    // Avoid comparing a 31-bit smi to a non-smi.
    if (smi < 1000) return 3;
    if (smi < 10000) return 4;
    if (smi < _POW_10_7) {
      if (smi < 100000) return 5;
      if (smi < 1000000) return 6;
      return 7;
    }
    if (smi < _POW_10_10) {
      if (smi < _POW_10_8) return 8;
      if (smi < _POW_10_9) return 9;
      return 10;
    }
    smi = smi ~/ _POW_10_10;
    if (smi < 10) return 11;
    if (smi < 100) return 12;
    return 10 + _positiveBase10Length(smi);
  }

  String toString() {
    if (this < 0) return _negativeToString(this);
    // Inspired by Andrei Alexandrescu: "Three Optimization Tips for C++"
    // Avoid expensive remainder operation by doing it on more than
    // one digit at a time.
    const int DIGIT_ZERO = 0x30;
    if (this < 10) {
      return _OneByteString._allocate(1).._setAt(0, DIGIT_ZERO + this);
    }
    if (this < 100) {
      int digitIndex = 2 * this;
      return _OneByteString._allocate(2)
          .._setAt(0, _digitTable[digitIndex])
          .._setAt(1, _digitTable[digitIndex + 1]);
    }
    int length = _positiveBase10Length(this);
    _OneByteString result = _OneByteString._allocate(length);
    int index = length - 1;
    var smi = this;
    do {
      // Two digits at a time.
      var twoDigits = smi.remainder(100);
      smi = smi ~/ 100;
      int digitIndex = twoDigits * 2;
      result._setAt(index, _digitTable[digitIndex + 1]);
      result._setAt(index - 1, _digitTable[digitIndex]);
      index -= 2;
    } while (smi >= 100);
    if (smi < 10) {
      // Character code for '0'.
      result._setAt(index, DIGIT_ZERO + smi);
    } else {
      // No remainder for this case.
      int digitIndex = smi * 2;
      result._setAt(index, _digitTable[digitIndex + 1]);
      result._setAt(index - 1, _digitTable[digitIndex]);
    }
    return result;
  }

  // Find the number of decimal digits in a negative smi.
  static int _negativeBase10Length(var negSmi) {
    // A negative smi has length <= 19 if 63-bit, <=10 if 31-bit.
    // Avoid comparing a 31-bit smi to a non-smi.
    if (negSmi > -1000) return 3;
    if (negSmi > -10000) return 4;
    if (negSmi > -_POW_10_7) {
      if (negSmi > -100000) return 5;
      if (negSmi > -1000000) return 6;
      return 7;
    }
    if (negSmi > -_POW_10_10) {
      if (negSmi > -_POW_10_8) return 8;
      if (negSmi > -_POW_10_9) return 9;
      return 10;
    }
    negSmi = negSmi ~/ _POW_10_10;
    if (negSmi > -10) return 11;
    if (negSmi > -100) return 12;
    return 10 + _negativeBase10Length(negSmi);
  }

  // Convert a negative smi to a string.
  // Doesn't negate the smi to avoid negating the most negative smi, which
  // would become a non-smi.
  static String _negativeToString(int negSmi) {
    // Character code for '-'
    const int MINUS_SIGN = 0x2d;
    // Character code for '0'.
    const int DIGIT_ZERO = 0x30;
    if (negSmi > -10) {
      return _OneByteString._allocate(2).._setAt(0, MINUS_SIGN)
                                        .._setAt(1, DIGIT_ZERO - negSmi);
    }
    if (negSmi > -100) {
      int digitIndex = 2 * -negSmi;
      return _OneByteString._allocate(3)
          .._setAt(0, MINUS_SIGN)
          .._setAt(1, _digitTable[digitIndex])
          .._setAt(2, _digitTable[digitIndex + 1]);
    }
    // Number of digits, not including minus.
    int digitCount = _negativeBase10Length(negSmi);
    _OneByteString result = _OneByteString._allocate(digitCount + 1);
    result._setAt(0, MINUS_SIGN);  // '-'.
    int index = digitCount;
    do {
      var twoDigits = negSmi.remainder(100);
      negSmi = negSmi ~/ 100;
      int digitIndex = -twoDigits * 2;
      result._setAt(index, _digitTable[digitIndex + 1]);
      result._setAt(index - 1, _digitTable[digitIndex]);
      index -= 2;
    } while (negSmi <= -100);
    if (negSmi > -10) {
      result._setAt(index, DIGIT_ZERO - negSmi);
    } else {
      // No remainder necessary for this case.
      int digitIndex = -negSmi * 2;
      result._setAt(index, _digitTable[digitIndex + 1]);
      result._setAt(index - 1, _digitTable[digitIndex]);
    }
    return result;
  }
}

// Represents integers that cannot be represented by Smi but fit into 64bits.
class _Mint extends _IntegerImplementation implements int {
  factory _Mint._uninstantiable() {
    throw new UnsupportedError(
        "_Mint can only be allocated by the VM");
  }
  int get _identityHashCode {
    return this;
  }
  int operator ~() native "Mint_bitNegate";
  int get bitLength native "Mint_bitLength";

  // Shift by mint exceeds range that can be handled by the VM.
  int _shrFromInt(int other) {
    if (other < 0) {
      return -1;
    } else {
      return 0;
    }
  }
  int _shlFromInt(int other) native "Mint_shlFromInt";
}

// A number that can be represented as Smi or Mint will never be represented as
// Bigint.
class _Bigint extends _IntegerImplementation implements int {
  factory _Bigint._uninstantiable() {
    throw new UnsupportedError(
        "_Bigint can only be allocated by the VM");
  }
  int get _identityHashCode {
    return this;
  }
  int operator ~() native "Bigint_bitNegate";
  int get bitLength native "Bigint_bitLength";

  // Shift by bigint exceeds range that can be handled by the VM.
  int _shrFromInt(int other) {
    if (other < 0) {
      return -1;
    } else {
      return 0;
    }
  }
  int _shlFromInt(int other) native "Bigint_shlFromInt";

  int pow(int exponent) {
    throw "Bigint.pow not implemented";
  }
}
