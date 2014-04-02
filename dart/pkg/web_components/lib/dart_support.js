// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

(function() {
  var ShadowDOMPolyfill = window.ShadowDOMPolyfill;
  if (!ShadowDOMPolyfill) return;

  if (navigator.userAgent.indexOf('(Dart)') !== -1) {
    console.error("ShadowDOMPolyfill polyfill was loaded in Dartium. This " +
        "will not work. This indicates that Dartium's Chrome version is " +
        "not compatible with this version of web_components.");
  }

  var needsConstructorFix = window.constructor === window.Window;

  // TODO(jmesserly): we need to wrap document somehow (a dart:html hook?)
  window.dartExperimentalFixupGetTag = function(originalGetTag) {
    var NodeList = ShadowDOMPolyfill.wrappers.NodeList;
    var ShadowRoot = ShadowDOMPolyfill.wrappers.ShadowRoot;
    var unwrapIfNeeded = ShadowDOMPolyfill.unwrapIfNeeded;
    function getTag(obj) {
      // TODO(jmesserly): do we still need these?
      if (obj instanceof NodeList) return 'NodeList';
      if (obj instanceof ShadowRoot) return 'ShadowRoot';
      if (MutationRecord && (obj instanceof MutationRecord))
          return 'MutationRecord';
      if (MutationObserver && (obj instanceof MutationObserver))
          return 'MutationObserver';

      // TODO(jmesserly): this prevents incorrect interaction between ShadowDOM
      // and dart:html's <template> polyfill. Essentially, ShadowDOM is
      // polyfilling native template, but our Dart polyfill fails to detect this
      // because the unwrapped node is an HTMLUnknownElement, leading it to
      // think the node has no content.
      if (obj instanceof HTMLTemplateElement) return 'HTMLTemplateElement';

      var unwrapped = unwrapIfNeeded(obj);
      if (unwrapped && (needsConstructorFix || obj !== unwrapped)) {
        // Fix up class names for Firefox, or if using the minified polyfill.
        // dart2js prefers .constructor.name, but there are all kinds of cases
        // where this will give the wrong answer.
        var ctor = obj.constructor
        if (ctor === unwrapped.constructor) {
          var name = ctor._ShadowDOMPolyfill$cacheTag_;
          if (!name) {
            name = Object.prototype.toString.call(unwrapped);
            name = name.substring(8, name.length - 1);
            ctor._ShadowDOMPolyfill$cacheTag_ = name;
          }
          return name;
        }

        obj = unwrapped;
      }
      return originalGetTag(obj);
    }

    return getTag;
  };
})();
