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

/** A Dart wrapper for a web component. */
abstract class WebComponent {
  /** The web component element wrapped by this class. */
  abstract Element get element();

  /** Invoked when this component gets created. */
  abstract void created();

  /** Invoked when this component gets inserted in the DOM tree. */
  abstract void inserted();

  /** Invoked when any attribute of the component is modified. */
  abstract void attributeChanged(
      String name, String oldValue, String newValue);

  /** Invoked when this component is removed from the DOM tree. */
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
    _customDeclarations = <String, _CustomDeclaration>{};
    // We use a ListMap because DOM objects aren't hashable right now.
    // TODO(samhop): DOM objects (and everything else) should be hashable
    _customElements = new ListMap<Element, WebComponent>();
    initializeInsertedRemovedCallbacks(document);
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

  /**
   * Look for all custom elements under [root] and expand them appropriately.
   * This calls the [created] event on a webcomponent, but it will not insert
   * the component in the DOM tree (and hence it won't call [inserted].
   */
  List expandDeclarations(Element root) =>
      _expandDeclarations(root, insert: false);

  /** Look for all custom elements uses and expand them appropriately. */
  List _expandDeclarations([Element root, bool insert = true]) {
    var newCustomElements = [];
    var target;
    var rootUnderTemplate = false;
    if (root == null) {
      target = document;
    } else {
      target = root;
      rootUnderTemplate = root.matchesSelector('template *');
    }
    for (var declaration in _customDeclarations.getValues()) {
      var selector = '${declaration.extendz}[is=${declaration.name}]';
      var all = target.queryAll(selector);
      // templates are innert and should not be expanded.
      var activeElements = all.filter(
          (e) => !e.matchesSelector('template *'));
      if (root != null && root.matchesSelector(selector)
         && !rootUnderTemplate) {
        activeElements.add(root);
      }
      for (var e in activeElements) {
        var component = _customElements[e];
        if (component == null) {
          component = declaration.morph(e);
          newCustomElements.add(component);
        }
        if (insert) {
          component.inserted();
        }
      }
    }
    return newCustomElements;
  }


  /** Fire the [removed] event on all web components under [root]. */
  void _removeComponents(Element root) {
    for (var decl in _customDeclarations.getValues()) {
      for (var e in root.queryAll('${decl.extendz}[is=${decl.name}]')) {
        if (_customElements[e] != null) _customElements[e].removed();
      }
      if (root.matchesSelector('${decl.extendz}[is=${decl.name}]')) {
        if (_customElements[root] != null) _customElements[root].removed();
      }
    }
  }

  /**
   * Expands the given html string into a custom element.
   * Assumes only one element use in htmlString (or only returns the
   * first one) and assumes corresponding custom element is already
   * registered.
   */
  WebComponent expandHtml(String htmlString) {
    return expandElement(new Element.html(htmlString));
  }

  /** Expand [element], assuming it is a webcomponent. */
  WebComponent expandElement(Element element) {
    var declaration = _customDeclarations[element.attributes['is']];
    if (declaration == null) throw 'No such custom element declaration';
    return declaration.morph(element);
  }

  WebComponent operator [](Element element) => _customElements[element];

  // Initializes management of inserted and removed
  // callbacks for WebComponents below root in the DOM. We need one of these
  // for every shadow subtree, since mutation observers can't see across
  // shadow boundaries.
  void initializeInsertedRemovedCallbacks(Element root) {
    _insertionObserver = new MutationObserver((mutations, observer) {
      for (var mutation in mutations) {
        // TODO(samhop): remove this test if it turns out that it always passes
        if (mutation.type == 'childList') {
          for (var node in mutation.addedNodes) {
            if (node is Element) _expandDeclarations(node);
          }
          for (var node in mutation.removedNodes) {
            if (node is Element) _removeComponents(node);
          }
        }
      }
    });
    _insertionObserver.observe(root, childList: true, subtree: true);
  }
}

bool _hasShadowRoot;

/**
 * True if the browser supports the [ShadowRoot] element and it is enabled.
 * See the [Shadow DOM spec](http://www.w3.org/TR/shadow-dom/) for more
 * information about the ShadowRoot.
 */
bool get hasShadowRoot() {
  if (_hasShadowRoot == null) {
    try {
      // TODO(jmesserly): it'd be nice if we could check this without causing
      // an exception to be thrown.
      new ShadowRoot(new DivElement());
      _hasShadowRoot = true;
    } catch (var e) {
      _hasShadowRoot = false;
      // Hide <template> elements.
      // TODO(jmesserly): This is a workaround because we don't distribute
      // children correctly. It's not actually the right fix.
      var style = new Element.html(
          @'<style type="text/css">template { display: none; }</style>');
      document.head.nodes.add(style);
    }
  }
  return _hasShadowRoot;
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
  /**
   * Modify the DOM for e, return a new Dart object corresponding to it.
   * Returns null if this custom declaration has no template element.
   */
  WebComponent morph(Element e) {
    if (template == null) {
      return null;
    }

    var shadowRoot;
    if (hasShadowRoot) {
      shadowRoot = new ShadowRoot(e);
      shadowRoot.resetStyleInheritance = false;
      if (applyAuthorStyles) {
        shadowRoot.applyAuthorStyles = true;
      }
    } else {
      // Remove the old ShadowRoot, if any
      // TODO(jmesserly): can we avoid morphing the same node twice?
      shadowRoot = e.query('.shadowroot');
      if (shadowRoot != null && shadowRoot.parent == e) shadowRoot.remove();

      // TODO(jmesserly): distribute children to insertion points.
      shadowRoot = new Element.html('<div class="shadowroot"></div>');
      e.nodes.add(shadowRoot);
    }

    template.nodes.forEach((node) => shadowRoot.nodes.add(node.clone(true)));
    var newCustomElement = manager._lookup(this.name)(shadowRoot, e);
    manager._customElements[e] = newCustomElement;
    manager._expandDeclarations(shadowRoot, insert: false);
    newCustomElement.created();
    manager._expandDeclarations(shadowRoot, insert: true);

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

    // Listen for all insertions and deletions on the DOM so that we can
    // catch custom elements being inserted and call the appropriate callbacks.
    manager.initializeInsertedRemovedCallbacks(shadowRoot);
    return newCustomElement;
  }
}
