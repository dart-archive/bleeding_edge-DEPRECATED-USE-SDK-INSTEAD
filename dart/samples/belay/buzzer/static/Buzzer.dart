// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('Buzzer');
#import('../../../../client/base/base.dart');
#import('../../../../client/html/html.dart');
#import('../../bcap/bcap.dart');
#import('../../bcap/bcap_client.dart');

final String RC_BELAY_GEN = 'belay/generate';
final String RC_POST = 'urn:x-belay://resouce-class/social-feed/post';

class Buzzer {

  final BelayClient belay;
  Element topDiv;
  Bcap postCap;
  Bcap readCap;

  void onBzrClicked() {
    TextAreaElement elt = window.document.queryOne("#body");
    String body = elt.value;
    Map buzz = { "body": body };
    postCap.post(buzz);
    displayBuzz(buzz);
  }

  void displayBuzz(Map buzz) {
    Element item = makeDiv('buzzer-item');
    if (buzz['via']) {
      item.nodes.add(makeP('buzzer-via', buzz['via']));
    }
    if (buzz['nicedate']) {
      item.nodes.add(makeP('buzzer-stamp', buzz['nicedate']));
    }
    item.nodes.add(makeP('buzzer-body', buzz['body']));

    window.document.queryOne("#buzzer-items").elements.add(item);
  }

  void revive() {
    belay.capServer.setReviver(BcapHandler _(rc) {
      if (rc == RC_POST) {
        return new BcapFunctionHandler(_(data) {
          Map p = { 'body': data['body'],
                    'via': data['via'] };
          postCap.post(p, void _(__) { displayBuzz(p); });
        });
      } else {
        return null;
      }
    });
  }
  
  static Element makeDiv(String cssClass) {
    final element = document.createElement('div');
    element.classes.add(cssClass);
    return element;
  }
  
  static Element makeP(String cssClass, String content) {
    final element = document.createElement('p');
    element.classes.add(cssClass);
    element.nodes.add(new Text(content));
    return element;
  }

  Buzzer(this.belay) {
    postCap = belay.info["postCap"];
    readCap = belay.info["readCap"];

    Element titleElt = window.document.queryOne("#title");
    titleElt.text = belay.info["title"];

    topDiv = window.document.body.nodes.first;
    
    window.document.queryOne('#post').on['click'].add(
      void _(event) { onBzrClicked(); });

    Element elt = window.document.queryOne('.buzzer-post-chit');
    belay.capDraggable(
      elt,
      RC_POST,
      belay.capServer.grantFunc(_(selectedRC) {
        return belay.capServer.grantKey(selectedRC);
      }),
      belay.info['postChitURL']);

    readCap.get_(void _(posts) {
        for (var p in posts) { displayBuzz(p); };
    });

    revive();
  }
}

void main() {
  BelayClient.main((belay){ new Buzzer(belay); });
}

