package com.google.dart.engine.context;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisDelta.AnalysisLevel;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.io.FileUtilities2;

import java.util.Map;

public class AnalysisDeltaTest extends EngineTestCase {

  public void test_getAnalysisLevels() {
    AnalysisDelta delta = new AnalysisDelta();
    assertEquals(0, delta.getAnalysisLevels().size());
  }

  public void test_setAnalysisLevel() {
    AnalysisDelta delta = new AnalysisDelta();
    TestSource source1 = new TestSource();
    TestSource source2 = new TestSource(FileUtilities2.createFile("bar.dart"), "");
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
