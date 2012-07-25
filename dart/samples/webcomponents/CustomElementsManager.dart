// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Loads and manages the custom elements on a page. */
class CustomElementsManager {

  /** 
   * Maps tag names to our internal dart representation of the custom element.
   */
  Map<String, _CustomDeclaration> _customDeclarations;

  // TODO(samhop): evaluate possibility of using vsm's trick of storing
  // arbitrary Dart objects directly on DOM objects rather than this map.
  /** Maps DOM elements to the user-defiend corresponding dart objects. */
  Map<Element, WebComponent> _customElements;

  RegistryLookupFunction _lookup;

  MutationObserver _insertionObserver;

  CustomElementsManager._internal(this._lookup) {
    // TODO(samhop): check for ShadowDOM support
    _customDeclarations = <String, _CustomDeclaration>{};
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

  /** Locate all external component files and load each of them. */
  void _loadComponents() {
    queryAll('link[rel=components]').forEach((link) => _load(link.href));
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
        _expandDeclarations();
      }
    });
    request.send();
  }

  /** Parse the given string of HTML to extract the custom declarations. */
  List<_CustomDeclaration>  _parse(String toParse) {
    var tmpParent = new DivElement();
    tmpParent.nodes.add(new Element.html(toParse));
    var newDeclarations = [];
    tmpParent.queryAll('element').forEach((element) {
      newDeclarations.add(new _CustomDeclaration(element));
    });
    return newDeclarations;
  }

  /** Look for all custom elements uses and expand them appropriately. */
  List _expandDeclarations() {
    var newCustomElements = [];
    _customDeclarations.getValues().forEach((declaration) {
      var query = '${declaration.extendz}[is=${declaration.name}]';
      queryAll(query).forEach((Element e) { 
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

  List refresh() {
    return _expandDeclarations();
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

    // TODO(samhop): investigate refactoring/redesigning the API so that
    // components which don't need their attributes observed don't have an
    // observer created, for perf reasons. 
    var attributeObserver = new MutationObserver((mutations, observer) {
      for (var mutation in mutations) {
        if (mutation.type == 'attributes') {
          var name = mutation.attributeName;
          newCustomElement.attributeChanged(name,
              mutation.oldValue, mutation.target.attributes[name]);
        }
      }
    });
    attributeObserver.observe(e, attributes: true, attributeOldValue: true);

    return newCustomElement;
  }
}
