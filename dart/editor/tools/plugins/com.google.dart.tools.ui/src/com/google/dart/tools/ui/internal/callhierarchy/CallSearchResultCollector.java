/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;

import java.util.HashMap;
import java.util.Map;

class CallSearchResultCollector {

  /**
   * A map from handle identifier ({@link String}) to {@link MethodCall}.
   */
  private Map<String, MethodCall> calledMembers;

  public CallSearchResultCollector() {
    this.calledMembers = createCalledMethodsData();
  }

  /**
   * @return a map from handle identifier ({@link String}) to {@link MethodCall}
   */
  public Map<String, MethodCall> getCallers() {
    return calledMembers;
  }

  protected void addMember(DartElement member, DartElement calledMember, int start, int end) {
    addMember(member, calledMember, start, end, CallLocation.UNKNOWN_LINE_NUMBER);
  }

  protected void addMember(DartElement member, DartElement calledMember, int start, int end,
      int lineNumber) {
    if ((member != null) && (calledMember != null)) {
      if (!isIgnored(calledMember)) {
        MethodCall methodCall = calledMembers.get(calledMember.getHandleIdentifier());

        if (methodCall == null) {
          methodCall = new MethodCall(calledMember);
          calledMembers.put(calledMember.getHandleIdentifier(), methodCall);
        }

        methodCall.addCallLocation(new CallLocation(
            (CompilationUnitElement) member,
            (CompilationUnitElement) calledMember,
            start,
            end,
            lineNumber));
      }
    }
  }

  protected Map<String, MethodCall> createCalledMethodsData() {
    return new HashMap<String, MethodCall>();
  }

  private Type getTypeOfElement(DartElement element) {
    if (element.getElementType() == DartElement.TYPE) {
      return (Type) element;
    }
    if (element instanceof TypeMember) {
      return ((TypeMember) element).getDeclaringType();
    }
    return null;
  }

  private boolean isIgnored(DartElement enclosingElement) {
    Type type = getTypeOfElement(enclosingElement);
    String fullyQualifiedName;
    if (type == null) {
      fullyQualifiedName = enclosingElement.getElementName();
    } else {
      fullyQualifiedName = getTypeOfElement(enclosingElement).getElementName();
    }
    return CallHierarchy.getDefault().isIgnored(fullyQualifiedName);
  }
}
