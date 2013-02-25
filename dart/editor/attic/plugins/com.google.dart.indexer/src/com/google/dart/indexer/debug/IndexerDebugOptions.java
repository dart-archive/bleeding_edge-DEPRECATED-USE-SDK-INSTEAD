/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.indexer.debug;

/**
 * The interface <code>IndexerDebugOptions</code> defines constants associated with the output of
 * debugging information within the indexer.
 */
public interface IndexerDebugOptions {
  public static final String ALL_IO = "com.google.dart.indexer/debug/allIO";

  public static final String ANOMALIES = "com.google.dart.indexer/debug/anomalies";

  public static final String CATALOG_INTERNALS = "com.google.dart.indexer/debug/catalogInternals";

  public static final String DEPENDENT_UPDATES = "com.google.dart.indexer/debug/dependentUpdates";

  /**
   * The name of the option used to control whether tracing output should be produced when the index
   * is not available for some reason.
   */
  public static final String INDEX_NOT_AVAILABLE = "com.google.dart.indexer/debug/indexNotAvailable";

  /**
   * The name of the option used to control whether tracing output should be produced when indexing
   * files.
   */
  public static final String INDEXED_FILES = "com.google.dart.indexer/debug/indexedFiles";

  /**
   * The name of the option used to control whether tracing output should be produced to show how
   * long it takes to perform an index.
   */
  public static final String INDEXING_TIME = "com.google.dart.indexer/debug/indexingTime";

  public static final String INFOSTORE_CALLS = "com.google.dart.indexer/debug/infostoreCalls";

  public static final String INFOSTORE_MICROSTATS = "com.google.dart.indexer/debug/infostoreMicrostats";

  public static final String INFOSTORE_SPLITS = "com.google.dart.indexer/debug/infostoreSplits";

  public static final String LOCATION_PATH_SPLITTING = "com.google.dart.indexer/debug/locationPathSplitting";

  public static final String MISCELLANEOUS = "com.google.dart.indexer/debug/miscellaneous";

  public static final String RARE_ANOMALIES = "com.google.dart.indexer/debug/rareAnomalies";

  public static final String SESSION_LIFETIME = "com.google.dart.indexer/debug/sessionLifetime";

  public static final String STORAGE_CALLS = "com.google.dart.indexer/debug/storageCalls";

  public static final String STORE_CONTENTS_BEFORE_EACH_QUERY = "com.google.dart.indexer/debug/storeContentsBeforeEachQuery";

  public static final String TREE_CONSISTENCY = "com.google.dart.indexer/debug/treeConsistency";

  public static final String TREE_LOOKUPS = "com.google.dart.indexer/debug/treeLookups";

  public static final String TREE_MODIFICATIONS = "com.google.dart.indexer/debug/treeModifications";
}
