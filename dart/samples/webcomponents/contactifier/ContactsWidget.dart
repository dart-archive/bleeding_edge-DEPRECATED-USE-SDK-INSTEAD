// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A contacts widget demonstrating the Shadow DOM. */
class ContactsWidget {

  // Statically populated for demo purposes.
  static const _contacts = const ['Gertrude Stein', 'Ezra Pound', 
                                   'T.S. Elliot', 'James Joyce', 
                                   'F. Scott Fitzgerald', 'Ernest Hemmingway'];
  static const _contactStyle = 
    """
    <style scoped> 
      ul {
        font-family: "Comic Sans MS", sans-serif;
        color: purple;
        list-style: none;
      }

      #userContent {
        text-align: right;
        margin: 20px;
      }
    </style>
    """;

  /**
   * User-supplied element in the external DOM that is the 
   * Shadow host for the contacts widget.
   */
  final Element _shadowHost;
  final ShadowRoot _shadowRoot;

  ContactsWidget(shadowHost) :
    _shadowHost = shadowHost,
    _shadowRoot = new ShadowRoot(shadowHost) {
    _shadowRoot.nodes.add(new Element.html(_contactStyle));
    _shadowRoot.nodes.add(contactsDOM());

    var userContent = new DivElement();
    userContent.id = 'userContent';
    userContent.nodes.add(new Element.tag('content'));
    _shadowRoot.nodes.add(userContent);
  }

  /** Returns a DOM tree fragment containing a list of contacts. */
  Element contactsDOM() {
    var ul = new UListElement();
    // TODO(samhop): set class names in contact list DOM
    _contacts.forEach((contact) {
      ul.nodes.add(new Element.html('<li>$contact</li>'));
    });
    return ul;
  }
}
