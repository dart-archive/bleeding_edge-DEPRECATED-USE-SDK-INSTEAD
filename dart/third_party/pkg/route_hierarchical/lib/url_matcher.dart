library url_matcher;

import 'src/utils.dart';

/**
 * A reversible URL matcher interface.
 */
abstract class UrlMatcher extends Comparable {

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

  /**
   * Return a value which is:
   * * negative if this matcher should be tested before another.
   * * zero if this matcher and another can be tested in no particular order.
   * * positive if this matcher should be tested after another.
   */
  int compareTo(UrlMatcher other) => 0;
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
        mapsShallowEqual(o.parameters, parameters);
  }

  int get hashCode => 13 + match.hashCode + tail.hashCode + parameters.hashCode;

  String toString() {
    return '{$match, $tail, $parameters}';
  }
}
