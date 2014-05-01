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
package com.google.dart.engine.context;

import com.google.dart.engine.source.Source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instances of the class {@code AnalysisDelta} indicate changes to the types of analysis that
 * should be performed.
 */
public class AnalysisDelta {
  /**
   * The enumeration {@code AnalysisLevel} encodes the different levels at which a source can be
   * analyzed.
   */
  public enum AnalysisLevel {
    /**
     * Indicates a source should be fully analyzed.
     */
    ALL,

    /**
     * Indicates a source should be resolved and that errors, warnings and hints are needed.
     */
    ERRORS,

    /**
     * Indicates a source should be resolved, but that errors, warnings and hints are not needed.
     */
    RESOLVED,

    /**
     * Indicates a source is not of interest to the client.
     */
    NONE
  }

  /**
   * A mapping from source to what type of analysis should be performed on that source.
   */
  private HashMap<Source, AnalysisLevel> analysisMap = new HashMap<Source, AnalysisLevel>();

  /**
   * Initialize a newly created analysis set to be empty.
   */
  public AnalysisDelta() {
    super();
  }

  /**
   * Return a collection of the sources that have been added. This is equivalent to calling
   * {@link #getAnalysisLevels()} and collecting all sources that do not have an analysis level of
   * {@link AnalysisLevel#NONE}.
   * 
   * @return a collection of the sources
   */
  public Collection<? extends Source> getAddedSources() {
    ArrayList<Source> result = new ArrayList<Source>();
    for (Entry<Source, AnalysisLevel> entry : analysisMap.entrySet()) {
      if (entry.getValue() != AnalysisLevel.NONE) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  /**
   * Return a mapping of sources to the level of analysis that should be performed.
   * 
   * @return the analysis map
   */
  public Map<Source, AnalysisLevel> getAnalysisLevels() {
    return analysisMap;
  }

  /**
   * Record that the specified source should be analyzed at the specified level.
   * 
   * @param source the source
   * @param level the level at which the given source should be analyzed
   */
  public void setAnalysisLevel(Source source, AnalysisLevel level) {
    analysisMap.put(source, level);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean needsSeparator = appendSources(builder, false, AnalysisLevel.ALL);
    needsSeparator = appendSources(builder, needsSeparator, AnalysisLevel.RESOLVED);
    appendSources(builder, needsSeparator, AnalysisLevel.NONE);
    return builder.toString();
  }

  /**
   * Append sources with the given analysis level, prefixed with a label and possibly a separator.
   * 
   * @param builder the builder to which the sources are to be appended
   * @param needsSeparator {@code true} if a separator is needed before the label
   * @param level the analysis level of the sources to be appended
   * @return {@code true} if future lists of sources will need a separator
   */
  private boolean appendSources(StringBuilder builder, boolean needsSeparator, AnalysisLevel level) {
    boolean first = true;
    for (Entry<Source, AnalysisLevel> entry : analysisMap.entrySet()) {
      if (entry.getValue() == level) {
        if (first) {
          first = false;
          if (needsSeparator) {
            builder.append("; ");
          }
          builder.append(level);
          builder.append(" ");
        } else {
          builder.append(", ");
        }
        builder.append(entry.getKey().getFullName());
      }
    }
    return needsSeparator || !first;
  }
}
