// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeTypedArrayTest');

#import('unittest_node.dart');
#import('../../../lib/node/node.dart');

void compareLists(List a, List b) {
  Expect.equals(a.length, b.length, 'length of lists');
  for (int i = 0; i < a.length; i++) {
    Expect.equals(a[i], b[i]);
  }
}

List listFromList(ctor, List a) {
  int length = a.length;
  List b = ctor(length);
  for (int i = 0; i < a.length; i++) {
    b[i] = a[i];
  }
  return b;
}

void listTest(List ctor(int length)) {
  group('List', (){
      test('length', () {
          List a = ctor(10);
          Expect.equals(10, a.length);
      });    
      test('operator[]', () {
          List src = [0,1,2,3];
          List a = listFromList(ctor, src);
          compareLists(src, a);
      });    
      test('operator[]=', () {
          List src = [0,1,2,3];
          int length = src.length;
          List a = ctor(length);
          for (int i = 0; i < length; i++) {
            a[i] = src[i];
          }
          compareLists(src, a);
      });    
      test('indexOf', () {
          List a = listFromList(ctor, [10,11,12,11]);
          Expect.equals(-1, a.indexOf(17));
          Expect.equals(1, a.indexOf(11));
          Expect.equals(3, a.indexOf(11, 2));
      });
      test('lastIndexOf', () {
          List a = listFromList(ctor, [10,11,12,11]);
          Expect.equals(-1, a.lastIndexOf(17));
          Expect.equals(3, a.lastIndexOf(11));
          Expect.equals(1, a.lastIndexOf(11, 2));
      });
      test('last', () {
          List a = listFromList(ctor, [10,11,12,13]);
          Expect.equals(13, a.last());
      });
      test('getRange', () {
          List a = listFromList(ctor, [10,11,12,13]);
          List b = a.getRange(1,2);
          a[1] = 15;
          a[2] = 17;
          compareLists(listFromList(ctor, [11,12]), b);
      });
      test('sort', () {
          List a = listFromList(ctor, [13,11,11,12]);
          List b = listFromList(ctor, [11,11,12,13]);
          a.sort((a,b) => a < b ? -1 : ((a == b) ? 0 : 1));
          compareLists(b, a);
      });
      test('forEach', () {
          List a = listFromList(ctor, [10, 11, 12, 13]);
          int expected = 10;
          int count = 0;
          a.forEach((e) {
            Expect.equals(expected, e);
            expected++;
            count++;
          });
          Expect.equals(a.length, count);
      });
      test('filter', () {
          List a = listFromList(ctor, [10, 11, 12, 13]);
          int expected = 10;
          int count = 0;
          List b = a.filter((e) {
            Expect.equals(expected, e);
            expected++;
            count++;
            return (e & 1) == 0;
          });
          Expect.equals(a.length, count);
          compareLists(listFromList(ctor, [10, 12]), b);
      });
      test('map', () {
          List a = listFromList(ctor, [10, 11, 12, 13]);
          int expected = 10;
          int count = 0;
          List b = a.map((e) {
            Expect.equals(expected, e);
            expected++;
            count++;
            return e * 2;
          });
          Expect.equals(a.length, count);
          compareLists(listFromList(ctor, [20, 22, 24, 26]), b);
      });
      test('every', () {
          List a = listFromList(ctor, [10, 11, 12, 13]);
          Expect.equals(true, a.every((e) => e >= 10));
          Expect.equals(false, a.every((e) => e < 13));
      });
      test('some', () {
          List a = listFromList(ctor, [10, 11, 12, 13]);
          Expect.equals(true, a.some((e) => e == 13));
          Expect.equals(false, a.some((e) => e == 14));
      });
      test('isEmpty', () {
          Expect.equals(true, ctor(0).isEmpty());
          Expect.equals(false, ctor(1).isEmpty());
      });
      test('iterator', () {
        List a = listFromList(ctor, [10, 11, 12, 13]);
        int expected = 10;
        int count = 0;
        Iterator i = a.iterator();
        while (i.hasNext()) {
          var e = i.next();
          Expect.equals(expected, e);
          expected++;
          count++;
        }
        Expect.equals(a.length, count);
      });
    });
} 

main() {
  test('ArrayBuffer', () {
      ArrayBuffer a = new ArrayBuffer(100);
      Expect.equals(a.byteLength, 100);
    });
    
    void basicTest(TypedArrayBufferView a, int length,
        int bytesPerElement) {
      a[0] = 12;
      Expect.equals(12, a[0]);
      Expect.equals(bytesPerElement, a.BYTES_PER_ELEMENT);
      Expect.equals(length, a.length);
      Expect.equals(0, a.byteOffset);
      Expect.equals(length * a.BYTES_PER_ELEMENT, a.byteLength);
      Expect.equals(a.byteLength, a.buffer.byteLength);

      var sub = a.subarray(75);
      Expect.equals(length - 75, sub.length, 'subarray length');
      sub = a.subarray(2, 10);
      Expect.equals(8, sub.length, 'subarray length 8');
      
      sub[0] = 13;
      a.set(sub, 1);
      Expect.equals(13, a[1], 'polymorphic set 2 args');
      
      sub[0] = 14;
      a.set(sub, 0);
      Expect.equals(14, a[0], 'polymorphic set 1 arg');
    }
    
    group('Int8Array', () {
      test('basic', () {
        basicTest(new Int8Array(100), 100, 1);
      });
      listTest((length) => new Int8Array(length));
      test('fromArray', () {
        Int8Array a = new Int8Array(100);
        a[0] = 12;
        Int8Array b = new Int8Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<int> a = [12];
        Int8Array b = new Int8Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int8Array b = new Int8Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int8Array b = new Int8Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int8Array b = new Int8Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
    
    group('Uint8Array', () {
      test('basic', () {
        basicTest(new Uint8Array(100), 100, 1);
      });
      listTest((length) => new Uint8Array(length));
      test('fromArray', () {
        Uint8Array a = new Uint8Array(100);
        a[0] = 12;
        Uint8Array b = new Uint8Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<int> a = [12];
        Uint8Array b = new Uint8Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint8Array b = new Uint8Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint8Array b = new Uint8Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint8Array b = new Uint8Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
    
    group('Int16Array', () {
      test('basic', () {
        basicTest(new Int16Array(100), 100, 2);
      });
      listTest((length) => new Int16Array(length));
      test('fromArray', () {
        Int16Array a = new Int16Array(100);
        a[0] = 12;
        Int16Array b = new Int16Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<int> a = [12];
        Int16Array b = new Int16Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int16Array b = new Int16Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int16Array b = new Int16Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int16Array b = new Int16Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
    
    group('Uint16Array', () {
      test('basic', () {
        basicTest(new Uint16Array(100), 100, 2);
      });
      listTest((length) => new Uint16Array(length));
      test('fromArray', () {
        Uint16Array a = new Uint16Array(100);
        a[0] = 12;
        Uint16Array b = new Uint16Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<int> a = [12];
        Uint16Array b = new Uint16Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint16Array b = new Uint16Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint16Array b = new Uint16Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint16Array b = new Uint16Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
    
    group('Int32Array', () {
      test('basic', () {
        basicTest(new Int32Array(100), 100, 4);
      });
      listTest((length) => new Int32Array(length));
      test('fromArray', () {
        Int32Array a = new Int32Array(100);
        a[0] = 12;
        Int32Array b = new Int32Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<int> a = [12];
        Int32Array b = new Int32Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int32Array b = new Int32Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int32Array b = new Int32Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Int32Array b = new Int32Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
    
    group('Uint32Array', () {
      test('basic', () {
        basicTest(new Uint32Array(100), 100, 4);
      });
      listTest((length) => new Uint32Array(length));
      test('fromArray', () {
        Uint32Array a = new Uint32Array(100);
        a[0] = 12;
        Uint32Array b = new Uint32Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<int> a = [12];
        Uint32Array b = new Uint32Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint32Array b = new Uint32Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint32Array b = new Uint32Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Uint32Array b = new Uint32Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
    
    group('Float32Array', () {
      test('basic', () {
        basicTest(new Float32Array(100), 100, 4);
      });
      listTest((length) => new Float32Array(length));
      test('fromArray', () {
        Float32Array a = new Float32Array(100);
        a[0] = 0.5;
        Float32Array b = new Float32Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<double> a = [0.5];
        Float32Array b = new Float32Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Float32Array b = new Float32Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Float32Array b = new Float32Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Float32Array b = new Float32Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
    
    group('Float64Array', () {
      test('basic', () {
        basicTest(new Float64Array(100), 100, 8);
      });
      listTest((length) => new Float64Array(length));
      test('fromArray', () {
        Float64Array a = new Float64Array(100);
        a[0] = 0.5;
        Float64Array b = new Float64Array.fromArray(a);
        Expect.equals(a[0], b[0]);
      });
      test('fromList', () {
        List<double> a = [0.5];
        Float64Array b = new Float64Array.fromList(a);
        Expect.equals(a[0], b[0]);
      });
       test('fromArrayBuffer 1 arg', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Float64Array b = new Float64Array.fromArrayBuffer(a);
         Expect.equals(b.buffer, a);
         Expect.equals(128, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 2 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Float64Array b = new Float64Array.fromArrayBuffer(a, 16);
         Expect.equals(a, b.buffer);
         Expect.equals(112, b.length * b.BYTES_PER_ELEMENT);
      });
       test('fromArrayBuffer 3 args', () {
         ArrayBuffer a = new ArrayBuffer(128);
         Float64Array b = new Float64Array.fromArrayBuffer(a, 16, 3);
         Expect.equals(a, b.buffer);
         Expect.equals(3, b.length);
      });
    });
  
  test('DataView', () {
    Int8Array bytes = new Int8Array.fromList([0, -1, 2, 3, 4, 5]);
    DataView d = new DataView.fromArray(bytes.buffer);
    Expect.equals(bytes.buffer, d.buffer);
    Expect.equals(0, d.byteOffset);
    Expect.equals(6, d.byteLength);
    
    Expect.equals(-1, d.getInt8(1));
    
    Expect.equals(0xff, d.getUint8(1));
    
    Expect.equals(0x0203, d.getInt16(2));
    Expect.equals(0x0203, d.getInt16(2, false));
    Expect.equals(0x0302, d.getInt16(2, true));
    
    Expect.equals(0x0203, d.getUint16(2));
    Expect.equals(0x0203, d.getUint16(2, false));
    Expect.equals(0x0302, d.getUint16(2, true));
    
    Expect.equals(0x02030405, d.getInt32(2));
    Expect.equals(0x02030405, d.getInt32(2, false));
    Expect.equals(0x05040302, d.getInt32(2, true));
    
    Expect.equals(0x02030405, d.getUint32(2));
    Expect.equals(0x02030405, d.getUint32(2, false));
    Expect.equals(0x05040302, d.getUint32(2, true));
    
    Int8Array floatBytes = new Int8Array.fromList([0x3f, 0x80, 0x00, 0x00]);
    d = new DataView.fromArray(floatBytes.buffer);
    Expect.equals(1.0, d.getFloat32(0));
    
    Int8Array doubleBytes = new Int8Array.fromList(
        [0x3f, 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
    d = new DataView.fromArray(doubleBytes.buffer);
    Expect.equals(1.0, d.getFloat64(0));

    d = new DataView.fromArray(new ArrayBuffer(8));
    d.setInt8(0, -5);
    Expect.equals(-5, d.getInt8(0));
    d.setUint8(0, -5);
    Expect.equals(0xfb, d.getUint8(0));
    d.setInt16(0, -5);
    Expect.equals(-5, d.getInt16(0));
    d.setUint16(0, -5);
    Expect.equals(0xfffb, d.getUint16(0));
    d.setInt32(0, -5);
    Expect.equals(-5, d.getInt32(0));
    d.setUint32(0, -5);
    Expect.equals(0xfffffffb, d.getUint32(0));
    d.setFloat32(0, 2.0);
    Expect.equals(2.0, d.getFloat32(0));
    d.setFloat64(0, 2.0);
    Expect.equals(2.0, d.getFloat64(0));
  });
}