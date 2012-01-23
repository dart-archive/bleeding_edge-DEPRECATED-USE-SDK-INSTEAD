// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('cluster');
#import('node.dart');

// module cluster

/**
 * Note: cluster.fork will fail when run from frogsh. It will succeed
 * when dart code is compiled to JavaScript, and the resulting
 * JavaScript is run from node.js.
 */
 
typedef void ClusterDeathListener(ChildProcess cp);

class cluster native "require('cluster')" {
  static void fork() native;
  static bool isMaster;
  static bool isWorker;
  
  // EventEmitter
  static void removeAllListeners(String event) native;
  static void setMaxListeners(num n) native;
  _listeners(String key)
    native "return this.listeners(key);";

  // Death event
  static void emitDeath(ChildProcess cp)
    native "this.emit('death', cp);";
  static void addListenerDeath(ClusterDeathListener listener)
    native "this.addListener('death', listener);";
  static void onDeath(ClusterDeathListener listener)
    native "this.on('death', listener);";
  static void onceDeath(ClusterDeathListener listener)
    native "this.once('death', listener);";
  static void removeListenerDeath(ClusterDeathListener listener)
    native "this.removeListener('death', listener);";
  static List<ClusterDeathListener> listenersDeath()
      => _listeners('death');
}
