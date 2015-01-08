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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.EngineTestCase;

public class AnalysisOptionsImplTest extends EngineTestCase {
  public void test_AnalysisOptionsImpl_copy() {
    boolean booleanValue = true;
    for (int i = 0; i < 2; i++, booleanValue = !booleanValue) {
      AnalysisOptionsImpl options = new AnalysisOptionsImpl();
      options.setAnalyzeAngular(booleanValue);
      options.setAnalyzeFunctionBodies(booleanValue);
      options.setAnalyzePolymer(booleanValue);
      options.setCacheSize(i);
      options.setDart2jsHint(booleanValue);
      options.setGenerateSdkErrors(booleanValue);
      options.setHint(booleanValue);
      options.setIncremental(booleanValue);
      options.setPreserveComments(booleanValue);
      AnalysisOptionsImpl copy = new AnalysisOptionsImpl(options);
      assertEquals(options.getAnalyzeAngular(), copy.getAnalyzeAngular());
      assertEquals(options.getAnalyzeFunctionBodies(), copy.getAnalyzeFunctionBodies());
      assertEquals(options.getAnalyzePolymer(), copy.getAnalyzePolymer());
      assertEquals(options.getCacheSize(), copy.getCacheSize());
      assertEquals(options.getDart2jsHint(), copy.getDart2jsHint());
      assertEquals(options.getGenerateSdkErrors(), copy.getGenerateSdkErrors());
      assertEquals(options.getHint(), copy.getHint());
      assertEquals(options.getIncremental(), copy.getIncremental());
      assertEquals(options.getPreserveComments(), copy.getPreserveComments());
    }
  }

  public void test_getAnalyzeAngular() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getAnalyzeAngular();
    options.setAnalyzeAngular(value);
    assertEquals(value, options.getAnalyzeAngular());
  }

  public void test_getAnalyzeFunctionBodies() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getAnalyzeFunctionBodies();
    options.setAnalyzeFunctionBodies(value);
    assertEquals(value, options.getAnalyzeFunctionBodies());
  }

  public void test_getAnalyzePolymer() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getAnalyzePolymer();
    options.setAnalyzePolymer(value);
    assertEquals(value, options.getAnalyzePolymer());
  }

  public void test_getCacheSize() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    assertEquals(AnalysisOptionsImpl.DEFAULT_CACHE_SIZE, options.getCacheSize());
    int value = options.getCacheSize() + 1;
    options.setCacheSize(value);
    assertEquals(value, options.getCacheSize());
  }

  public void test_getDart2jsHint() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getDart2jsHint();
    options.setDart2jsHint(value);
    assertEquals(value, options.getDart2jsHint());
  }

  public void test_getGenerateSdkErrors() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getGenerateSdkErrors();
    options.setGenerateSdkErrors(value);
    assertEquals(value, options.getGenerateSdkErrors());
  }

  public void test_getHint() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getHint();
    options.setHint(value);
    assertEquals(value, options.getHint());
  }

  public void test_getIncremental() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getIncremental();
    options.setIncremental(value);
    assertEquals(value, options.getIncremental());
  }

  public void test_getPreserveComments() {
    AnalysisOptionsImpl options = new AnalysisOptionsImpl();
    boolean value = !options.getPreserveComments();
    options.setPreserveComments(value);
    assertEquals(value, options.getPreserveComments());
  }
}
