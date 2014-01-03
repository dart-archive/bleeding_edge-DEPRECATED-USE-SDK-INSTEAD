library url_matcher;

import 'package:unittest/matcher.dart';

/**
 * A reversible URL matcher interface.
 */
abstract class UrlMatcher {

  /**
   * Attempts to match a given URL. If match is successul then returns an
   * instance or [UrlMatch], otherwise returns [null].
   */
  UrlMatch match(String url);

  /**
   * Reverses (reconstructs) a URL from optionally provided parameters map
   * and a tail.
   */
  String reverse({Map parameters, String tail});

  /**
   * Returns a list of named parameters in the URL.
   */
  List<String> urlParameterNames();
}

/**
 * Object representing a successul URL match.
 */
class UrlMatch {

  /// Matched section of the URL
  final String match;

  /// Remaining unmatched suffix
  final String tail;

  ///
  final Map parameters;

  UrlMatch(this.match, this.tail, this.parameters);

  bool operator ==(o) {
    if (!(o is UrlMatch)) {
      return false;
    }
    return o.match == match && o.tail == tail &&
        equals(o.parameters, 1).matches(parameters, null);
  }

  String toString() {
    return '{$match, $tail, $parameters}';
  }
}
