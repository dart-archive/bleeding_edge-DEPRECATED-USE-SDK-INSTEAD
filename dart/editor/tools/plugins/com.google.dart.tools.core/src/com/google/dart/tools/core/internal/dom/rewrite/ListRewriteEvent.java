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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.ast.DartNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class <code>ListRewriteEvent</code>
 */
public class ListRewriteEvent extends RewriteEvent {
  public final static int NEW = 1;
  public final static int OLD = 2;
  public final static int BOTH = NEW | OLD;

  /** original list of 'ASTNode' */
  private List<DartNode> originalNodes;

  /** list of type 'RewriteEvent' */
  private List<RewriteEvent> listEntries;

  /**
   * Creates a ListRewriteEvent from the original ASTNodes. The resulting event represents the
   * unmodified list.
   * 
   * @param originalNodes The original nodes (type ASTNode)
   */
  public ListRewriteEvent(List<DartNode> originalNodes) {
    this.originalNodes = new ArrayList<DartNode>(originalNodes);
  }

  /**
   * Creates a ListRewriteEvent from existing rewrite events.
   * 
   * @param children The rewrite events for this list.
   */
  public ListRewriteEvent(RewriteEvent[] children) {
    listEntries = new ArrayList<RewriteEvent>(children.length * 2);
    originalNodes = new ArrayList<DartNode>(children.length * 2);
    for (int i = 0; i < children.length; i++) {
      RewriteEvent curr = children[i];
      listEntries.add(curr);
      if (curr.getOriginalValue() != null) {
        originalNodes.add((DartNode) curr.getOriginalValue());
      }
    }
  }

  @Override
  public int getChangeKind() {
    if (listEntries != null) {
      for (int i = 0; i < listEntries.size(); i++) {
        RewriteEvent curr = listEntries.get(i);
        if (curr.getChangeKind() != UNCHANGED) {
          return CHILDREN_CHANGED;
        }
      }
    }
    return UNCHANGED;
  }

  public int getChangeKind(int index) {
    return ((NodeRewriteEvent) getEntries().get(index)).getChangeKind();
  }

  @Override
  public RewriteEvent[] getChildren() {
    List<RewriteEvent> entries = getEntries();
    return entries.toArray(new RewriteEvent[entries.size()]);
  }

  public int getIndex(DartNode node, int kind) {
    List<RewriteEvent> entries = getEntries();
    for (int i = entries.size() - 1; i >= 0; i--) {
      RewriteEvent curr = entries.get(i);
      if (((kind & OLD) != 0) && (curr.getOriginalValue() == node)) {
        return i;
      }
      if (((kind & NEW) != 0) && (curr.getNewValue() == node)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public Object getNewValue() {
    List<RewriteEvent> entries = getEntries();
    ArrayList<Object> res = new ArrayList<Object>(entries.size());
    for (int i = 0; i < entries.size(); i++) {
      RewriteEvent curr = entries.get(i);
      Object newVal = curr.getNewValue();
      if (newVal != null) {
        res.add(newVal);
      }
    }
    return res;
  }

  @Override
  public Object getOriginalValue() {
    return originalNodes;
  }

  public RewriteEvent insert(DartNode insertedNode, int insertIndex) {
    NodeRewriteEvent change = new NodeRewriteEvent(null, insertedNode);
    if (insertIndex != -1) {
      getEntries().add(insertIndex, change);
    } else {
      getEntries().add(change);
    }
    return change;
  }

  @Override
  public boolean isListRewrite() {
    return true;
  }

  // API to modify the list nodes

  public RewriteEvent removeEntry(DartNode originalEntry) {
    return replaceEntry(originalEntry, null);
  }

  public RewriteEvent replaceEntry(DartNode entry, DartNode newEntry) {
    if (entry == null) {
      throw new IllegalArgumentException();
    }

    List<RewriteEvent> entries = getEntries();
    int nEntries = entries.size();
    for (int i = 0; i < nEntries; i++) {
      NodeRewriteEvent curr = (NodeRewriteEvent) entries.get(i);
      if (curr.getOriginalValue() == entry || curr.getNewValue() == entry) {
        curr.setNewValue(newEntry);
        if (curr.getNewValue() == null && curr.getOriginalValue() == null) { // removed
                                                                             // an
                                                                             // inserted
                                                                             // node
          entries.remove(i);
          return null;
        }
        return curr;
      }
    }
    return null;
  }

  public void revertChange(NodeRewriteEvent event) {
    Object originalValue = event.getOriginalValue();
    if (originalValue == null) {
      List<RewriteEvent> entries = getEntries();
      entries.remove(event);
    } else {
      event.setNewValue(originalValue);
    }
  }

  public void setNewValue(DartNode newValue, int insertIndex) {
    NodeRewriteEvent curr = (NodeRewriteEvent) getEntries().get(insertIndex);
    curr.setNewValue(newValue);
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(" [list change\n\t"); //$NON-NLS-1$

    RewriteEvent[] events = getChildren();
    for (int i = 0; i < events.length; i++) {
      if (i != 0) {
        buf.append("\n\t"); //$NON-NLS-1$
      }
      buf.append(events[i]);
    }
    buf.append("\n]"); //$NON-NLS-1$
    return buf.toString();
  }

  private List<RewriteEvent> getEntries() {
    if (listEntries == null) {
      // create if not yet existing
      int nNodes = originalNodes.size();
      listEntries = new ArrayList<RewriteEvent>(nNodes * 2);
      for (int i = 0; i < nNodes; i++) {
        DartNode node = originalNodes.get(i);
        // all nodes unchanged
        listEntries.add(new NodeRewriteEvent(node, node));
      }
    }
    return listEntries;
  }
}
