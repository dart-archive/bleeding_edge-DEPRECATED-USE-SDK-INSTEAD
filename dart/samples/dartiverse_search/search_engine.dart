// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of search_engine;


/**
 * A [SearchEngine] provides the ability to search for a given string.
 */
abstract class SearchEngine {
  /**
   * Get the name of the search engine.
   */
  String get name;

  /**
   * Perform a search for [input]. The returned [Stream] will complete
   * when there are no more results.
   */
  Stream<SearchResult> search(String input);
}


/**
 * A [SearchResult] entry, returned by [SearchEngine.search].
 */
class SearchResult {
  /**
   * The title of the result.
   */
  final String title;

  /**
   * The link of the result.
   */
  final String link;

  /**
   * Create a new [SearchResult] from a title and a link.
   */
  SearchResult(this.title, this.link);
}
