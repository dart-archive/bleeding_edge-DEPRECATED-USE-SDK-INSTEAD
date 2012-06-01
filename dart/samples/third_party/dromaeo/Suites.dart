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
    'dom': 'DOM Core Tests (dart:dom_deprecated)',
    'html': 'DOM Core Tests (dart:html)',
    'htmlidiomatic': 'DOM Core Tests (dart:html) Idiomatic',
    'js': 'DOM Core Tests (JavaScript)',
    'dart': 'DOM Core Tests (dart)',
    'frog': 'DOM Core Tests (frog)',
    'dart2js': 'DOM Core Tests (dart2js)',
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
  static _nativeMapper(lib) =>
      ((path) => path.replaceFirst('.html', '-$lib.html'));
  static _compiledMapper(lib, compiler) =>
      ((path) =>
       '$compiler/${path.replaceFirst(".html", "-dom-js.html")}');

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
    // Add original JS versions.
    add('js', _jsPath, ['js']);
    // Add native Dart versions.
    for (var version in ['html', 'htmlidiomatic']) {
      add('dart:$version', _nativeMapper(version), ['dart', version]);
    }
    // Add Dart compiled-to JS versions.
    for (var compiler in ['frog', 'dart2js']) {
      for (var version in ['dom', 'html', 'htmlidiomatic']) {
        add('$compiler:$version',
            _compiledMapper(version, compiler),
            [compiler, version]);
      }
    }
    return _SUITE_DESCRIPTIONS;
  }

  static List<SuiteDescription> getSuites(String tags) {
    // A disjunction of conjunctions (e.g.,
    // 'js&modify|dart&dom&modify').
    final taglist = tags.split('|').map((tag) => tag.split('&'));

    bool match(suite) {
      // If any conjunction matches, return true.
      for (final tagset in taglist) {
        if (tagset.every((tag) => suite.tags.indexOf(tag) >= 0)) {
          return true;
        }
      }
      return false;
    }
    final suites = SUITE_DESCRIPTIONS.filter(match);

    suites.sort((s1, s2) => s1.name.compareTo(s2.name));
    return suites;
  }

  static getCategory(String tags) {
    if (CATEGORIES.containsKey(tags)) {
      return CATEGORIES[tags];
    }
    for (final suite in _CORE_SUITE_DESCRIPTIONS) {
      if (suite.tags[0] == tags) {
        return suite.name;
      }
    }
    return null;
  }
}
