package com.google.dart.engine.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

/**
 * Helper for creating and managing single {@link AnalysisContext}.
 */
public class AnalysisContextHelper {
  public final AnalysisContext context;

  /**
   * Creates new {@link AnalysisContext} using {@link AnalysisContextFactory#contextWithCore()}.
   */
  public AnalysisContextHelper() {
    context = AnalysisContextFactory.contextWithCore();
  }

  public Source addSource(String path, String code) {
    Source source = new FileBasedSource(createFile(path));
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(source);
    context.applyChanges(changeSet);
    context.setContents(source, code);
    return source;
  }

  public CompilationUnit resolveDefiningUnit(Source source) throws Exception {
    LibraryElement libraryElement = context.computeLibraryElement(source);
    return context.resolveCompilationUnit(source, libraryElement);
  }

  public void runTasks() {
    while (context.performAnalysisTask().getChangeNotices() != null) {
    }
  }
}
