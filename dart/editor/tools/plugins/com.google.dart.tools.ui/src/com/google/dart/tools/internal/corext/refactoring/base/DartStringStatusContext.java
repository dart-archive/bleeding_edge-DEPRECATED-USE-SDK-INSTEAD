package com.google.dart.tools.internal.corext.refactoring.base;

import com.google.dart.tools.core.model.SourceRange;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

/**
 * A Dart string context can be used to annotate a </code>RefactoringStatusEntry<code>
 * with detailed information about an error detected in Dart source code represented
 * by a string.
 */
public class DartStringStatusContext extends RefactoringStatusContext {

  private final String fSource;
  private final SourceRange fSourceRange;

  /**
   * @param source the source code containing the error
   * @param range a source range inside <code>source</code> or <code>null</code> if no special
   *          source range is known.
   */
  public DartStringStatusContext(String source, SourceRange range) {
    Assert.isNotNull(source);
    fSource = source;
    fSourceRange = range;
  }

  @Override
  public Object getCorrespondingElement() {
    return null;
  }

  public String getSource() {
    return fSource;
  }

  public SourceRange getSourceRange() {
    return fSourceRange;
  }
}
