// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** 
 * Polyfill script for custom elements. To use this script, your app must
 * create a CustomElementsManager with the appropriate lookup function before
 * doing any DOM queries or modifications. 
 * Currently, all custom elements must be registered with the polyfill.  To
 * register custom elements, provide the appropriate lookup function to your
 * CustomElementsManager.
 *
 * This script only works at present in dart2js, but it should work in dartium
 * soon (pending MutationObservers). The script does an XMLHTTP request, so
 * to test using locally defined custom elements you must run chrome with the
 * flag -allow-file-access-from-files.
 */

#library('webcomponents');

#import('dart:html');

#source('CustomElementsManager.dart');
#source('ListMap.dart');
#source('WebComponent.dart');

// typedefs
typedef WebComponent WebComponentFactory (ShadowRoot shadowRoot, Element elt);
typedef WebComponentFactory RegistryLookupFunction(String tagName);

// Globals
final int REQUEST_DONE = 4;
CustomElementsManager _manager;
CustomElementsManager get manager() => _manager;

void initializeComponents(RegistryLookupFunction lookup) {
  _manager = new CustomElementsManager._internal(lookup);
  manager._loadComponents();
}
