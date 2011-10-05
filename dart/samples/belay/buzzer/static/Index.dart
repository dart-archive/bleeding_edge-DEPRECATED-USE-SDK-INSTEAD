// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('Index');
#import('../../../../client/base/base.dart');
#import('../../../../client/html/html.dart');
#import('../../bcap/bcap.dart');
#import('../../bcap/bcap_client.dart');


class Index {
  final BelayClient belay;

  final String GENERATE_CAP_URL =
    "http://localhost:9014/belay/generateProfile";

  Index(this.belay) {
    window.document.queryOne('#create').on.click.add(
      void _(__) { generate(); }
    );
  }

  void generate() {
    InputElement titleElt = window.document.queryOne('#title');
    String title = titleElt.value;
    Bcap genCap = belay.capServer.restore(GENERATE_CAP_URL);

    genCap.post({ 'title': titleElt.value }, void _(launch) {
        belay.becomeInstance.put(launch, belay.sk, belay.fk);
    });
  }
}

void main() {
  BelayClient.main(_(belay) { new Index(belay); });
}

