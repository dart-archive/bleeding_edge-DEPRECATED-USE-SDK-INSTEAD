part of angular.directive;

/**
 * The [NgTemplateElementDirective] allows one to preload an Angular template
 * into the [TemplateCache].  It works on `<template>` and `<script>` elements
 * that have `type="text/ng-template`.  For such elements, The entire contents
 * of the elements are loaded into the [TemplateCache] under the URL specified
 * by the `id` attribute.
 *
 * Sample usage:
 *
 *     <template id="template_1.html" type="text/ng-template">
 *       TEMPLATE 1 CONTENTS
 *     </template>
 *     <script id="template_2.html" type="text/ng-template">
 *       TEMPLATE 2 CONTENTS
 *     </template>
 *
 * Refer [TemplateCache] for a **full example** as well as more information.
 */
@NgDirective(
  selector: 'template[type=text/ng-template]',
  map: const {'id': '@templateUrl'})
@NgDirective(
  selector: 'script[type=text/ng-template]',
  children: NgAnnotation.IGNORE_CHILDREN,
  map: const {'id': '@templateUrl'})
class NgTemplateDirective {
  final dom.Element element;
  final TemplateCache templateCache;

  NgTemplateDirective(this.element, this.templateCache);
  set templateUrl(url) => templateCache.put(url, new HttpResponse(200,
      element is dom.TemplateElement
          ? (element as dom.TemplateElement).content.innerHtml
          : element.innerHtml));
}
