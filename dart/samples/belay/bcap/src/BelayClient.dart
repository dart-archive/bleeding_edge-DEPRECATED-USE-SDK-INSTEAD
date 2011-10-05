// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

typedef void BelayReady(BelayClient client);

class BelayClient {

  BcapClientServer capServer;
  String instanceID;
  Map outpost;
  Bcap becomeServer;
  Bcap becomeInstance;
  Map info;
  BelayReady belayReady;
  

  Success sk;
  Failure fk;
  var preventDefault;

  BelayClient(this.belayReady) {
    sk = void _(var data) { };
    fk = void _(BcapError err) {
      window.console.log("Belay failure ${err.status}: ${err.message}");
    };
    preventDefault = bool _(Event evt) {
      evt.preventDefault();
      return false;
    };
  }

  static void main(Function callback) {
    BelayClient client = new BelayClient(callback);
    BelayPort.initialize(void _(port) { client.portReady(port); });
  }

  void portReady(BelayPort port) {
    BcapTunnel tunnel;

    BcapServerInterface portResolver(instID) => (instID == instanceID) ? capServer : null;
    BcapServerInterface capServerResolver(instID) => tunnel;
    
    void outpostHandler(String rawMsg) {
      Map blob = JSON.parse(rawMsg);
      var msgJSON = blob['value'];
      var rawInfo = msgJSON['info'];
      instanceID = msgJSON['instanceID'];

      Bcap snapCap;
      SaveState saveState = void _(__) {
        if (snapCap != null) {
          snapCap.post(capServer.snapshot());
        }
      };
      String snapshot =
        rawInfo && rawInfo.containsKey('snapshot')
        ? rawInfo['snapshot'] : null;
      
      capServer = new BcapClientServer(instanceID, snapshot, saveState);
      
      capServer.setResolver(capServerResolver);
      outpost = capServer.dataPostProcess(rawMsg);
     
      becomeInstance = outpost['becomeInstance'];
      becomeServer = outpost['becomeServer'];
      info = outpost['info'];
      snapCap = info != null && info.containsKey('snapshotCap')
        ? info['snapshotCap'] : null;
      belayReady(this);
    };
    
    tunnel = new BcapTunnel(port, portResolver, outpostHandler);

  }

  void capDraggable(Element elt, String rc, Bcap gen, var imgUrl) {
    elt.attributes['data-rc'] = rc;
    elt.classes.add('belay-cap-source');
    elt.attributes['draggable'] = 'true';

    elt.on.dragStart.add(void _(evt) {
      elt.classes.add('belay-selected');
 
      // TODO: highlight by RC
      Clipboard clip = BelayUtil.getClipboard(evt);
      var data = capServer.dataPreProcess({ 'rc': rc, 'gen': gen });
      clip.effectAllowed = 'all';

      // TODO: JavaScript version uses btoa(unescape(encodeURIComponent(data)))
      // unescape is unavailable in Dart
      // String transferData = window.btoa(HttpUtils.encodeURIComponent(data));
      String transferData = BelayUtil.belayEncode(data);

      var dragImg;
      if (imgUrl is String) { dragImg = imgUrl; }
      else if (imgUrl is Function) { dragImg = imgUrl(); }
      else { throw 'invalid imgUrl'; }

      // TODO: XSS vulnerability here
      Element img = document.createElement('img');
      img.attributes['src'] = dragImg;
      img.attributes['data'] = transferData;
      clip.setDragImage(img, 0, 0);
      clip.setData('text/html', img.outerHTML);
    });

    elt.on.dragEnd.add(void _(evt) {
      // TODO: unhighlight
      elt.classes.remove('belay-selected');
    });
  }

  void capDroppable(Element elt, String rc, var accept) {
    elt.attributes['data-rc'] = rc;
    elt.classes.add('belay-cap-target');

    elt.on['mouseenter'].add(void _(evt) {
      elt.classes.add('belay-selected');
      // TODO: highlight by RC
    });

    elt.on['mouseleave'].add(void _(evt) {
      elt.classes.remove('belay-selected');
      // TODO: unhighlight
    });
    
    elt.on.dragEnter.add(void _(evt) {
      if (elt.classes.contains('belay-possible')) {
        elt.classes.add('belay-selected');
      }
      preventDefault(evt);
    });

    elt.on.dragLeave.add(void _(evt) {
      elt.classes.remove('belay-selected');
    });

    elt.on.dragOver.add(preventDefault);

    elt.on.drop.add(void _(Event evt) {
      window.console.log('BelayClient: drop handler not yet implemented');
    });
    
  }

}
