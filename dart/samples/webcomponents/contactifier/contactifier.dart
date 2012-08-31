// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A contacts app. */
#library('contactifier');

#import('dart:html');

#source('ContactsWidget.dart');

const String SITE_NAME = 'Contactifier Inc.';

void main() {
  // TODO(samhop): check for shadow dom support
  var contacts = new ContactsWidget(query('#contactsWidget'));
  queryAll('.siteName').forEach((e) {
    e.nodes.add(new Element.html('<p class="siteName">$SITE_NAME</p>'));
  });
}
