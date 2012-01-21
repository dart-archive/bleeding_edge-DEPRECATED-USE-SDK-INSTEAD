// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeBufferTest');

#import('unittest_node.dart');
#import('../../../lib/node/node.dart');
#import('../../../lib/node/dns.dart');

void lookupCallback(Error err, String address, int family) {
  if (err != null) {
    console.log('error:');
    console.dir(err);
  } else {
    console.log('family: $family address: $address');
  }
  callbackDone();
}

void resolveCallback(Error err, List addresses) {
  if (err != null) {
    console.log('error:');
    console.dir(err);
  } else {
    console.log('[');
    for (var address in addresses) {
      console.log(address);
    }
    console.log(']');
  }
  callbackDone();
}

void stringCallback(Error err, List<String> addresses) {
  if (err != null) {
    console.log('error:');
    console.dir(err);
  } else {
    console.log('[');
    for (var address in addresses) {
      console.log(address);
    }
    console.log(']');
  }
  callbackDone();
}

void mapStringCallback(Error err, List<Map<String,String>> addresses) {
  if (err != null) {
    console.log('error:');
    console.dir(err);
  } else {
    console.log('[');
    for (var map in addresses) {
      for (var key in map.getKeys()) {
        console.log('$key: ${map[key]}');
      }
    }
    console.log(']');
  }
  callbackDone();
}

void mapObjectCallback(Error err, List<Map<String,String>> addresses) {
  if (err != null) {
    console.log('error:');
    console.dir(err);
  } else {
    console.log('[');
    for (var map in addresses) {
      for (var key in map.getKeys()) {
        console.log('$key: ${map[key]}');
      }
    }
    console.log(']');
  }
  callbackDone();
}

void main() {
  group('Node DNS', () {
      asyncTest('lookup', 1, () =>
          dns.lookup('google.com', null, lookupCallback));
      asyncTest('resolve', 1, () =>
          dns.resolve('google.com', 'A', resolveCallback));
      asyncTest('resolve4', 1, () =>
          dns.resolve4('google.com', stringCallback));
      asyncTest('resolve6', 1, () =>
          dns.resolve6('google.com', stringCallback));
      asyncTest('resolveMx', 1, () =>
          dns.resolveMx('google.com', mapStringCallback));
      asyncTest('resolveTxt', 1, () =>
          dns.resolveTxt('google.com', stringCallback));
      asyncTest('resolveSrv', 1, () =>
          dns.resolveSrv('google.com', mapObjectCallback));
      asyncTest('reverse', 1, () =>
          dns.reverse("127.0.0.1", stringCallback));
      asyncTest('resolveNs', 1, () =>
          dns.resolveNs('google.com', stringCallback));
      asyncTest('resolveCname', 1, () =>
          dns.resolveCname('google.com', stringCallback));
      });
}