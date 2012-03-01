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

  static final _CORE_SUITE_DESCRIPTIONS = const [
      const SuiteDescription(
          'dom-attr.html',
          'DOM Attributes',
          JOHN_RESIG,
          'Setting and getting DOM node attributes',
          const ['attributes']),
      const SuiteDescription(
          'dom-modify.html',
          'DOM Modification',
          JOHN_RESIG,
          'Creating and injecting DOM nodes into a document',
          const ['modify']),
      const SuiteDescription(
          'dom-query.html',
          'DOM Query',
          JOHN_RESIG,
          'Querying DOM elements in a document',
          const ['query']),
      const SuiteDescription(
          'dom-traverse.html',
          'DOM Traversal',
          JOHN_RESIG,
          'Traversing a DOM structure',
          const ['traverse']),
  ];

  static getVariants(List<SuiteDescription> suites, variant, mapper, tags) {
    getVariant(suite) {
      final combined = new List.from(suite.tags);
      combined.addAll(tags);
      final name = null === variant ? suite.name : '${suite.name} ($variant)';
      return new SuiteDescription(
          mapper(suite.file),
          name,
          suite.origin,
          suite.description,
          combined);
    }
    return suites.map(getVariant);
  }

  // Mappings from original path to Dart-specific variants.
  static _jsPath(path) => path;
  static _domPath(path) => path.replaceFirst('.html', '-dom.html');
  static _htmlPath(path) => path.replaceFirst('.html', '-html.html');
  static _frogDomPath(path) =>
      'frog/${path.replaceFirst(".html", "-dom-js.html")}';
  static _frogHtmlPath(path) =>
      'frog/${path.replaceFirst(".html", "-html-js.html")}';

  static var _SUITE_DESCRIPTIONS;

  static List<SuiteDescription> get SUITE_DESCRIPTIONS() {
    if (null !== _SUITE_DESCRIPTIONS) {
      return _SUITE_DESCRIPTIONS;
    }
    _SUITE_DESCRIPTIONS = <SuiteDescription>[];
    add(variant, mapper, tags) {
      _SUITE_DESCRIPTIONS.addAll(
          getVariants(_CORE_SUITE_DESCRIPTIONS, variant, mapper, tags));
    }
    add('js', _jsPath, ['js']);
    add('dart:dom', _domPath, ['dart', 'dom']);
    add('dart:html', _htmlPath, ['dart', 'html']);
    add('frog dart:dom', _frogDomPath, ['frog', 'dom']);
    add('frog dart:html', _frogHtmlPath, ['frog', 'html']);
    return _SUITE_DESCRIPTIONS;
  }

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
