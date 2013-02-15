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
package com.google.dart.engine.services.internal.completion;

import com.google.dart.engine.services.completion.ProposalKind;

import junit.framework.TestCase;

public class CompletionProposalImplTest extends TestCase {

  public void test1() throws Exception {
    CompletionProposalImpl prop = new CompletionProposalImpl();
    assertNotNull(prop.getCompletion());
    assertNotNull(prop.getDeclaringType());
    assertNotNull(prop.getReturnType());
    assertNotNull(prop.getParameterNames());
    assertNotNull(prop.getParameterTypes());
    assertTrue(prop.getKind() == ProposalKind.NONE);
  }

  public void test2() throws Exception {
    CompletionProposalImpl prop = new CompletionProposalImpl();
    prop.setCompletion("fubar");
    assertTrue(prop.getReplacementLength() == "fubar".length());
    assertTrue(prop.getCompletion().equals("fubar"));
  }

  public void test3() throws Exception {
    CompletionProposalImpl prop = new CompletionProposalImpl();
    prop.setParameterStyle(3, true, false);
    assertTrue(prop.getPositionalParameterCount() == 3);
    assertTrue(prop.hasNamed());
    assertFalse(prop.hasPositional());
  }
}
