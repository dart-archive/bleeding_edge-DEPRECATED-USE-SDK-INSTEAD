/*
 * Copyright 2013, the Dart project authors.
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
package com.google.dart.engine.internal.index.operation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.html.angular.AngularHtmlIndexContributor;
import com.google.dart.engine.source.Source;

/**
 * Instances of the {@link IndexHtmlUnitOperation} implement an operation that adds data to the
 * index based on the resolved {@link HtmlUnit}.
 * 
 * @coverage dart.engine.index
 */
public class IndexHtmlUnitOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private final IndexStore indexStore;

  /**
   * The context in which {@link HtmlUnit} was resolved.
   */
  private final AnalysisContext context;

  /**
   * The {@link HtmlUnit} being indexed.
   */
  private final HtmlUnit unit;

  /**
   * The element of the {@link HtmlUnit} being indexed.
   */
  private final HtmlElement htmlElement;

  /**
   * The source being indexed.
   */
  private final Source source;

  /**
   * Initialize a newly created operation that will index the specified {@link HtmlUnit}.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param context the context in which {@link HtmlUnit} was resolved
   * @param unit the fully resolved {@link HtmlUnit}
   */
  public IndexHtmlUnitOperation(IndexStore indexStore, AnalysisContext context, HtmlUnit unit) {
    this.indexStore = indexStore;
    this.context = context;
    this.unit = unit;
    this.htmlElement = unit.getElement();
    this.source = htmlElement.getSource();
  }

  /**
   * @return the {@link Source} to be indexed.
   */
  public Source getSource() {
    return source;
  }

  /**
   * @return the {@link HtmlUnit} to be indexed.
   */
  @VisibleForTesting
  public HtmlUnit getUnit() {
    return unit;
  }

  @Override
  public boolean isQuery() {
    return false;
  }

  @Override
  public void performOperation() {
    synchronized (indexStore) {
      try {
        boolean mayIndex = indexStore.aboutToIndexHtml(context, htmlElement);
        if (!mayIndex) {
          return;
        }
        AngularHtmlIndexContributor contributor = new AngularHtmlIndexContributor(indexStore);
        unit.accept(contributor);
        indexStore.doneIndex();
      } catch (Throwable exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not index " + unit.getElement().getLocation(),
            exception);
      }
    }
  }

  @Override
  public boolean removeWhenSourceRemoved(Source source) {
    return Objects.equal(this.source, source);
  }

  @Override
  public String toString() {
    return "IndexHtmlUnitOperation(" + source.getFullName() + ")";
  }
}
