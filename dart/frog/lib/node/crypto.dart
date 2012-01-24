// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('crypto');
#import('dart:coreimpl');
#import('node.dart');
#import('nodeimpl.dart');

// module crypto

class crypto native "require('crypto')" {
  static Credentials createCredentials([Map<String,Object> details]) native;
  static Hash createHash(String algorithm) native;
  static Hmac createHmac(String algorithm, String key) native;
  static Cipher createCipher(String algorithm, String password) native;
  static Cipher createCipheriv(String algorithm, String key, String iv) native;
  static Decipher createDecipher(String algorithm, String password) native;
  static Decipher createDecipheriv(String algorithm, String key, String iv) native;
  static Signer createSign(String algorithm) native;
  static Verifier createVerify(String algorithm) native;
  static DiffieHellman createDiffieHellman(int prime_length) native;
  static DiffieHellman createDiffieHellmanFromPrime(String prime, [String encoding])
      native "return this.createDiffieHellman(prime, encoding);";
  static void pbkdf2(String password, String salt, int iterations, int keylen,
      void callback(Error err, String derivedKey)) native;
  static SlowBuffer randomBytes(size,
      [void callback(Error error, SlowBuffer sb)]) native;
}

/**
 * This is a heap-based buffer. It is an implementation detail of node.js,
 * but unfortunately it is exposed by crypto.randomBytes.
 *
 */
class SlowBuffer implements Buffer native '*SlowBuffer' {
  int write(String string, int offset, int length, [String encoding='utf8'])
    native;
  String toString(String encoding, int start, int end) native;

  // List<int> protocol
  int operator[](int index) native;
  int operator[]=(int index, int value) native;

  void add(int value) => _throwUnsupported();
  void addAll(Collection<int> collection) => _throwUnsupported();
  void addLast(int value) => _throwUnsupported();
  void clear() => _throwUnsupported();
  List<int> getRange(int start, int length) {
    FixedLists.getRangeCheck(this.length, start, length);
    Buffer b = new Buffer(length);
    this.copy(b, 0, start, start + length);
    return b;
  }
  int indexOf(int element, [int start])
      => FixedLists.indexOf(this, element, start);
  void insertRange(int start, int length, [int initialValue])
      => _throwUnsupported();
  int last()
      => FixedLists.last(this);
  int lastIndexOf(int element, [int start])
      => FixedLists.lastIndexOf(this, element, start);
  int removeLast() {_throwUnsupported(); return 0; }
  void removeRange(int start, int length) => _throwUnsupported();
  void setRange(int start, int length, List<int> from, [int startFrom])
      => _throwUnsupported();
  void sort(int compare(int a, int b))
      => DualPivotQuicksort.sort(this, compare);

  // Collection<int> members:
  void forEach(void f(int element)) => FixedLists.forEach(this, f);
  Buffer filter(bool f(int element))
    => FixedLists.filter(this, f, (length) => new Buffer(length));
  bool every(bool f(int element)) => FixedLists.every(this, f);
  bool some(bool f(int element)) => FixedLists.some(this, f);
  bool isEmpty() => FixedLists.isEmpty(this);

  // Iterable<int> members:
  Iterator<int> iterator() => new ListIterator(this);

  static bool isBuffer(obj) native;
  static int byteLength(String string, [String encoding='utf8']) native;
  final length;
  void copy(Buffer targetBuffer, int targetStart, int sourceStart, int sourceEnd
      ) native;
  Buffer slice(int start, int end) native;

  int readUInt8(int offset, [bool noAssert=false]) native;
  int readUInt16LE(int offset, [bool noAssert=false]) native;
  int readUInt16BE(int offset, [bool noAssert=false]) native;
  int readUInt32LE(int offset, [bool noAssert=false]) native;
  int readUInt32BE(int offset, [bool noAssert=false]) native;

  int readInt8(int offset, [bool noAssert=false]) native;
  int readInt16LE(int offset, [bool noAssert=false]) native;
  int readInt16BE(int offset, [bool noAssert=false]) native;
  int readInt32LE(int offset, [bool noAssert=false]) native;
  int readInt32BE(int offset, [bool noAssert=false]) native;

  double readFloatLE(int offset, [bool noAssert=false]) native;
  double readFloatBE(int offset, [bool noAssert=false]) native;
  double readDoubleLE(int offset, [bool noAssert=false]) native;
  double readDoubleBE(int offset, [bool noAssert=false]) native;

  void writeUInt8(int value, int offset, [bool noAssert=false]) native;
  void writeUInt16LE(int value, int offset, [bool noAssert=false]) native;
  void writeUInt16BE(int value, int offset, [bool noAssert=false]) native;
  void writeUInt32LE(int value, int offset, [bool noAssert=false]) native;
  void writeUInt32BE(int value, int offset, [bool noAssert=false]) native;

  void writeInt8(int value, int offset, [bool noAssert=false]) native;
  void writeInt16LE(int value, int offset, [bool noAssert=false]) native;
  void writeInt16BE(int value, int offset, [bool noAssert=false]) native;
  void writeInt32LE(int value, int offset, [bool noAssert=false]) native;
  void writeInt32BE(int value, int offset, [bool noAssert=false]) native;

  void writeFloatLE(double value, int offset, [bool noAssert=false]) native;
  void writeFloatBE(double value, int offset, [bool noAssert=false]) native;
  void writeDoubleLE(double value, int offset, [bool noAssert=false]) native;
  void writeDoubleBE(double value, int offset, [bool noAssert=false]) native;

  // end defaults to buffer.length
  void fill(int value, int offset, int end) native;
}

class Credentials native "require('crypto').Credentials" {
  // No public protocol
}

class Hash native "require('crypto').Hash" {
  void update(String data) native;
  void updateBuffer(Buffer buffer) native "this.update(buffer);";
  String digest([String encoding]) native;
}

class Hmac native "require('crypto').Hmac" {
  void update(var data) native;
  void updateBuffer(Buffer buffer) native "this.update(buffer);";
  String digest([String encoding]) native;
}

class Cipher native "require('crypto').Cipher" {
  String update(String data, [String input_encoding, String output_encoding]) native;
  String finalData([String output_encoding])
      native "return this.final(output_encoding);";
}

class Decipher native "require('crypto').Decipher" {
  String update(String data, [String input_encoding, String output_encoding]) native;
  String finalData([String output_encoding])
    native "return this.final(output_encoding);";
}

class Signer native "require('crypto').Signer" {
  void update(String data) native;
  String sign(String private_key, [String output_format]) native;
}

class Verifier native "require('crypto').Verifier" {
  void update(String data) native;
  bool verify(String object, String signature, [String signature_format]) native;
}

class DiffieHellman native "require('crypto').DiffieHellman" {
  String generateKeys([String encoding]) native;
  String computeSecret(String other_public_key,
    [String input_encoding, String output_encoding]) native;
  String getPrime([String encoding]) native;
  String getGenerator([String encoding]) native;
  String getPublicKey([String encoding]) native;
  String getPrivateKey([String encoding]) native;
  String setPublicKey(String public_key, [String encoding]) native;
  String setPrivateKey(String private_key, [String encoding]) native;
}
