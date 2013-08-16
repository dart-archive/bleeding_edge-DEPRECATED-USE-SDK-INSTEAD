import 'package:ghpages_generator/ghpages_generator.dart' as gh;

/**
 * Run this file to update the _gh-pages_ branch with a fresh dartdoc of
 * the library and examples compiled through dart2js.
 * Once ran you only have to push the _gh-pages_ branch to github.
 */
main() {
  new gh.Generator()
  ..setDartDoc(['lib/js.dart'], excludedLibs: ['metadata'], outDir: 'docs')
  ..setExamples(true)
  ..templateDir = 'gh-pages-template'
  ..generate();
}
