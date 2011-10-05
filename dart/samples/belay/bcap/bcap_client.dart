// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('bcap_client');

// Bcap library for use on the client. It implements a BcapServer that can
// invoke both client-side and http-based Bcaps. Also interoperates with the
// Belay implementation in JS so that Dart based web pages can fully exchange
// Bcaps with other Belay based apps (client and server side).

#import('../../../client/base/base.dart');
#import('../../../client/html/html.dart');
#import('../../../client/util/utilslib.dart');
#import('../../../client/json/json.dart');
#import('bcap.dart');
#import('src/impl_bcap.dart');

#source('src/BelayPort.dart');
#source('src/BelayClient.dart');
#source('src/BcapTunnel.dart');

#native('src/BelayPort.js');

class BcapClientServer extends BcapServerImpl {

  static RegExp urlRegex = null;
  
  BcapClientServer(String instanceID,
                   String snapshot = null,
                   SaveState saveState = null)
    : super(instanceID, snapshot, saveState) {
      if (urlRegex === null) {
        urlRegex = new RegExp('^https?:', '');
      }
  }

  void privateInvoke(String ser, String method, String data,
                     SuccessI ski, FailureI fki) {
    if (urlRegex.hasMatch(ser)) {
      var req = new XMLHttpRequest();
      req.open(method, ser, true);
      req.on.readyStateChange.add(void _(evt) {
        if (req.readyState == 4) {
          if (req.status == 200) {
            ski(req.responseText);
          } else {
            fki(new BcapError(req.status, req.statusText));
          }
        }
      });
      req.send(data);
      return;
    } else {
      super.privateInvoke(ser, method, data, ski, fki);
    }
  }

  Bcap wrapURL(String url, String key = null) {
    return new BcapImpl(url, this);
  }
}

