// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Pattern utilities for use with server.Router.
 *
 * Example:
 *
 *     var router = new Router(server);
 *     router.filter(matchesAny(new UrlPattern(r'/(\w+)'),
 *         exclude: [new UrlPattern('/login')]), authFilter);
 */
library pattern;

class _MultiPattern extends Pattern {
  final Iterable<Pattern> include;
  final Iterable<Pattern> exclude;

  _MultiPattern(Iterable<Pattern> this.include,
      {Iterable<Pattern> this.exclude});

  Iterable<Match> allMatches(String str) {
    var _allMatches = [];
    for (var pattern in include) {
      var matches = pattern.allMatches(str);
      if (_hasMatch(matches)) {
        if (exclude != null) {
          for (var excludePattern in exclude) {
            if (_hasMatch(excludePattern.allMatches(str))) {
              return [];
            }
          }
        }
        _allMatches.add(matches);
      }
    }
    return _allMatches.expand((x) => x);
  }

  Match matchAsPrefix(String string, [int start = 0]) {
    throw new UnimplementedError('matchAsPrefix is not implemented');
  }
}

/**
 * Returns a [Pattern] that matches against every pattern in [include] and
 * returns all the matches. If the input string matches against any pattern in
 * [exclude] no matches are returned.
 */
Pattern matchAny(Iterable<Pattern> include, {Iterable<Pattern> exclude}) =>
    new _MultiPattern(include, exclude: exclude);

/**
 * Returns true if [pattern] has a single match in [str] that matches the whole
 * string, not a substring.
 */
bool matchesFull(Pattern pattern, String str) {
  var iter = pattern.allMatches(str).iterator;
  if (iter.moveNext()) {
    var match = iter.current;
    return match.start == 0 && match.end == str.length && !iter.moveNext();
  }
  return false;
}

bool matchesPrefix(Pattern pattern, String str) {
  Iterable<Match> matches = pattern.allMatches(str);
  return !matches.isEmpty && matches.first.start == 0;
}

/// return the tail
Match prefixMatch(Pattern pattern, String str) {
  Iterable<Match> matches = pattern.allMatches(str);
  if (!matches.isEmpty && matches.first.start == 0) {
    return matches.first;
  }
  return null;
}

bool _hasMatch(Iterable<Match> matches) => matches.iterator.moveNext();
