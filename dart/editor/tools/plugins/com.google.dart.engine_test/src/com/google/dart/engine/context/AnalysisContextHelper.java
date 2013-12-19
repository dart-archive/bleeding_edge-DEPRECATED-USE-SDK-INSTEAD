package com.google.dart.engine.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

/**
 * Helper for creating and managing single {@link AnalysisContext}.
 */
public class AnalysisContextHelper {
  public final AnalysisContext context;
  private final SourceFactory sourceFactory;
  private final ContentCache cache;

  /**
   * Creates new {@link AnalysisContext} using {@link AnalysisContextFactory#contextWithCore()}.
   */
  public AnalysisContextHelper() {
    context = AnalysisContextFactory.contextWithCore();
    sourceFactory = context.getSourceFactory();
    cache = sourceFactory.getContentCache();
  }

  public Source addSource(String path, String code) {
    Source source = new FileBasedSource(cache, createFile(path));
    // add source
    {
      sourceFactory.setContents(source, "");
      ChangeSet changeSet = new ChangeSet();
      changeSet.added(source);
      context.applyChanges(changeSet);
    }
    // update source
    context.setContents(source, code);
    return source;
  }

  public CompilationUnit resolveDefiningUnit(Source source) throws Exception {
    LibraryElement libraryElement = context.computeLibraryElement(source);
    return context.resolveCompilationUnit(source, libraryElement);
  }
}
