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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;

import org.eclipse.swt.graphics.Image;

/**
 * Proposal for {@link TrackedNodePosition}.
 */
public final class TrackedNodeProposal {
  private final Image icon;
  private final String text;

  public TrackedNodeProposal(Image icon, String text) {
    this.icon = icon;
    this.text = text;
  }

  public Image getIcon() {
    return icon;
  }

  public String getText() {
    return text;
  }
}
