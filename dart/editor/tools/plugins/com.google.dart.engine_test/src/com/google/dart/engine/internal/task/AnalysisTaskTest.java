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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.SourceFactory;

public class AnalysisTaskTest extends EngineTestCase {
  public void test_perform_exception() throws AnalysisException {
    InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    AnalysisTask task = new AnalysisTask(context) {
      @Override
      public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
        assertNotNull(getException());
        return null;
      }

      @Override
      protected String getTaskDescription() {
        return null;
      }

      @Override
      protected void internalPerform() throws AnalysisException {
        throw new AnalysisException("Forced exception");
      }
    };
    task.perform(new TestTaskVisitor<Void>());
  }
}
