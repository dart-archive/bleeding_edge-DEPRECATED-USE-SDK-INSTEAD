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
package com.google.dart.tools.debug.core.internal;


/**
 * The IValue implementation for the Chrome debug elements.
 */
public class ChromeDebugValue {
//extends ChromeDebugElement implements IValue {
//  private JsValue jsValue;
//  private IVariable[] variables;
//  private boolean isObject = false;
//
//  /**
//   * Create a new ChromeDebugValue.
//   * 
//   * @param target
//   * @param jsValue
//   */
//  public ChromeDebugValue(IDebugTarget target, JsValue jsValue) {
//    super(target);
//
//    this.jsValue = jsValue;
//
//    if (JsValue.Type.isObjectType(jsValue.getType())) {
//      isObject = true;
//    }
//  }
//
//  @Override
//  public String getReferenceTypeName() throws DebugException {
//    return jsValue.getType().toString();
//  }
//
//  @Override
//  public String getValueString() throws DebugException {
//    if (!isObject) {
//      return jsValue.getValueString();
//    }
//
//    JsObject jsObject = jsValue.asObject();
//    String valueString = jsObject.getValueString();
//
//    if (jsObject instanceof JsArray || jsObject instanceof JsFunction) {
//      return valueString;
//    }
//
//    // To get the name of the object type. Need to fetch the internal properties. This take time as this
//    // gets value from remote. object > proto > constructor > name
//    // TODO: can we make these calls faster
////    Collection<? extends JsVariable> internalProperties = jsObject.getInternalProperties();
////    JsVariable proto = (JsVariable) internalProperties.toArray()[0];
////    if (proto != null) {
////      JsVariable var = proto.getValue().asObject().getProperty("constructor");
////      if (var != null) {
////        JsValue constructorValue = var.getValue();
////        if (constructorValue != null && JsValue.Type.isObjectType(constructorValue.getType())) {
////          JsVariable nameVar = constructorValue.asObject().getProperty("name");
////          if (nameVar != null) {
////            String name = nameVar.getValue().getValueString();
////            return getTypeName(name);
////          }
////        }
////      }
////    }
//
//    return valueString;
//  }
//
//  @Override
//  public IVariable[] getVariables() throws DebugException {
//    if (variables == null) {
//      variables = createVariables();
//    }
//
//    return variables;
//  }
//
//  @Override
//  public boolean hasVariables() throws DebugException {
//    return isObject;
//  }
//
//  @Override
//  public boolean isAllocated() throws DebugException {
//    return true;
//  }
//
//  boolean isObject() {
//    return isObject;
//  }
//
//  private IVariable[] createVariables() {
//    if (!isObject) {
//      return new IVariable[0];
//    } else {
//      List<IVariable> vars = new ArrayList<IVariable>();
//
//      for (JsVariable var : jsValue.asObject().getProperties()) {
//        if (CompilerUtilities.isDartFieldName(var.getName())) {
//          vars.add(new ChromeDebugVariable(getDebugTarget(), var,
//              CompilerUtilities.getDartFieldName(var.getName())));
//        }
//      }
//      return vars.toArray(new IVariable[vars.size()]);
//    }
//  }
//
//  // cleans up a string Total_appe24dcb$Style$Dart to return Style
////  private String getTypeName(String name) {
////    return name.split("\\$")[1];
////  }

}
