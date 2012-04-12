// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeBufferTest');

#import('node_config.dart');
#import('../../../../lib/unittest/unittest.dart');
#import('../../../lib/node/node.dart');

main() {
  useNodeConfiguration();
  group('Node Buffer', () {
      test('constructor', () {
          Buffer b = new Buffer(100);
          Expect.equals(100, b.length);
        });
      test('fromList', () {
          Buffer b = new Buffer.fromList([0, 1, 2]);
          Expect.equals(3, b.length);
          Expect.equals(0, b[0]);
          Expect.equals(1, b[1]);
          Expect.equals(2, b[2]);
        });
      test('fromString', () {
          Buffer b = new Buffer.fromString('abc');
          Expect.equals(3, b.length);
          Expect.equals(97, b[0]);
          Expect.equals(98, b[1]);
          Expect.equals(99, b[2]);
        });
      test('write', () {
          Buffer b = new Buffer(4);
          b.write('hi', 0, 2);
          b.write('hoo', 2, 2);
          String s = b.toString('utf8', 0, b.length);
          Expect.equals('hiho', s);
        });
      test('charsWritten', () {
          Buffer b = new Buffer(4);
          int bytesWritten = b.write('é', 0, 4, 'utf8');
          int charsWritten = b.charsWritten;
          Expect.equals(2, bytesWritten);
          Expect.equals(1, charsWritten);
        });
      test('toString', () {
          Buffer b = new Buffer.fromString('0123');
          String s = b.toString('utf8', 1, 3);
          Expect.equals('12', s);
        });
      test('[]', () {
          Buffer b = new Buffer.fromString('012');
          Expect.equals(48, b[0]);
          Expect.equals(49, b[1]);
          Expect.equals(50, b[2]);
        });
      test('[]=', () {
          Buffer b = new Buffer(3);
          b[0] = 48;
          b[1] = 49;
          b[2] = 50;
          String s = b.toString('utf8', 0, 3);
          Expect.equals('012', s);
        });
      test('isBuffer', () {
          Buffer b = new Buffer(3);
          var o = [3];
          Expect.equals(true, Buffer.isBuffer(b));
          Expect.equals(false, Buffer.isBuffer(o));
      });
      test('byteLength', () {
          Expect.equals(1, Buffer.byteLength('e'));
          Expect.equals(2, Buffer.byteLength('é'));
      });
      test('length', () {
          Expect.equals(1, new Buffer.fromString('e').length);
          Expect.equals(2, new Buffer.fromString('é').length);
      });
      test('copy', () {
          Buffer a = new Buffer.fromString('abc');
          Buffer b = new Buffer.fromString('123');
          a.copy(b, 1, 1, 2);
          Expect.equals('abc', a.toString('utf8', 0, 3));
          Expect.equals('1b3', b.toString('utf8', 0, 3));
      });
      test('slice', () {
          Buffer a = new Buffer.fromString('abc');
          Buffer b = a.slice(1,2);
          b[0] = 48;
          Expect.equals('a0c', a.toString('utf8', 0, 3));
          Expect.equals('0', b.toString('utf8', 0, 1));
      });
      test('read-write', () {
          Buffer a = new Buffer(100);
          a.writeUInt8(0x12, 0);
          a.writeUInt16LE(0x1234, 1);
          a.writeUInt16BE(0x1234, 3);
          a.writeUInt32LE(0x12345678, 5);
          a.writeUInt32BE(0x12345678, 9);
          a.writeFloatLE(1.0, 13);
          a.writeFloatBE(1.0, 17);
          a.writeDoubleLE(1.0, 21);
          a.writeDoubleBE(1.0, 29);
        
          Expect.equals(0x12, a.readUInt8(0));
          Expect.equals(0x1234, a.readUInt16LE(1));
          Expect.equals(0x1234, a.readUInt16BE(3));
          Expect.equals(0x12345678, a.readUInt32LE(5));
          Expect.equals(0x12345678, a.readUInt32BE(9));
          Expect.equals(1.0, a.readFloatLE(13));
          Expect.equals(1.0, a.readFloatBE(17));
          Expect.equals(1.0, a.readDoubleLE(21));
          Expect.equals(1.0, a.readDoubleBE(29));

          a.writeInt8(-0x12, 0);
          a.writeInt16LE(-0x1234, 1);
          a.writeInt16BE(-0x1234, 3);
          a.writeInt32LE(-0x12345678, 5);
          a.writeInt32BE(-0x12345678, 9);

          Expect.equals(-0x12, a.readInt8(0));
          Expect.equals(-0x1234, a.readInt16LE(1));
          Expect.equals(-0x1234, a.readInt16BE(3));
          Expect.equals(-0x12345678, a.readInt32LE(5));
          Expect.equals(-0x12345678, a.readInt32BE(9));
      });
      test('fill', () {
          Buffer a = new Buffer.fromString('abc');
          a.fill(65, 1,2);
          Expect.equals('aAc', a.toString('utf8', 0, 3));
      });
      test('INSPECT_MAX_BYTES', () {
          int oldVal = Buffer.INSPECT_MAX_BYTES;
          Buffer.INSPECT_MAX_BYTES = 77;
          Expect.equals(77, Buffer.INSPECT_MAX_BYTES);
          Buffer.INSPECT_MAX_BYTES = oldVal;
      });
  });
}
