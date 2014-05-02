package com.google.dart.engine.context;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.io.FileUtilities2;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.util.Collection;
import java.util.Map;

/**
 * TODO(scheglov) Restore this test once TestSource is not file based.
 */
@DartOmit
public class AnalysisDeltaTest extends EngineTestCase {

  private TestSource source1 = new TestSource();
  private TestSource source2 = new TestSource(FileUtilities2.createFile("bar.dart"), "");
  private TestSource source3 = new TestSource(FileUtilities2.createFile("baz.dart"), "");

  public void test_getAddedSources() {
    AnalysisDelta delta = new AnalysisDelta();
    delta.setAnalysisLevel(source1, AnalysisLevel.ALL);
    delta.setAnalysisLevel(source2, AnalysisLevel.ERRORS);
    delta.setAnalysisLevel(source3, AnalysisLevel.NONE);
    Collection<? extends Source> addedSources = delta.getAddedSources();
    assertEquals(2, addedSources.size());
    assertContains(addedSources.toArray(), source1, source2);
  }

  public void test_getAnalysisLevels() {
    AnalysisDelta delta = new AnalysisDelta();
    assertEquals(0, delta.getAnalysisLevels().size());
  }

  public void test_setAnalysisLevel() {
    AnalysisDelta delta = new AnalysisDelta();
    delta.setAnalysisLevel(source1, AnalysisLevel.ALL);
    delta.setAnalysisLevel(source2, AnalysisLevel.ERRORS);
    Map<Source, AnalysisLevel> levels = delta.getAnalysisLevels();
    assertEquals(2, levels.size());
    assertEquals(AnalysisLevel.ALL, levels.get(source1));
    assertEquals(AnalysisLevel.ERRORS, levels.get(source2));
  }

  public void test_toString() {
    AnalysisDelta delta = new AnalysisDelta();
    delta.setAnalysisLevel(new TestSource(), AnalysisLevel.ALL);
    String result = delta.toString();
    assertNotNull(result);
    assertTrue(result.length() > 0);
  }
}
