// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeZlibTest');

#import('unittest_node.dart');
#import('../../../lib/node/node.dart');
#import('../../../lib/node/zlib.dart');

void compareBuffers(Buffer a, Buffer b) {
  Expect.equals(a.length, b.length);
  for (int i = 0; i < a.length; i++) {
    Expect.equals(a[i], b[i]);
  }
}

void compareBuffersDone(Buffer a, Buffer b) {
  compareBuffers(a, b);
  callbackDone();
}

void clearBuffer(Buffer a) {
  for (int i = 0; i < a.length; i++) {
    a[i] = 0;
  }
}

Buffer createBuffer(int length) {
  Buffer b = new Buffer(length);
  clearBuffer(b);
  return b;
}

void checkTransformer(Buffer source, Buffer expected, 
    ReadWriteStream transformer) {
  transformer.addListenerData((Buffer buf2) =>
    compareBuffersDone(expected, buf2));
  transformer.endBuffer(source);
}

main() {
  asyncTest('Deflate', 1, () {
    Buffer a = createBuffer(100);
    zlib.deflateBuffer(a, (err, Buffer buf1) =>
      checkTransformer(a, buf1, zlib.createDeflate()));
  });

  asyncTest('DeflateRaw', 1, () {
    Buffer a = createBuffer(100);
    zlib.deflateRawBuffer(a, (err, Buffer buf1) =>
      checkTransformer(a, buf1, zlib.createDeflateRaw()));
  });

  asyncTest('Gzip', 1, () {
    Buffer a = createBuffer(100);
    zlib.gzipBuffer(a, (err, Buffer buf1) =>
      checkTransformer(a, buf1, zlib.createGzip()));
  });
  
  asyncTest('Inflate', 1, () {
    Buffer a = createBuffer(100);
    zlib.deflateBuffer(a, (err, Buffer buf1) =>
      zlib.inflateBuffer(buf1, (err, Buffer buf2) =>
        compareBuffersDone(a, buf2)));
  });
  
  asyncTest('InflateRaw', 1, () {
    Buffer a = createBuffer(100);
    zlib.deflateRawBuffer(a, (err, Buffer buf1) =>
      zlib.inflateRawBuffer(buf1, (err, Buffer buf2) =>
        compareBuffersDone(a, buf2)));
  });

  asyncTest('Gunzip', 1, () {
    Buffer a = createBuffer(100);
    zlib.gzipBuffer(a, (err, Buffer buf1) =>
      zlib.gunzipBuffer(buf1, (err, Buffer buf2) =>
        compareBuffersDone(a, buf2)));
  });

  asyncTest('Unzip', 2, () {
    Buffer a = createBuffer(100);
    zlib.gzipBuffer(a, (err, Buffer buf1) =>
      zlib.unzipBuffer(buf1, (err, Buffer buf2) =>
        compareBuffersDone(a, buf2)));
    zlib.deflateBuffer(a, (err, Buffer buf1) =>
      zlib.unzipBuffer(buf1, (err, Buffer buf2) =>
        compareBuffersDone(a, buf2)));
  });

  asyncTest('Inflate Stream', 1, () {
    Buffer a = createBuffer(100);
    zlib.deflateBuffer(a, (err, Buffer buf1) =>
      checkTransformer(buf1, a, zlib.createInflate()));
  });

  asyncTest('InflateRaw Stream', 1, () {
    Buffer a = createBuffer(100);
    zlib.deflateRawBuffer(a, (err, Buffer buf1) =>
      checkTransformer(buf1, a, zlib.createInflateRaw()));
  });
  
  asyncTest('Gunzip Stream', 1, () {
    Buffer a = createBuffer(100);
    zlib.gzipBuffer(a, (err, Buffer buf1) =>
      checkTransformer(buf1, a, zlib.createGunzip()));
  });
  
  asyncTest('Unzip Stream', 2, () {
    Buffer a = createBuffer(100);
    zlib.gzipBuffer(a, (err, Buffer buf1) =>
      checkTransformer(buf1, a, zlib.createUnzip()));
    zlib.deflateBuffer(a, (err, Buffer buf1) =>
      checkTransformer(buf1, a, zlib.createUnzip()));
  });
}