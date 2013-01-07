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
package com.google.dart.tools.ui.web.css.model;

import com.google.dart.tools.ui.web.utils.Node;
import com.google.dart.tools.ui.web.utils.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A CSS model section object. A section contains 0 or more selectors, and a body.
 */
public class CssSection extends Node {
  private List<Token> selectors = new ArrayList<Token>();
  private CssBody body;

  public CssSection() {

  }

  public CssBody getBody() {
    return body;
  }

  @Override
  public String getLabel() {
    if (selectors.size() == 0) {
      return "";
    } else if (selectors.size() == 1) {
      return selectors.get(0).getValue();
    } else {
      return selectors.get(0).getValue() + ", ...";
    }
  }

  public List<Token> getSelectors() {
    return selectors;
  }

  protected void addSelector(Token token) {
    selectors.add(token);

    if (getStartToken() == null) {
      setStart(token);
      setLabel(token.getValue());
    }

    setEnd(token);
  }

  protected void setBody(CssBody body) {
    this.body = body;

    addChild(body);
  }

}
