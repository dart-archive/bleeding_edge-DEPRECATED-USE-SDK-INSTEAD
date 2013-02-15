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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.engine.services.completion.ProposalKind;
import com.google.dart.tools.core.completion.CompletionProposal;

import junit.framework.TestCase;

public class ProxyProposalTest extends TestCase {

  public void test1() throws Exception {
    com.google.dart.engine.services.completion.CompletionProposal prop;
    prop = new com.google.dart.engine.services.internal.completion.CompletionProposalImpl();
    CompletionProposal proxy = new ProxyProposal(prop);
    prop.setCompletion("densha");
    prop.setDeclaringType("Rasshu");
    prop.setKind(ProposalKind.METHOD);
    prop.setLocation(97);
    prop.setParameterNames(new String[] {"a", "b", "c"});
    prop.setParameterTypes(new String[] {"int", "bool", "Object"});
    prop.setParameterStyle(1, false, true);
    prop.setReturnType("JR");
    assertTrue(proxy.hasOptionalParameters());
    assertFalse(proxy.hasNamedParameters());
    assertTrue(proxy.getPositionalParameterCount() == 1);
    assertTrue(b(proxy.getCompletion(), "densha"));
    assertTrue(b(proxy.getName(), "densha"));
    assertTrue(b(proxy.getSignature(), "densha"));
    assertTrue(b(proxy.getDeclarationSignature(), "Rasshu"));
    assertTrue(b(proxy.getReturnTypeName(), "JR"));
    assertTrue(proxy.getKind() == CompletionProposal.METHOD_REF);
    assertTrue(proxy.getCompletionLocation() == 96);
    assertTrue(proxy.getParameterTypeNames()[0][0] == 'i');
    assertTrue(proxy.getParameterNames()[1][0] == 'b');
    assertTrue(proxy.getReplaceStart() == 97);
    assertTrue(proxy.getReplaceEnd() == 103);
  }

  private boolean b(char[] a, String b) {
    char[] c = b.toCharArray();
    if (a.length != c.length) {
      return false;
    }
    for (int i = 0; i < a.length; i++) {
      if (a[i] != c[i]) {
        return false;
      }
    }
    return true;
  }
}
