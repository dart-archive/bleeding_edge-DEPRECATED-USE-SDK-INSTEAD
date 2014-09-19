package com.google.dart.engine.services.completion;

import com.google.common.base.Joiner;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
import com.google.dart.engine.internal.index.IndexConstants;
import com.google.dart.engine.internal.index.file.MemoryNodeManager;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.util.LocationSpec;
import com.google.dart.engine.services.util.MockCompletionRequestor;
import com.google.dart.engine.source.Source;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CompletionTestCase extends ResolverTestCase {
  /**
   * Replaces "!" with the {@link CompletionProposal#CURSOR_MARKER}.
   */
  protected static String resultWithCursor(String result) {
    return result.replace('!', CompletionProposal.CURSOR_MARKER);
  }

  protected static String src(String... parts) {
    return Joiner.on('\n').join(parts);
  }

  protected Index index;
  protected SearchEngine searchEngine;

  @Override
  public void setUp() {
    super.setUp();
    index = IndexFactory.newIndex(IndexFactory.newSplitIndexStore(new MemoryNodeManager()));
    new Thread() {
      @Override
      public void run() {
        index.run();
        index = null;
      }
    }.start();
    searchEngine = SearchEngineFactory.createSearchEngine(index);
  }

  /**
   * Resolve and index all of the compilation units that comprise the libraries specified by the
   * given sources, returning the resolved compilation unit for the last library in the list.
   * 
   * @param sources the sources of the libraries to be resolved and indexed
   * @throws AnalysisException if the libraries could not be resolved or indexed
   */
  protected CompilationUnit resolveAndIndex(List<Source> sources) throws AnalysisException {
    AnalysisContext context = getAnalysisContext();
    CompilationUnit libraryUnit = null;
    for (Source source : sources) {
      LibraryElement library = resolve(source);
      libraryUnit = getAnalysisContext().resolveCompilationUnit(source, library);
      index.indexUnit(context, libraryUnit);
      for (CompilationUnitElement partElement : library.getParts()) {
        CompilationUnit partUnit = getAnalysisContext().resolveCompilationUnit(
            partElement.getSource(),
            library);
        index.indexUnit(context, partUnit);
      }
    }
    return libraryUnit;
  }

  @Override
  protected void tearDown() throws Exception {
    index.getRelationships(
        IndexConstants.UNIVERSE,
        IndexConstants.IS_READ_BY,
        new RelationshipCallback() {
          @Override
          public void hasRelationships(Element a, Relationship b, Location[] c) {
            index.stop();
          }
        });
    searchEngine = null;
    super.tearDown();
  }

  /**
   * Run a set of completion tests on the given <code>originalSource</code>. The source string has
   * completion points embedded in it, which are identified by '!X' where X is a single character.
   * Each X is matched to positive or negative results in the array of
   * <code>validationStrings</code>. Validation strings contain the name of a prediction with a two
   * character prefix. The first character of the prefix corresponds to an X in the
   * <code>originalSource</code>. The second character is either a '+' or a '-' indicating whether
   * the string is a positive or negative result.
   * 
   * @param originalSource The source for a completion test that contains completion points
   * @param validationStrings The positive and negative predictions
   */
  protected void test(String originalSource, List<Source> sources, String... results)
      throws URISyntaxException, AnalysisException {
    Collection<LocationSpec> completionTests = LocationSpec.from(originalSource, results);
    assertTrue(
        "Expected exclamation point ('!') within the source"
            + " denoting the position at which code completion should occur",
        !completionTests.isEmpty());
    Source source = addSource(completionTests.iterator().next().source);
    sources.add(source);
    CompilationUnit compilationUnit = resolveAndIndex(sources);
    AnalysisContext analysisContext = getAnalysisContext();
    CompletionFactory factory = new CompletionFactory();
    for (LocationSpec test : completionTests) {
      MockCompletionRequestor requestor = new MockCompletionRequestor();
      CompletionEngine engine = new CompletionEngine(requestor, factory);
      engine.complete(new AssistContext(
          searchEngine,
          analysisContext,
          null,
          source,
          compilationUnit,
          test.testLocation,
          0));
      if (test.positiveResults.size() > 0) {
        assertTrue(
            "Test " + test.id + " expected code completion suggestions "
                + String.valueOf(test.positiveResults),
            requestor.validate());
      }
      for (String result : test.positiveResults) {
        requestor.assertSuggested(result, test.id);
      }
      for (String result : test.negativeResults) {
        requestor.assertNotSuggested(result, test.id);
      }
      TestAll.Count += test.positiveResults.size() + test.negativeResults.size();
    }
  }

  /**
   * Run a set of completion tests on the given <code>originalSource</code>. The source string has
   * completion points embedded in it, which are identified by '!X' where X is a single character.
   * Each X is matched to positive or negative results in the array of
   * <code>validationStrings</code>. Validation strings contain the name of a prediction with a two
   * character prefix. The first character of the prefix corresponds to an X in the
   * <code>originalSource</code>. The second character is either a '+' or a '-' indicating whether
   * the string is a positive or negative result.
   * 
   * @param originalSource The source for a completion test that contains completion points
   * @param validationStrings The positive and negative predictions
   */
  protected void test(String originalSource, String... results) throws URISyntaxException,
      AnalysisException {
    test(originalSource, new ArrayList<Source>(), results);
  }
}
