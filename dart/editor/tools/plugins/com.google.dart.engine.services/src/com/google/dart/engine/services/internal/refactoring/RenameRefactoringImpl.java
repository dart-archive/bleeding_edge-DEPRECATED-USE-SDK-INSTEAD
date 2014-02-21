/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.internal.refactoring;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LocalElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.search.MatchKind;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeElementName;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of {@link RenameRefactoring}.
 */
public abstract class RenameRefactoringImpl extends RefactoringImpl implements RenameRefactoring {
  /**
   * @return the {@link Edit} to replace the given {@link SearchMatch} reference.
   */
  protected static Edit createReferenceEdit(SourceReference reference, String newText) {
    return new Edit(reference.range, newText);
  }

  /**
   * When one {@link Source} (one file) is used in more than one context, {@link SearchEngine} will
   * return separate {@link SearchMatch} for each context. But in rename refactoring we want to
   * update {@link Source} only once.
   */
  protected static List<SourceReference> getSourceReferences(List<SearchMatch> matches) {
    Map<SourceReference, SourceReference> uniqueReferences = Maps.newHashMap();
    for (SearchMatch match : matches) {
      Element element = match.getElement();
      MatchKind kind = match.getKind();
      Source source = element.getSource();
      SourceRange range = match.getSourceRange();
      SourceReference newReference = new SourceReference(kind, source, range);
      SourceReference oldReference = uniqueReferences.get(newReference);
      if (oldReference == null) {
        uniqueReferences.put(newReference, newReference);
        oldReference = newReference;
      }
      oldReference.elements.add(element);
    }
    return Lists.newArrayList(uniqueReferences.keySet());
  }

  /**
   * @return {@code true} if two given {@link Element}s are {@link LocalElement}s and have
   *         intersecting with visibility ranges.
   */
  protected static boolean haveIntersectingRanges(LocalElement localElement, Element element) {
    if (!(element instanceof LocalElement)) {
      return false;
    }
    LocalElement localElement2 = (LocalElement) element;
    Source localSource = localElement.getSource();
    Source localSource2 = localElement2.getSource();
    SourceRange localRange = localElement.getVisibleRange();
    SourceRange localRange2 = localElement2.getVisibleRange();
    return Objects.equal(localSource2, localSource) && localRange != null && localRange2 != null
        && localRange2.intersects(localRange);
  }

  /**
   * Check if the given {@link Element} is in the given {@link AnalysisContext}.
   */
  protected static boolean isInContext(Element element, AnalysisContext context) {
    AnalysisContext elementContext = element.getContext();
    if (elementContext != context) {
      if (context instanceof InstrumentedAnalysisContextImpl) {
        if (elementContext != ((InstrumentedAnalysisContextImpl) context).getBasis()) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if the given {@link Element} is visible in the given {@link Source}.
   */
  protected static boolean isInTheSameLibrary(Element element, AnalysisContext context,
      Source source) {
    // should be the same AnalysisContext
    if (!isInContext(element, context)) {
      return false;
    }
    // private elements are visible only in their library
    Source[] librarySourcesOfSource = context.getLibrariesContaining(source);
    Source librarySourceOfElement = element.getLibrary().getSource();
    return ArrayUtils.contains(librarySourcesOfSource, librarySourceOfElement);
  }

  /**
   * Check if the given {@link Element} is visible in the given {@link Source}.
   */
  protected static boolean isPublicOrInTheSameLibrary(Element element, AnalysisContext context,
      Source source) {
    // should be the same AnalysisContext
    if (!isInContext(element, context)) {
      return false;
    }
    // public elements are always visible
    if (element.isPublic()) {
      return true;
    }
    // private elements are visible only in their library
    return isInTheSameLibrary(element, context, source);
  }

  /**
   * @return if given unqualified {@link SearchMatch} intersects with visibility range of
   *         {@link LocalElement}.
   */
  protected static boolean isReferenceInLocalRange(LocalElement localElement, SearchMatch reference) {
    if (reference.isQualified()) {
      return false;
    }
    Source localSource = localElement.getSource();
    Source referenceSource = reference.getElement().getSource();
    SourceRange localRange = localElement.getVisibleRange();
    SourceRange referenceRange = reference.getSourceRange();
    return Objects.equal(referenceSource, localSource) && referenceRange.intersects(localRange);
  }

  private static String getDisplayName(Element element) {
    if (element instanceof ImportElement) {
      PrefixElement prefix = ((ImportElement) element).getPrefix();
      if (prefix != null) {
        return prefix.getDisplayName();
      }
    }
    return element.getDisplayName();
  }

  protected final SearchEngine searchEngine;
  protected final Element element;
  protected final AnalysisContext context;
  protected final String oldName;

  protected String newName;

  public RenameRefactoringImpl(SearchEngine searchEngine, Element element) {
    this.searchEngine = searchEngine;
    this.element = element;
    this.context = element.getContext();
    this.oldName = getDisplayName(element);
  }

  @Override
  public RefactoringStatus checkInitialConditions(ProgressMonitor pm) throws Exception {
    return new RefactoringStatus();
  }

  @Override
  public RefactoringStatus checkNewName(String newName) {
    RefactoringStatus result = new RefactoringStatus();
    if (Objects.equal(newName, getCurrentName())) {
      result.addFatalError("Choose another name.");
    }
    return result;
  }

  @Override
  public String getCurrentName() {
    return element.getDisplayName();
  }

  @Override
  public String getNewName() {
    return newName;
  }

  @Override
  public void setNewName(String newName) {
    this.newName = newName;
  }

  @Override
  public boolean shouldReportUnsafeRefactoringSource(AnalysisContext context, Source source) {
    return isPublicOrInTheSameLibrary(element, context, source);
  }

  /**
   * Adds the "Update declaration" {@link Edit} to the {@link SourceChange}.
   */
  protected final void addDeclarationEdit(SourceChange change, Element element) throws Exception {
    Edit edit = new Edit(rangeElementName(element), newName);
    addEdit(change, "Update declaration", edit);
  }

  /**
   * Adds the {@link Edit} that replaces {@link #oldName} to the {@link SourceChange}.
   */
  protected final void addEdit(SourceChange sourceChange, String description, Edit edit)
      throws Exception {
    CorrectionUtils.addEdit(context, sourceChange, description, oldName, edit);
  }

  /**
   * Adds the "Update reference" {@link Edit} to the {@link SourceChange}.
   */
  protected final void addReferenceEdit(SourceChange change, SourceReference reference)
      throws Exception {
    Edit edit = createReferenceEdit(reference, newName);
    addEdit(change, "Update reference", edit);
  }
}
