package com.google.dart.tools.internal.corext;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.SourceRange;

public class SourceRangeFactory {

  public static SourceRange create(DartCompilationError error) {
    return new SourceRangeImpl(error.getStartPosition(), error.getLength());
  }

  public static SourceRange create(DartNode node) {
    return new SourceRangeImpl(node);
  }

}
