// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeBufferTest');

#import('unittest_node.dart');
#import('../../../lib/node/node.dart');

void compareBuffers(Buffer a, Buffer b) {
  Expect.equals(a.length, b.length);
  for (int i = 0; i < a.length; i++) {
    Expect.equals(a[i], b[i]);
  }
}

main() {
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
          int charsWritten = Buffers.charsWritten;
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
          Expect.equals(true, Buffers.isBuffer(b));
          Expect.equals(false, Buffers.isBuffer(o));
      });
      test('byteLength', () {
          Expect.equals(1, Buffers.byteLength('e'));
          Expect.equals(2, Buffers.byteLength('é'));
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
          int oldVal = Buffers.INSPECT_MAX_BYTES;
          Buffers.INSPECT_MAX_BYTES = 77;
          Expect.equals(77, Buffers.INSPECT_MAX_BYTES);
          Buffers.INSPECT_MAX_BYTES = oldVal;
      });
      group('List<num>', () {
        test('indexOf', () {
            Buffer a = new Buffer.fromString('abcb');
            Expect.equals(-1, a.indexOf(17));
            Expect.equals(1, a.indexOf(98));
            Expect.equals(3, a.indexOf(98, 2));
        });
        test('lastIndexOf', () {
            Buffer a = new Buffer.fromString('abcb');
            Expect.equals(-1, a.lastIndexOf(17));
            Expect.equals(3, a.lastIndexOf(98));
            Expect.equals(1, a.lastIndexOf(98, 2));
        });
        test('last', () {
            Buffer a = new Buffer.fromString('abc');
            Expect.equals(99, a.last());
        });
        test('getRange', () {
            Buffer a = new Buffer.fromString('abcb');
            Buffer b = a.getRange(1,2);
            compareBuffers(b, new Buffer.fromString('bc'));
        });
        test('sort', () {
            Buffer a = new Buffer.fromString('the quick brown fox jumped over the lazy dog');
            Buffer b = new Buffer.fromString('        abcddeeeefghhijklmnoooopqrrttuuvwxyz');
            a.sort((a,b) => a < b ? -1 : ((a == b) ? 0 : 1));
            compareBuffers(b, a);
        });
        test('forEach', () {
            Buffer a = new Buffer.fromList([0, 1, 2, 3]);
            int expected = 0;
            a.forEach((e) {
              Expect.equals(expected, e);
              expected++;
            });
            Expect.equals(a.length, expected);
        });
        test('filter', () {
            Buffer a = new Buffer.fromList([0, 1, 2, 3]);
            int expected = 0;
            Buffer b = a.filter((e) {
              Expect.equals(expected, e);
              expected++;
              return (e & 1) == 0;
            });
            Expect.equals(a.length, expected);
            compareBuffers(new Buffer.fromList([0, 2]), b);
        });
        test('map', () {
            Buffer a = new Buffer.fromList([10, 11, 12, 13]);
            int expected = 10;
            int count = 0;
            Buffer b = a.map((e) {
              Expect.equals(expected, e);
              expected++;
              count++;
              return e * 2;
            });
            Expect.equals(a.length, count);
            compareBuffers(new Buffer.fromList([20, 22, 24, 26]), b);
        });
        test('every', () {
            Buffer a = new Buffer.fromList([0, 1, 2, 3]);
            Expect.equals(true, a.every((e) => e >= 0));
            Expect.equals(false, a.every((e) => e < 3));
        });
        test('some', () {
            Buffer a = new Buffer.fromList([0, 1, 2, 3]);
            Expect.equals(true, a.some((e) => e == 3));
            Expect.equals(false, a.some((e) => e == 4));
        });
        test('isEmpty', () {
            Expect.equals(true, (new Buffer(0)).isEmpty());
            Expect.equals(false, (new Buffer(1)).isEmpty());
        });
        test('iterator', () {
          Buffer a = new Buffer.fromList([0, 1, 2, 3]);
          int expected = 0;
          Iterator i = a.iterator();
          while (i.hasNext()) {
            var e = i.next();
            Expect.equals(expected, e);
            expected++;
          }
          Expect.equals(a.length, expected);
        });
    });
  });
}