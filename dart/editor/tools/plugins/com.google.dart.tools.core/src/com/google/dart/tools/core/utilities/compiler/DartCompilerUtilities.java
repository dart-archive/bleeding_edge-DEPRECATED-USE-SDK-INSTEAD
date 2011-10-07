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
package com.google.dart.tools.core.utilities.compiler;

import com.google.dart.compiler.CommandLineOptions.CompilerOptions;
import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.DartCompiler;
import com.google.dart.compiler.DartCompilerListener;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.DefaultCompilerConfiguration;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.Source;
import com.google.dart.compiler.SourceDelta;
import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.parser.DartParser;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.util.DartSourceString;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.LocalArtifactProvider;
import com.google.dart.tools.core.internal.builder.RootArtifactProvider;
import com.google.dart.tools.core.internal.cache.LRUCache;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The class <code>DartCompilerUtilities</code> defines utility methods for parsing, resolving, and
 * compiling Dart source, including compilation units, libraries, and applications.
 */
public class DartCompilerUtilities {
  /**
   * The abstract class <code>CompilerRunner</code> defines behavior common to classes used to
   * safely invoke the parser, record compilation errors, and capture any parser exception.
   */
  private static abstract class CompilerRunner extends DartCompilerListener implements
      ISafeRunnable {
    private final Collection<DartCompilationError> parseErrors;
    protected Throwable exception;

    protected CompilerRunner(Collection<DartCompilationError> parseErrors) {
      this.parseErrors = parseErrors;
    }

    @Override
    public void compilationError(DartCompilationError event) {
      if (parseErrors != null) {
        parseErrors.add(event);
      }
    }

    @Override
    public void compilationWarning(DartCompilationError event) {
      if (parseErrors != null) {
        parseErrors.add(event);
      }
    }

    @Override
    public void handleException(Throwable exception) {
      this.exception = exception;

      // Log exceptions, but don't flood the log
      if (parserExceptionCount < 5) {
        DartCore.logError("Exception on [" + getThreadName() + "] when parsing:\n"
            + getTargetName(), exception);
      }
      parserExceptionCount++;
    }

    @Override
    public void typeError(DartCompilationError event) {
      if (parseErrors != null) {
        parseErrors.add(event);
      }
    }

    protected abstract String getTargetName();
  }

  private static class DartURIStringSource implements DartSource {
    private LibrarySource library;
    private URI uri;
    private String relPath;
    private String source;
    private long lastModified = System.currentTimeMillis();

    private DartURIStringSource(LibrarySource library, URI uri, String relPath, String source) {
      this.library = library;
      this.uri = uri;
      this.relPath = relPath;
      this.source = source;
    }

    @Override
    public boolean exists() {
      return true;
    }

    @Override
    public long getLastModified() {
      return lastModified;
    }

    @Override
    public LibrarySource getLibrary() {
      return library;
    }

    @Override
    public String getName() {
      return uri.toString();
    }

    @Override
    public String getRelativePath() {
      return relPath;
    }

    @Override
    public Reader getSourceReader() throws IOException {
      return new StringReader(source);
    }

    @Override
    public URI getUri() {
      return uri;
    }
  }

  /**
   * Internal class for safely calling the parser, recording compilation errors, and capturing any
   * parser exception.
   * <p>
   * TODO Unify with ResolverRunnable - compilerConfig could be shared
   */
  private static final class DeltaAnalysisRunnable extends CompilerRunner {
    private int completionLocation;
    private DartNode completionNode;
    private LibraryWithSuppliedSources librarySource;
    private URI unitUri;
    private DartUnit parsedUnit;
    private DartSourceString source;
    private DartNode analyzedNode;

    DeltaAnalysisRunnable(LibrarySource librarySource, URI unitUri,
        Map<URI, String> suppliedSources, DartUnit suppliedUnit, DartNode completionNode,
        int completionLocation, Collection<DartCompilationError> parseErrors) {
      super(parseErrors);
      librarySource.getClass(); // quick null check
      unitUri.getClass(); // quick null check
      suppliedSources.getClass(); // quick null check
      suppliedUnit.getClass(); // quick null check
      completionNode.getClass(); // quick null check
      parseErrors.getClass(); // quick null check
      this.librarySource = new LibraryWithSuppliedSources(librarySource, suppliedSources);
      this.unitUri = unitUri;
      this.parsedUnit = suppliedUnit;
      this.completionNode = completionNode;
      this.completionLocation = completionLocation;
      String sourceString = suppliedSources.get(unitUri);
      this.source = new DartSourceString(unitUri.getPath(), sourceString);
    }

    @Override
    public void run() throws Exception {
      final SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
      LibraryElement enclosingLibrary = cachedLibraries.get(librarySource.wrappedSource).getElement();

      // Try to find the core library in the enclosing set of libraries, otherwise the typeAnalyzer
      // will be void of core types.
      LibraryUnit coreUnit = DartCompiler.getCoreLib(enclosingLibrary.getLibraryUnit());
      if (coreUnit == null) {
        throw new RuntimeException("Unable to locate core library");
      }
      LibraryElement coreLibrary = coreUnit.getElement();

      SourceDelta delta = new SourceDelta() {

        @Override
        public DartSource getSourceAfter() {
          return source;
        }

        @Override
        public Source getSourceBefore() {
          return null;
        }

        @Override
        public DartUnit getUnitAfter() {
          return parsedUnit;
        }
      };
      final CompilerConfiguration config = new DefaultCompilerConfiguration(new CompilerOptions(),
          libraryManager) {

        @Override
        public boolean allowNoSuchType() {
          return true;
        }

        @Override
        public boolean checkOnly() {
          return true;
        }

        @Override
        public boolean incremental() {
          return true;
        }

        @Override
        public boolean resolveDespiteParseErrors() {
          return true;
        }

        @Override
        public boolean typeErrorsAreFatal() {
          return false;
        }

        @Override
        public boolean warningsAreFatal() {
          return false;
        }
      };
      analyzedNode = DartCompiler.analyzeDelta(delta, enclosingLibrary, coreLibrary,
          completionNode, completionLocation, 0, config, this);
    }

    @Override
    protected String getTargetName() {
      String targetName = null;
      targetName = unitUri.toString();
      if (targetName == null) {
        targetName = librarySource.getName();
      }
      return targetName;
    }

  }

  private static class LibraryWithSuppliedSources implements LibrarySource {
    private LibrarySource wrappedSource;

    private SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();

    private Map<URI, String> suppliedSources;

    private LibraryWithSuppliedSources(LibrarySource wrappedSource, Map<URI, String> suppliedSources) {
      this.wrappedSource = wrappedSource;
      this.suppliedSources = suppliedSources;
    }

    @Override
    public boolean exists() {
      return wrappedSource.exists();
    }

    @Override
    public LibrarySource getImportFor(String relPath) throws IOException {
      return wrappedSource.getImportFor(relPath);
    }

    @Override
    public long getLastModified() {
      return wrappedSource.getLastModified();
    }

    @Override
    public String getName() {
      return wrappedSource.getName();
    }

    @Override
    public DartSource getSourceFor(String relPath) {
      URI uri = wrappedSource.getUri().resolve(relPath);
      if (uri != null) {
        for (URI key : suppliedSources.keySet()) {
          if (equalUris(libraryManager, key, uri)) {
            String source = suppliedSources.get(key);
            return new DartURIStringSource(this, uri, relPath, source);
          }
        }
      }
      return wrappedSource.getSourceFor(relPath);
    }

    @Override
    public Reader getSourceReader() throws IOException {
      return wrappedSource.getSourceReader();
    }

    @Override
    public URI getUri() {
      return wrappedSource.getUri();
    }
  }

  /**
   * Internal class for safely calling the parser, recording compilation errors, and capturing any
   * parser exception.
   */
  private static final class ParserRunnable extends CompilerRunner {
    private final DartSource sourceRef;
    private final String source;
    private DartUnit result;

    public ParserRunnable(DartSource sourceRef, String source,
        Collection<DartCompilationError> parseErrors) {
      super(parseErrors);
      this.sourceRef = sourceRef;
      this.source = source;
    }

    @Override
    public void run() throws Exception {
      result = new DartParser(sourceRef, source, this).parseUnit(sourceRef);
    }

    @Override
    protected String getTargetName() {
      return source;
    }
  }

  /**
   * Internal class for safely calling the parser, recording compilation errors, and capturing any
   * parser exception.
   */
  private static final class ResolverRunnable extends CompilerRunner {
    private LibrarySource librarySource;
    private URI unitUri;
    private boolean forceFullAST;
    private LibraryUnit libraryResult;
    private DartUnit unitResult;
    private Map<URI, DartUnit> parsedUnits;

    ResolverRunnable(LibrarySource librarySource, Map<URI, DartUnit> suppliedUnits,
        boolean forceFullAST, Collection<DartCompilationError> parseErrors) {
      this(librarySource, null, null, suppliedUnits, forceFullAST, parseErrors);
    }

    ResolverRunnable(LibrarySource librarySource, URI unitUri, Map<URI, String> suppliedSources,
        boolean forceFullAST, Collection<DartCompilationError> parseErrors) {
      this(librarySource, unitUri, suppliedSources, null, forceFullAST, parseErrors);
    }

    private ResolverRunnable(LibrarySource librarySource, URI unitUri,
        Map<URI, String> suppliedSources, Map<URI, DartUnit> suppliedUnits, boolean forceFullAST,
        Collection<DartCompilationError> parseErrors) {
      super(parseErrors);
      if (suppliedSources != null) {
        this.librarySource = new LibraryWithSuppliedSources(librarySource, suppliedSources);
      } else {
        this.librarySource = librarySource;
      }
      this.unitUri = unitUri;
      this.parsedUnits = suppliedUnits;
      this.forceFullAST = forceFullAST;
    }

    @Override
    public void run() throws Exception {
      final SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
      final CompilerConfiguration config = new DefaultCompilerConfiguration(new CompilerOptions(),
          libraryManager) {

        @Override
        public boolean checkOnly() {
          return true;
        }

        @Override
        public boolean incremental() {
          return true;
        }

        @Override
        public boolean resolveDespiteParseErrors() {
          return true;
        }
      };
      DartArtifactProvider provider = new LocalArtifactProvider(RootArtifactProvider.getInstance()) {
        @Override
        protected boolean isOutOfDateInParent(Source source, Source base, String extension) {
          if (forceFullAST || equalUris(libraryManager, unitUri, source.getUri())) {
            return true;
          }
          if (parsedUnits != null && parsedUnits.containsKey(source.getUri())) {
            return true;
          }
          return super.isOutOfDateInParent(source, base, extension);
        }
      };
      libraryResult = DartCompiler.analyzeLibrary(librarySource, parsedUnits, config, provider,
          this);

      if (libraryResult != null && unitUri != null) {
        for (DartUnit unit : libraryResult.getUnits()) {
          DartSource source = unit.getSource();
          if (source != null) {
            if (equalUris(libraryManager, unitUri, source.getUri())) {
              unitResult = unit;
              return;
            }
          }
        }
      }
    }

    @Override
    protected String getTargetName() {
      String targetName = null;
      if (unitUri != null) {
        targetName = unitUri.toString();
      }
      if (targetName == null) {
        targetName = librarySource.getName();
      }
      return targetName;
    }

  }

  public static int parserExceptionCount = 0;

  private static LRUCache<LibrarySource, LibraryUnit> cachedLibraries = new LRUCache<LibrarySource, LibraryUnit>(
      10);

  public static DartNode analyzeDelta(LibrarySource library, String sourceString,
      DartUnit suppliedUnit, DartNode completionNode, int completionLocation,
      final Collection<DartCompilationError> parseErrors) throws DartModelException {
    if (cachedLibraries.get(library) == null) {
      Collection<DartUnit> parsedUnits = new ArrayList<DartUnit>();
      parsedUnits.add(suppliedUnit);
      LibraryUnit resolvedLib = resolveLibrary(library, parsedUnits, parseErrors);
      synchronized (cachedLibraries) {
        LibraryUnit newLib = cachedLibraries.get(library);
        if (newLib == null) {
          cachedLibraries.put(library, resolvedLib);
        }
      }
      return completionNode;
    }
    DartSource src = suppliedUnit.getSource();
    URI unitUri = src.getUri();
    Map<URI, String> suppliedSources = new HashMap<URI, String>();
    suppliedSources.put(unitUri, sourceString);
    DeltaAnalysisRunnable runnable = new DeltaAnalysisRunnable(library, unitUri, suppliedSources,
        suppliedUnit, completionNode, completionLocation, parseErrors);
    SafeRunner.run(runnable);
    if (runnable.exception != null) {
      throw new DartModelException(new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Failed to parse " + library.getName(), runnable.exception)));
    }
    return runnable.analyzedNode;
  }

  /**
   * Parse the specified source. Any exceptions thrown by the {@link DartParser} will be logged and
   * a {@link DartModelException} thrown.
   * 
   * @param sourceRef the Dart source being parsed
   * @param source the source to be parsed (not <code>null</code>)
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   */
  public static DartUnit parseSource(DartSource sourceRef, String source,
      final Collection<DartCompilationError> parseErrors) throws DartModelException {
    ParserRunnable runnable = new ParserRunnable(sourceRef, source, parseErrors);
    SafeRunner.run(runnable);
    if (runnable.exception != null) {
      throw new DartModelException(new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Failed to parse " + sourceRef.getName(), runnable.exception)));
    }
    return runnable.result;
  }

  /**
   * Parse the source for the specified compilation unit. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param name a name for the source being parsed
   * @param source the source to be parsed (not <code>null</code>)
   * @return the parse result or <code>null</code> if there are any parse errors
   */
  public static DartUnit parseSource(String name, String source) throws DartModelException {
    Collection<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();
    DartUnit dartUnit = parseSource(name, source, parseErrors);
    if (parseErrors.size() > 0) {
      return null;
    }
    return dartUnit;
  }

  /**
   * Parse the specified source. Any exceptions thrown by the {@link DartParser} will be logged and
   * a {@link DartModelException} thrown.
   * 
   * @param name a name for the source being parsed
   * @param source the source to be parsed (not <code>null</code>)
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   */
  public static DartUnit parseSource(String name, String source,
      final Collection<DartCompilationError> parseErrors) throws DartModelException {
    DartSource sourceRef = new DartSourceString(name, source);
    return parseSource(sourceRef, source, parseErrors);
  }

  /**
   * Parse the source for the specified compilation unit. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param compilationUnit the compilation unit (not <code>null</code>)
   * @return the parse result or <code>null</code> if there are any parse errors
   */
  public static DartUnit parseUnit(CompilationUnit compilationUnit) throws DartModelException {
    Collection<DartCompilationError> parseErrors = new ArrayList<DartCompilationError>();
    DartUnit dartUnit = parseUnit(compilationUnit, parseErrors);
    if (parseErrors.size() > 0) {
      return null;
    }
    return dartUnit;
  }

  /**
   * Parse the source for the specified compilation unit. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param compilationUnit the compilation unit (not <code>null</code>)
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   */
  public static DartUnit parseUnit(CompilationUnit compilationUnit,
      Collection<DartCompilationError> parseErrors) throws DartModelException {
    String name = compilationUnit.getElementName();
    String source = compilationUnit.getSource();
    return parseSource(name, source, parseErrors);
  }

  /**
   * Remove the given LibraryUnit from the cache of previously-analyzed libraries.
   */
  public static void removeCachedLibrary(LibrarySource library) {
    synchronized (cachedLibraries) {
      cachedLibraries.removeKey(library);
    }
  }

  /**
   * Parse the compilation units in the specified library. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param library the library to be parsed (not <code>null</code>)
   * @param forceFullAST <code>true</code> if full ASTs should be built for all compilation units
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   * @throws DartModelException if the library could not be parsed
   */
  public static LibraryUnit resolveLibrary(DartLibraryImpl library, boolean forceFullAST,
      final Collection<DartCompilationError> parseErrors) throws DartModelException {
    ResolverRunnable runnable = new ResolverRunnable(library.getLibrarySourceFile(), null, null,
        forceFullAST, parseErrors);
    SafeRunner.run(runnable);
    if (runnable.exception != null) {
      throw new DartModelException(new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Failed to parse " + library.getElementName(), runnable.exception)));
    }
    return runnable.libraryResult;
  }

  /**
   * Parse the compilation units in the specified library. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param library the library to be parsed (not <code>null</code>)
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   * @throws DartModelException if the library could not be parsed
   */
  public static LibraryUnit resolveLibrary(DartLibraryImpl library,
      final Collection<DartCompilationError> parseErrors) throws DartModelException {
    return resolveLibrary(library, false, parseErrors);
  }

  /**
   * Parse the compilation units in the specified library. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param library the library to be parsed (not <code>null</code>)
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   * @throws DartModelException if the library could not be parsed
   */
  public static LibraryUnit resolveLibrary(DartLibraryImpl library,
      Collection<DartUnit> suppliedUnits, final Collection<DartCompilationError> parseErrors)
      throws DartModelException {
    return resolveLibrary(library.getLibrarySourceFile(), suppliedUnits, parseErrors);
  }

  /**
   * Parse the compilation units in the specified library. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param library the library to be parsed (not <code>null</code>)
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   * @throws DartModelException if the library could not be parsed
   */
  public static LibraryUnit resolveLibrary(LibrarySource library,
      Collection<DartUnit> suppliedUnits, final Collection<DartCompilationError> parseErrors)
      throws DartModelException {
    ResolverRunnable runnable = new ResolverRunnable(library, createMap(suppliedUnits), false,
        parseErrors);
    SafeRunner.run(runnable);
    if (runnable.exception != null) {
      throw new DartModelException(new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Failed to parse " + library.getName(), runnable.exception)));
    }
    return runnable.libraryResult;
  }

  /**
   * Parse the source for the specified compilation unit. Any exceptions thrown by the
   * {@link DartParser} will be logged and a {@link DartModelException} thrown.
   * 
   * @param compilationUnit the compilation unit (not <code>null</code>)
   * @param resolve <code>true</code> if symbols are to be resolved
   * @return the parse result or <code>null</code> if there are any parse errors
   */
  public static DartUnit resolveUnit(CompilationUnit compilationUnit) throws DartModelException {
    return resolveUnit(compilationUnit, null);
  }

  /**
   * Parse the source for the specified compilation unit and resolve any symbols found in it. Any
   * exceptions thrown by the {@link DartParser} will be added to the given collection.
   * 
   * @param compilationUnit the compilation unit (not <code>null</code>)
   * @param parseErrors a collection to which parse errors are added or <code>null</code> if parse
   *          errors should be ignored
   * @return the parse result
   */
  public static DartUnit resolveUnit(CompilationUnit compilationUnit,
      Collection<DartCompilationError> parseErrors) throws DartModelException {
    DartLibraryImpl library = (DartLibraryImpl) compilationUnit.getLibrary();
    if (library == null) {
      // If we cannot get the library, we cannot resolve any symbols so we
      // revert to simply parsing the compilation unit.
      return parseUnit(compilationUnit, parseErrors);
    }
    IResource resource = compilationUnit.getResource();
    URI unitUri = null;
    if (resource != null) {
      unitUri = resource.getLocationURI();
    }
    if (unitUri == null && compilationUnit instanceof ExternalCompilationUnitImpl) {
      unitUri = ((ExternalCompilationUnitImpl) compilationUnit).getUri();
    }
    if (unitUri == null) {
      unitUri = ((CompilationUnitImpl) compilationUnit).getSourceRef().getUri();
    }
    String unitSource = compilationUnit.getSource();
    Map<URI, String> suppliedSources = new HashMap<URI, String>();
    if (unitSource != null) {
      suppliedSources.put(unitUri, unitSource);
    }
    return resolveUnit(library.getLibrarySourceFile(), unitUri, suppliedSources, parseErrors);
  }

  /**
   * Parse the specified source. Any exceptions thrown by the {@link DartParser} will be logged and
   * a {@link DartModelException} thrown.
   * 
   * @param librarySource the source for the library containing the compilation unit being parsed
   * @param unitUri the URI of the compilation unit being parsed
   * @param parseErrors a collection to which parse errors are appended or <code>null</code> if
   *          parse errors should be ignored
   * @return the parse result
   */
  public static DartUnit resolveUnit(LibrarySource librarySource, URI unitUri,
      Map<URI, String> suppliedSources, final Collection<DartCompilationError> parseErrors)
      throws DartModelException {
    ResolverRunnable runnable = new ResolverRunnable(librarySource, unitUri, suppliedSources,
        false, parseErrors);
    SafeRunner.run(runnable);
    if (runnable.exception != null) {
      throw new DartModelException(new CoreException(new Status(IStatus.ERROR, DartCore.PLUGIN_ID,
          "Failed to parse " + unitUri, runnable.exception)));
    }
    return runnable.unitResult;
  }

  /**
   * Perform some work that will cause compiler classes to be loaded, Dart core and dom classes to
   * be resolved, and possibly cause some compiler classes to be jitted.
   */
  public static void warmUpCompiler() {
    // TODO(devoncarew): this does not seem to warm up much at all - we'll need to do some real
    // compiling. (try adding directives to compile html or dom, all this gets is core)
    try {
      parseSource("hello", "class hello {\n}\n");
    } catch (DartModelException ex) {
      DartCore.logError(ex);
    }
  }

  private static Map<URI, DartUnit> createMap(Collection<DartUnit> suppliedUnits) {
    if (suppliedUnits == null || suppliedUnits.isEmpty()) {
      return null;
    }
    Map<URI, DartUnit> parsedUnits = new HashMap<URI, DartUnit>(suppliedUnits.size());
    for (DartUnit unit : suppliedUnits) {
      DartSource src = unit.getSource();
      URI uri = src.getUri();
      parsedUnits.put(uri, unit);
    }
    return parsedUnits;
  }

  private static boolean equalUris(SystemLibraryManager manager, URI firstUri, URI secondUri) {
    if (firstUri == null) {
      return secondUri == null;
    } else if (secondUri == null) {
      return false;
    }
    String firstScheme = firstUri.getScheme();
    String secondScheme = secondUri.getScheme();
    if (firstScheme == null || firstScheme.equals("file")) {
      if (secondScheme != null && !secondScheme.equals("file")) {
        return false;
      }
      return firstUri.getPath().equals(secondUri.getPath());
    } else if (secondScheme == null || !firstScheme.equals(secondScheme)) {
      return false;
    } else if (SystemLibraryManager.isDartUri(firstUri)) {
      return manager.resolveDartUri(firstUri).equals(manager.resolveDartUri(secondUri));
    }
    return URIUtil.toPath(firstUri).equals(URIUtil.toPath(secondUri));
  }

  private static String getThreadName() {
    Thread thread = Thread.currentThread();
    String name = thread.getName();
    if (name != null) {
      return name;
    } else {
      return thread.toString();
    }
  }
}
