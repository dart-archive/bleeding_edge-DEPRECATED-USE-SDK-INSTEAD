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

#source('list_map.dart');

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

abstract class WebComponent {
  abstract Element get element();
  abstract void created();
  abstract void inserted();
  abstract void attributeChanged(
      String name, String oldValue, String newValue);
  abstract void removed();
}

/** Loads and manages the custom elements on a page. */
class CustomElementsManager {

  /**
   * Maps tag names to our internal dart representation of the custom element.
   */
  Map<String, _CustomDeclaration> _customDeclarations;

  // TODO(samhop): evaluate possibility of using vsm's trick of storing
  // arbitrary Dart objects directly on DOM objects rather than this map.
  /** Maps DOM elements to the user-defiend corresponding dart objects. */
  ListMap<Element, WebComponent> _customElements;

  RegistryLookupFunction _lookup;

  MutationObserver _insertionObserver;

  CustomElementsManager._internal(this._lookup) {
    // TODO(samhop): check for ShadowDOM support
    _customDeclarations = <_CustomDeclaration>{};
    // We use a ListMap because DOM objects aren't hashable right now.
    // TODO(samhop): DOM objects (and everything else) should be hashable
    _customElements = new ListMap<Element, WebComponent>();

    // Listen for all insertions and deletions on the DOM so that we can
    // catch custom elements being inserted and call the appropriate callbacks.
    _insertionObserver = new MutationObserver((mutations, observer) {
      for (var mutation in mutations) {
        // TODO(samhop): remove this test if it turns out that it always passes
        if (mutation.type == 'childList') {
          for (var node in mutation.addedNodes) {
            var dartElement = _customElements[node];
            if (dartElement != null) {
              dartElement.inserted();
            }
          }
          for (var node in mutation.removedNodes) {
            var dartElement = _customElements[node];
            if (dartElement != null) {
              dartElement.removed();
            }
          }
        }
      }
    });
    _insertionObserver.observe(document, childList: true, subtree: true);
  }

  /**
   * Locate all external component files, load each of them, and expand
   * declarations.
   */
  void _loadComponents() {
    queryAll('link[rel=components]').forEach((link) => _load(link.href));
    _expandDeclarations();
  }

  /**
   * Load the document at the given url and parse it to extract
   * custom element declarations.
   */
  void _load(String url) {
    var request = new XMLHttpRequest();
    // We use a blocking request here because no Dart is allowed to run
    // until DOM content is loaded.
    // TODO(samhop): give a decent timeout message if custom elements fail to
    // load
    request.open('GET', url, async: false);
    request.on.readyStateChange.add((Event e) {
      if (request.readyState == REQUEST_DONE) {
        if (request.status >= 200 && request.status < 300
            || request.status == 304 || request.status == 0) {
          var declarations = _parse(request.response);
          declarations.forEach((declaration) {
            _customDeclarations[declaration.name] = declaration;
          });
        } else {
          window.console.error(
              'Unable to load component: Status ${request.status}'
              ' - ${request.statusText}');
        }
      }
    });
    request.send();
  }

  /** Parse the given string of HTML to extract the custom declarations. */
  List<_CustomDeclaration>  _parse(String toParse) {
    var declarations = new DocumentFragment.html(toParse);
    var newDeclarations = [];
    declarations.queryAll('element').forEach((element) {
      newDeclarations.add(new _CustomDeclaration(element));
    });
    return newDeclarations;
  }

  /** Look for all custom elements uses and expand them appropriately. */
  List _expandDeclarations([root]) {
    var newCustomElements = [];
    _customDeclarations.getValues().forEach((declaration) {
      var selector = '${declaration.extendz}[is=${declaration.name}]';
      var target = root == null ? document : root;
      target.queryAll(selector).forEach((Element e) {
        var newElement = declaration.morph(e);
        // must call the inserted callback here for elements in the initial
        // markup, since our mutation observers won't be able to see them
        // get expanded.
        // TODO(samhop): ensure that this never results in the callback
        // being called twice
        newElement.inserted();
        newCustomElements.add(newElement);
      });
    });
    return newCustomElements;
  }

  /**
   * Expands the given html string into a custom element.
   * Assumes only one element use in htmlString (or only returns the
   * first one) and assumes corresponding custom element is already
   * registered.
   */
  expandHtml(htmlString) {
    Element element = new Element.html(htmlString);
    var declaration = _customDeclarations[element.attributes['is']];
    if (declaration == null) throw 'No such custom element declaration';
    return declaration.morph(element);
  }

  WebComponent operator [](Element element) => _customElements[element];
}

class _CustomDeclaration {
  String name;
  String extendz;
  Element template;
  bool applyAuthorStyles;

  _CustomDeclaration(Element element) {
    name = element.attributes['name'];
    applyAuthorStyles = element.attributes.containsKey('apply-author-styles');
    if (name == null) {
      // TODO(samhop): friendlier errors
      window.console.error('name attribute is required');
      return;
    }
    extendz = element.attributes['extends'];
    if (extendz == null || extendz.length == 0) {
      window.console.error('extends attribute is required');
      return;
    }
    template = element.query('template');
  }

  int hashCode() {
    return name.hashCode();
  }

  operator ==(other) {
    if (other is! _CustomDeclaration) {
      return false;
    } else {
      return other.name == name &&
             other.extendz == extendz &&
             other.template == template;
    }
  }

  // TODO(samhop): better docs
  /** Modify the DOM for e, return a new Dart object corresponding to it. */
  morph(Element e) {
    if (this.template == null) {
      return;
    }

    var shadowRoot = new ShadowRoot(e);
    shadowRoot.resetStyleInheritance = false;
    if (applyAuthorStyles) {
      shadowRoot.applyAuthorStyles = true;
    }

    template.nodes.forEach((node) { shadowRoot.nodes.add(node.clone(true)); });
    var newCustomElement = manager._lookup(this.name)(shadowRoot, e);
    manager._customElements[e] = newCustomElement;
    newCustomElement.created();
    manager._expandDeclarations(shadowRoot);

    // TODO(samhop): investigate refactoring/redesigning the API so that
    // components which don't need their attributes observed don't have an
    // observer created, for perf reasons.
    var attributeObserver = new MutationObserver((mutations, observer) {
      for (var mutation in mutations) {
        if (mutation.type == 'attributes') {
          var attrName = mutation.attributeName;
          Element element = mutation.target;
          newCustomElement.attributeChanged(attrName,
              mutation.oldValue, element.attributes[attrName]);
        }
      }
    });
    attributeObserver.observe(e, attributes: true, attributeOldValue: true);

    return newCustomElement;
  }
}
