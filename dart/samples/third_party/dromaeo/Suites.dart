#library('Suites.dart');

class Origin {
  final String author;
  final String url;

  const Origin(this.author, this.url);
}

class SuiteDescription {
  final String file;
  final String name;
  final Origin origin;
  final String description;
  final List<String> tags;

  const SuiteDescription(this.file, this.name, this.origin,
                         this.description, this.tags);
}

class Suites {
  static final JOHN_RESIG = const Origin('John Resig', 'http://ejohn.org/');

  static final CATEGORIES = const {
    'dom': 'DOM Core Tests (dart:dom)',
    'html': 'DOM Core Tests (dart:html)',
    'js': 'DOM Core Tests (JavaScript)',
  };

  static final SUITE_DESCRIPTIONS = const [
      // Original JS.
      const SuiteDescription(
          'dom-attr.html',
          'DOM Attributes',
          JOHN_RESIG,
          'Setting and getting DOM node attributes',
          const ['js', 'attributes']),
      const SuiteDescription(
          'dom-modify.html',
          'DOM Modification',
          JOHN_RESIG,
          'Creating and injecting DOM nodes into a document',
          const ['js', 'modify']),
      const SuiteDescription(
          'dom-query.html',
          'DOM Query',
          JOHN_RESIG,
          'Querying DOM elements in a document',
          const ['js', 'query']),
      const SuiteDescription(
          'dom-traverse.html',
          'DOM Traversal',
          JOHN_RESIG,
          'Traversing a DOM structure',
          const ['js', 'traverse']),

      // dart:dom.
      const SuiteDescription(
          'dom-attr-dom.html',
          'DOM Attributes (dart:dom)',
          JOHN_RESIG,
          'Setting and getting DOM node attributes',
          const ['dart', 'dom', 'attributes']),
      const SuiteDescription(
          'dom-modify-dom.html',
          'DOM Modification (dart:dom)',
          JOHN_RESIG,
          'Creating and injecting DOM nodes into a document',
          const ['dart', 'dom', 'modify']),
      const SuiteDescription(
          'dom-query-dom.html',
          'DOM Query (dart:dom)',
          JOHN_RESIG,
          'Querying DOM elements in a document',
          const ['dart', 'dom', 'query']),
      const SuiteDescription(
          'dom-traverse-dom.html',
          'DOM Traversal (dart:dom)',
          JOHN_RESIG,
          'Traversing a DOM structure',
          const ['dart', 'dom', 'traverse']),

      // dart:html.
      const SuiteDescription(
          'dom-attr-html.html',
          'DOM Attributes (dart:html)',
          JOHN_RESIG,
          'Setting and getting DOM node attributes',
          const ['dart', 'html', 'attributes']),
      const SuiteDescription(
          'dom-modify-html.html',
          'DOM Modification (dart:html)',
          JOHN_RESIG,
          'Creating and injecting DOM nodes into a document',
          const ['dart', 'html', 'modify']),
      const SuiteDescription(
          'dom-query-html.html',
          'DOM Query (dart:html)',
          JOHN_RESIG,
          'Querying DOM elements in a document',
          const ['dart', 'html', 'query']),
      const SuiteDescription(
          'dom-traverse-html.html',
          'DOM Traversal (dart:html)',
          JOHN_RESIG,
          'Traversing a DOM structure',
          const ['dart', 'html', 'traverse']),
  ];

  static List<SuiteDescription> getSuites(List<String> tags) {
    final suites = <SuiteDescription>[];
    hasTag(tag) => tags.indexOf(tag) >= 0;

    for (final suite in SUITE_DESCRIPTIONS) {
      if (suite.tags.some(hasTag)) {
        suites.add(suite);
      }
    }

    suites.sort((s1, s2) => s1.name.compareTo(s2.name));
    return suites;
  }

  static getCategory(List<String> tags) {
    if (tags.length == 1 && CATEGORIES.containsKey(tags[0])) {
      return CATEGORIES[tags[0]];
    } else {
      return null;
    }
  }
}
