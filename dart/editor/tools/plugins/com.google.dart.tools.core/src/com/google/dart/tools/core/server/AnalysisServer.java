/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.tools.core.server;

import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.source.Source;

import java.util.Map;

/**
 * The interface {@code AnalysisServer} defines the behavior of objects that interface to an
 * analysis server.
 */
public interface AnalysisServer {
  /**
   * Add the given listener to the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be added
   */
  public void addAnalysisServerListener(AnalysisServerListener listener);

  /**
   * Inform the specified context that the changes encoded in the change set have been made. Any
   * invalidated analysis results will be flushed from the context.
   * 
   * @param contextId the identifier of the context to which the changes are to be applied
   * @param changeSet the change set to be applied
   */
  public void applyChanges(String contextId, ChangeSet changeSet);

  /**
   * Create a new context in which analysis can be performed. The context that is created will
   * persist until {@link #deleteContext(String)} is used to delete it. Clients, therefore, are
   * responsible for managing the lifetime of contexts.
   * 
   * @param name the name of the context, used for debugging purposes
   * @param sdkDirectory the path to the root directory of the Dart SDK
   * @param packageMap a table mapping package names to the path of the directory containing the
   *          package
   * @return an identifier used to identify the context that was created
   */
  public String createContext(String name, String sdkDirectory, Map<String, String> packageMap);

  /**
   * Delete the context with the given id. Future attempts to use the context id will result in an
   * error being returned.
   * 
   * @param contextId the identifier of the context to be deleted
   */
  public void deleteContext(String contextId);

  /**
   * Remove the given listener from the list of listeners that will receive notification when new
   * analysis results become available.
   * 
   * @param listener the listener to be removed
   */
  public void removeAnalysisServerListener(AnalysisServerListener listener);

  /**
   * Set the options controlling analysis within a context to the given set of options.
   * 
   * @param contextId the identifier of the context to which the options are to be applied
   * @param options the options to be applied
   */
  public void setOptions(String contextId, AnalysisOptions options);

  /**
   * Set the priority sources in the specified context to the sources in the given array.
   * 
   * @param contextId the identifier of the context to which the priority sources are to be applied
   * @param sources the options to be applied
   */
  public void setPrioritySources(String contextId, Source[] sources);

  /**
   * Cleanly shutdown the analysis server.
   */
  public void shutdown();

  /**
   * Return the version number of the analysis server.
   * 
   * @return the version number of the analysis server
   */
  public String version();
}
