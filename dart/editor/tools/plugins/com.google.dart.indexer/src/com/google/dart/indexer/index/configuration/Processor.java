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
package com.google.dart.indexer.index.configuration;

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.updating.FileInfoUpdater;

import org.eclipse.core.resources.IFile;

import java.util.Map;

/**
 * The interface <code>Processor</code> defines the behavior of objects that perform the processing
 * that is common to all contributors, so that operations such as parsing the file can be carried
 * out only once on each file. The processor determines a set of locations within a file, and calls
 * contributors to process each.
 * <p>
 * A processor handles a specific type of files, which is specified when the processor is
 * registered. The processor does not have to handle all files of that type. It should ignore
 * unsuitable files.
 * <p>
 * The processor is expected to delegate actual updating of the index to the contributors, however
 * it does not have to.
 */
public interface Processor {
  public long getAndResetTimeSpentParsing();

  public void initialize(ContributorWrapper[] calculators,
      Map<String, Processor> idsToUsedProcessors);

  public void processFile(IFile file, FileInfoUpdater updater) throws IndexRequestFailed;

  /**
   * Notification that the current transaction has ended. This method allows processors to cache
   * information for the duration of a single transaction in order to be more efficient.
   */
  public void transactionEnded();
}
