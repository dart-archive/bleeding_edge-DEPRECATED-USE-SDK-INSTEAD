// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library analysis_server.analysis.index.index_core;

import 'dart:async';

import 'package:analyzer/src/generated/engine.dart';
import 'package:analyzer/src/generated/source.dart';

/**
 * An object that can have a [Relationship] with various [Location]s in a code
 * base.
 *
 * Clients are expected to subtype this class when implementing plugins.
 */
abstract class IndexableObject {
  // TODO(brianwilkerson) Figure out the subset of the Element API that is used
  // by the index.
}

/**
 * An object used to add relationships to the index.
 *
 * Clients are expected to subtype this class when implementing plugins.
 */
abstract class IndexContributor {
  /**
   * Contribute relationships to the given index [store] as a result of
   * analyzing the given [source] in the given [context].
   */
  void contributeTo(IndexStore store, AnalysisContext context, Source source);
}

// A sketch of what the driver routine might look like:
//
//void buildIndexForSource(AnalysisContext context, Source source) {
//  IndexStoreImpl store;
//  store.aboutToIndex(context, source);
//  try {
//    for (IndexContributor contributor in contributors) {
//      contributor.contributeTo(store, context, source);
//    }
//  } finally {
//    store.doneIndexing();
//  }
//}

/**
 * An object that stores information about the relationships between locations
 * in a code base.
 *
 * Clients are not expected to subtype this class.
 */
abstract class IndexStore {
  /**
   * Remove all of the information from the index.
   */
  void clear();

  /**
   * Return a future that completes with the locations that have the given
   * [relationship] with the given [indexable] object.
   *
   * For example, if the [indexable] object represents a function and the
   * relationship is the `is-invoked-by` relationship, then the returned
   * locations will be all of the places where the function is invoked.
   */
  Future<List<Location>> getRelationships(
      IndexableObject indexable, Relationship relationship);

  /**
   * Record that the given [indexable] object and [location] have the given
   * [relationship].
   *
   * For example, if the [relationship] is the `is-invoked-by` relationship,
   * then the [indexable] object would be the function being invoked and
   * [location] would be the point at which it is invoked. Each indexable object
   * can have the same relationship with multiple locations. In other words, if
   * the following code were executed
   *
   *     recordRelationship(indexable, isReferencedBy, location1);
   *     recordRelationship(indexable, isReferencedBy, location2);
   *
   * (where `location1 != location2`) then both relationships would be
   * maintained in the index and the result of executing
   *
   *     getRelationship(indexable, isReferencedBy);
   *
   * would be a list containing both `location1` and `location2`.
   */
  void recordRelationship(
      IndexableObject indexable, Relationship relationship, Location location);

  /**
   * Remove from the index all of the information associated with the given
   * [context].
   *
   * This method should be invoked when the [context] is disposed.
   */
  void removeContext(AnalysisContext context);

  /**
   * Remove from the index all of the information associated with indexable
   * objects or locations in the given [source]. This includes relationships
   * between an indexable object in [source] and any other locations, as well as
   * relationships between any other indexable objects and locations within
   * the [source].
   *
   * This method should be invoked when [source] is no longer part of the given
   * [context].
   */
  void removeSource(AnalysisContext context, Source source);

  /**
   * Remove from the index all of the information associated with indexable
   * objects or locations in the given sources. This includes relationships
   * between an indexable object in the given sources and any other locations,
   * as well as relationships between any other indexable objects and a location
   * within the given sources.
   *
   * This method should be invoked when the sources described by the given
   * [container] are no longer part of the given [context].
   */
  void removeSources(AnalysisContext context, SourceContainer container);
}

/**
 * Instances of the class [Location] represent a location related to an
 * indexable object.
 *
 * The location is expressed as an offset and length, but the offset is relative
 * to the source containing the indexable object rather than the start of the
 * indexable object within that source.
 *
 * Clients are not expected to subtype this class.
 */
abstract class Location {
  /**
   * An empty list of locations.
   */
  static const List<Location> EMPTY_LIST = const <Location>[];

  /**
   * Return the indexable object containing this location.
   */
  IndexableObject get indexable;

  /**
   * Return `true` if this location is a qualified reference.
   */
  bool get isQualified;

  /**
   * Return `true` if this location is a resolved reference.
   */
  bool get isResolved;

  /**
   * Return the length of this location.
   */
  int get length;

  /**
   * Return the offset of this location within the source containing the
   * indexable object.
   */
  int get offset;
}

/**
 * A relationship between an indexable object and a location. Relationships are
 * identified by a globally unique identifier.
 *
 * Clients are not expected to subtype this class.
 */
abstract class Relationship {
  /**
   * Return a relationship that has the given [identifier]. If the relationship
   * has already been created, then it will be returned, otherwise a new
   * relationship will be created
   */
  factory Relationship(String identifier) => null;
}
