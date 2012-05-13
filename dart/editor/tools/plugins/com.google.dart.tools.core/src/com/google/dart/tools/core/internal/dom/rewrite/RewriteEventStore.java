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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.text.edits.TextEditGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Instances of the class <code>RewriteEventStore</code> store all rewrite events, descriptions of
 * events and know which nodes are copy or move sources or tracked.
 */
public final class RewriteEventStore {
  public static class CopySourceInfo implements Comparable<CopySourceInfo> {
    public final PropertyLocation location; // can be null, only used to mark as
                                            // removed on move
    private final DartNode node;
    public final boolean isMove;

    public CopySourceInfo(PropertyLocation location, DartNode node, boolean isMove) {
      this.location = location;
      this.node = node;
      this.isMove = isMove;
    }

    @Override
    public int compareTo(CopySourceInfo r2) {
      int startDiff = getNode().getSourceInfo().getOffset()
          - r2.getNode().getSourceInfo().getOffset();
      if (startDiff != 0) {
        return startDiff; // insert before if start node is first
      }

      if (r2.isMove != isMove) {
        return isMove ? -1 : 1; // first move then copy
      }
      return 0;
    }

    public DartNode getNode() {
      return node;
    }

    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();
      if (isMove) {
        buf.append("move source: "); //$NON-NLS-1$
      } else {
        buf.append("copy source: "); //$NON-NLS-1$
      }
      buf.append(node);
      return buf.toString();
    }
  }

  /**
   * Interface that allows to override the way how children are accessed from a parent. Use this
   * interface when the rewriter is set up on an already modified AST's (as it is the case in the
   * old ASTRewrite infrastructure)
   */
  public static interface INodePropertyMapper {
    /**
     * Return the node attribute for a given property name.
     * 
     * @param parent The parent node
     * @param childProperty The child property to access
     * @return The child node at the given property location.
     */
    Object getOriginalValue(DartNode parent, StructuralPropertyDescriptor childProperty);
  }

  public static final class PropertyLocation {
    private final DartNode parent;
    private final StructuralPropertyDescriptor property;

    public PropertyLocation(DartNode parent, StructuralPropertyDescriptor property) {
      this.parent = parent;
      this.property = property;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj.getClass().equals(getClass())) {
        PropertyLocation other = (PropertyLocation) obj;
        return other.getParent().equals(getParent()) && other.getProperty().equals(getProperty());
      }
      return false;
    }

    public DartNode getParent() {
      return parent;
    }

    public StructuralPropertyDescriptor getProperty() {
      return property;
    }

    @Override
    public int hashCode() {
      return getParent().hashCode() + getProperty().hashCode();
    }

  }

  /*
   * Store element to associate event and node position/
   */
  private static class EventHolder {
    public final DartNode parent;
    public final StructuralPropertyDescriptor childProperty;
    public final RewriteEvent event;

    public EventHolder(DartNode parent, StructuralPropertyDescriptor childProperty,
        RewriteEvent change) {
      this.parent = parent;
      this.childProperty = childProperty;
      this.event = change;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append(parent).append(" - "); //$NON-NLS-1$
      builder.append(childProperty.getId()).append(": "); //$NON-NLS-1$
      builder.append(event).append('\n');
      return builder.toString();
    }
  }

  private static class NodeRangeInfo implements Comparable<NodeRangeInfo> {
    private final DartNode first;
    private final DartNode last;
    public final CopySourceInfo copyInfo; // containing the internal placeholder
                                          // and the 'isMove' flag
    public final DartNode replacingNode;
    public final TextEditGroup editGroup;

    public NodeRangeInfo(DartNode parent, StructuralPropertyDescriptor childProperty,
        DartNode first, DartNode last, CopySourceInfo copyInfo, DartNode replacingNode,
        TextEditGroup editGroup) {
      this.first = first;
      this.last = last;
      this.copyInfo = copyInfo;
      this.replacingNode = replacingNode;
      this.editGroup = editGroup;
    }

    @Override
    public int compareTo(NodeRangeInfo r2) {
      int startDiff = getStartNode().getSourceInfo().getOffset()
          - r2.getStartNode().getSourceInfo().getOffset();
      if (startDiff != 0) {
        return startDiff; // insert before if start node is first
      }
      int endDiff = getEndNode().getSourceInfo().getOffset()
          - r2.getEndNode().getSourceInfo().getOffset();
      if (endDiff != 0) {
        return -endDiff; // insert before if length is longer
      }
      if (r2.isMove() != isMove()) {
        return isMove() ? -1 : 1; // first move then copy
      }
      return 0;
    }

    public DartNode getEndNode() {
      return last;
    }

    public DartBlock getInternalPlaceholder() {
      return (DartBlock) copyInfo.getNode();
    }

    public DartNode getStartNode() {
      return first;
    }

    public boolean isMove() {
      return copyInfo.isMove;
    }

    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();
      if (first != last) {
        buf.append("range "); //$NON-NLS-1$
      }
      if (isMove()) {
        buf.append("move source: "); //$NON-NLS-1$
      } else {
        buf.append("copy source: "); //$NON-NLS-1$
      }
      buf.append(first);
      buf.append(" - "); //$NON-NLS-1$
      buf.append(last);
      return buf.toString();
    }

    public void updatePlaceholderSourceRanges(TargetSourceRangeComputer sourceRangeComputer) {
      TargetSourceRangeComputer.SourceRange startRange = sourceRangeComputer.computeSourceRange(getStartNode());
      TargetSourceRangeComputer.SourceRange endRange = sourceRangeComputer.computeSourceRange(getEndNode());
      int startPos = startRange.getStartPosition();
      int endPos = endRange.getStartPosition() + endRange.getLength();

      DartBlock internalPlaceholder = getInternalPlaceholder();
      internalPlaceholder.setSourceInfo(new SourceInfo(
          internalPlaceholder.getSourceInfo().getSource(),
          startPos,
          endPos - startPos));
    }

  }

  /**
   * Iterates over all event parent nodes, tracked nodes and all copy/move sources
   */
  private class ParentIterator implements Iterator<DartNode> {

    private Iterator<DartNode> eventIter;
    private Iterator<CopySourceInfo> sourceNodeIter;
    private Iterator<PropertyLocation> rangeNodeIter;
    private Iterator<DartNode> trackedNodeIter;

    public ParentIterator() {
      eventIter = RewriteEventStore.this.eventLookup.keySet().iterator();
      if (RewriteEventStore.this.nodeCopySources != null) {
        sourceNodeIter = RewriteEventStore.this.nodeCopySources.iterator();
      } else {
        List<CopySourceInfo> emptyList = Collections.emptyList();
        sourceNodeIter = emptyList.iterator();
      }
      if (RewriteEventStore.this.nodeRangeInfos != null) {
        rangeNodeIter = RewriteEventStore.this.nodeRangeInfos.keySet().iterator();
      } else {
        List<PropertyLocation> emptyList = Collections.emptyList();
        rangeNodeIter = emptyList.iterator();
      }
      if (RewriteEventStore.this.trackedNodes != null) {
        trackedNodeIter = RewriteEventStore.this.trackedNodes.keySet().iterator();
      } else {
        List<DartNode> emptyList = Collections.emptyList();
        trackedNodeIter = emptyList.iterator();
      }
    }

    @Override
    public boolean hasNext() {
      return eventIter.hasNext() || sourceNodeIter.hasNext() || rangeNodeIter.hasNext()
          || trackedNodeIter.hasNext();
    }

    @Override
    public DartNode next() {
      if (eventIter.hasNext()) {
        return eventIter.next();
      }
      if (sourceNodeIter.hasNext()) {
        return sourceNodeIter.next().getNode();
      }
      if (rangeNodeIter.hasNext()) {
        return rangeNodeIter.next().getParent();
      }
      return trackedNodeIter.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public final static int NEW = 1;
  public final static int ORIGINAL = 2;
  public final static int BOTH = NEW | ORIGINAL;

  public static boolean isNewNode(DartNode node) {
    DartCore.notYetImplemented();
    // return (node.getFlags() & DartNode.ORIGINAL) == 0;
    return false;
  }

  /** all events by parent */
  final Map<DartNode, List<EventHolder>> eventLookup;

  /** cache for last accessed event */
  private EventHolder lastEvent;

  /** Maps events to group descriptions */
  private Map<RewriteEvent, TextEditGroup> editGroups;

  /** Stores which nodes are source of a copy or move (list of CopySourceInfo) */
  List<CopySourceInfo> nodeCopySources;

  /**
   * Stores node ranges that are used to copy or move (map of <PropertyLocation, CopyRangeInfo>)
   */
  Map<PropertyLocation, List<NodeRangeInfo>> nodeRangeInfos;

  /** Stores which nodes are tracked and the corresponding edit group */
  Map<DartNode, TextEditGroup> trackedNodes;

  /**
   * Stores which inserted nodes bound to the previous node. If not, a node is always bound to the
   * next node
   */
  private Set<DartNode> insertBoundToPrevious;

  /** optional mapper to allow fix already modified AST trees */
  private INodePropertyMapper nodePropertyMapper;

  @SuppressWarnings("unused")
  private static final String INTERNAL_PLACEHOLDER_PROPERTY = "rewrite_internal_placeholder"; //$NON-NLS-1$

  public RewriteEventStore() {
    eventLookup = new HashMap<DartNode, List<EventHolder>>();
    lastEvent = null;

    editGroups = null; // lazy initialization

    trackedNodes = null;
    insertBoundToPrevious = null;

    nodePropertyMapper = null;
    nodeCopySources = null;
    nodeRangeInfos = null;
  }

  public void addEvent(DartNode parent, StructuralPropertyDescriptor childProperty,
      RewriteEvent event) {
    validateHasChildProperty(parent, childProperty);

    if (event.isListRewrite()) {
      validateIsListProperty(childProperty);
    }

    EventHolder holder = new EventHolder(parent, childProperty, event);

    List<EventHolder> entriesList = eventLookup.get(parent);
    if (entriesList != null) {
      for (int i = 0; i < entriesList.size(); i++) {
        EventHolder curr = entriesList.get(i);
        if (curr.childProperty == childProperty) {
          entriesList.set(i, holder);
          lastEvent = null;
          return;
        }
      }
    } else {
      entriesList = new ArrayList<EventHolder>(3);
      eventLookup.put(parent, entriesList);
    }
    entriesList.add(holder);
  }

  public void clear() {
    eventLookup.clear();
    lastEvent = null;
    trackedNodes = null;

    editGroups = null; // lazy initialization
    insertBoundToPrevious = null;
    nodeCopySources = null;
  }

  public final CopySourceInfo createRangeCopy(DartNode parent,
      StructuralPropertyDescriptor childProperty, DartNode first, DartNode last, boolean isMove,
      DartNode internalPlaceholder, DartNode replacingNode, TextEditGroup editGroup) {
    CopySourceInfo copyInfo = createCopySourceInfo(null, internalPlaceholder, isMove);
    DartCore.notYetImplemented();
    // internalPlaceholder.setProperty(INTERNAL_PLACEHOLDER_PROPERTY,
    // internalPlaceholder);

    NodeRangeInfo copyRangeInfo = new NodeRangeInfo(
        parent,
        childProperty,
        first,
        last,
        copyInfo,
        replacingNode,
        editGroup);

    ListRewriteEvent listEvent = getListEvent(parent, childProperty, true);

    int indexFirst = listEvent.getIndex(first, ListRewriteEvent.OLD);
    if (indexFirst == -1) {
      throw new IllegalArgumentException("Start node is not a original child of the given list"); //$NON-NLS-1$
    }
    int indexLast = listEvent.getIndex(last, ListRewriteEvent.OLD);
    if (indexLast == -1) {
      throw new IllegalArgumentException("End node is not a original child of the given list"); //$NON-NLS-1$
    }

    if (indexFirst > indexLast) {
      throw new IllegalArgumentException("Start node must be before end node"); //$NON-NLS-1$
    }

    if (nodeRangeInfos == null) {
      nodeRangeInfos = new HashMap<PropertyLocation, List<NodeRangeInfo>>();
    }
    PropertyLocation loc = new PropertyLocation(parent, childProperty);
    List<NodeRangeInfo> innerList = nodeRangeInfos.get(loc);
    if (innerList == null) {
      innerList = new ArrayList<NodeRangeInfo>(2);
      nodeRangeInfos.put(loc, innerList);
    } else {
      assertNoOverlap(listEvent, indexFirst, indexLast, innerList);
    }
    innerList.add(copyRangeInfo);

    return copyInfo;
  }

  /**
   * Kind is either ORIGINAL, NEW, or BOTH
   * 
   * @param value
   * @param kind
   * @return the event with the given value of <code>null</code>
   */
  public RewriteEvent findEvent(Object value, int kind) {
    for (Iterator<List<EventHolder>> iter = eventLookup.values().iterator(); iter.hasNext();) {
      List<EventHolder> events = iter.next();
      for (int i = 0; i < events.size(); i++) {
        RewriteEvent event = events.get(i).event;
        if (isNodeInEvent(event, value, kind)) {
          return event;
        }
        if (event.isListRewrite()) {
          RewriteEvent[] children = event.getChildren();
          for (int k = 0; k < children.length; k++) {
            if (isNodeInEvent(children[k], value, kind)) {
              return children[k];
            }
          }
        }
      }
    }
    return null;
  }

  public List<RewriteEvent> getChangedPropertieEvents(DartNode parent) {
    List<RewriteEvent> changedPropertiesEvent = new ArrayList<RewriteEvent>();

    List<EventHolder> entriesList = eventLookup.get(parent);
    if (entriesList != null) {
      for (int i = 0; i < entriesList.size(); i++) {
        EventHolder holder = entriesList.get(i);
        if (holder.event.getChangeKind() != RewriteEvent.UNCHANGED) {
          changedPropertiesEvent.add(holder.event);
        }
      }
    }
    return changedPropertiesEvent;
  }

  public int getChangeKind(DartNode node) {
    RewriteEvent event = findEvent(node, ORIGINAL);
    if (event != null) {
      return event.getChangeKind();
    }
    return RewriteEvent.UNCHANGED;
  }

  public Iterator<DartNode> getChangeRootIterator() {
    return new ParentIterator();
  }

  public RewriteEvent getEvent(DartNode parent, StructuralPropertyDescriptor property) {
    validateHasChildProperty(parent, property);

    if (lastEvent != null && lastEvent.parent == parent && lastEvent.childProperty == property) {
      return lastEvent.event;
    }

    List<EventHolder> entriesList = eventLookup.get(parent);
    if (entriesList != null) {
      for (int i = 0; i < entriesList.size(); i++) {
        EventHolder holder = entriesList.get(i);
        if (holder.childProperty == property) {
          lastEvent = holder;
          return holder.event;
        }
      }
    }
    return null;
  }

  public TextEditGroup getEventEditGroup(RewriteEvent event) {
    if (editGroups == null) {
      return null;
    }
    return editGroups.get(event);
  }

  @SuppressWarnings("unchecked")
  public ListRewriteEvent getListEvent(DartNode parent, StructuralPropertyDescriptor childProperty,
      boolean forceCreation) {
    validateIsListProperty(childProperty);
    ListRewriteEvent event = (ListRewriteEvent) getEvent(parent, childProperty);
    if (event == null && forceCreation) {
      List<DartNode> originalValue = (List<DartNode>) accessOriginalValue(parent, childProperty);
      event = new ListRewriteEvent(originalValue);
      addEvent(parent, childProperty, event);
    }
    return event;
  }

  public Object getNewValue(DartNode parent, StructuralPropertyDescriptor property) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null) {
      return event.getNewValue();
    }
    return accessOriginalValue(parent, property);
  }

  public CopySourceInfo[] getNodeCopySources(DartNode node) {
    if (nodeCopySources == null) {
      return null;
    }
    return internalGetCopySources(nodeCopySources, node);
  }

  public NodeRewriteEvent getNodeEvent(DartNode parent, StructuralPropertyDescriptor childProperty,
      boolean forceCreation) {
    validateIsNodeProperty(childProperty);
    NodeRewriteEvent event = (NodeRewriteEvent) getEvent(parent, childProperty);
    if (event == null && forceCreation) {
      Object originalValue = accessOriginalValue(parent, childProperty);
      event = new NodeRewriteEvent(originalValue, originalValue);
      addEvent(parent, childProperty, event);
    }
    return event;
  }

  public Object getOriginalValue(DartNode parent, StructuralPropertyDescriptor property) {
    RewriteEvent event = getEvent(parent, property);
    if (event != null) {
      return event.getOriginalValue();
    }
    return accessOriginalValue(parent, property);
  }

  public PropertyLocation getPropertyLocation(Object value, int kind) {
    for (Iterator<List<EventHolder>> iter = eventLookup.values().iterator(); iter.hasNext();) {
      List<EventHolder> events = iter.next();
      for (int i = 0; i < events.size(); i++) {
        EventHolder holder = events.get(i);
        RewriteEvent event = holder.event;
        if (isNodeInEvent(event, value, kind)) {
          return new PropertyLocation(holder.parent, holder.childProperty);
        }
        if (event.isListRewrite()) {
          RewriteEvent[] children = event.getChildren();
          for (int k = 0; k < children.length; k++) {
            if (isNodeInEvent(children[k], value, kind)) {
              return new PropertyLocation(holder.parent, holder.childProperty);
            }
          }
        }
      }
    }
    if (value instanceof DartNode) {
      DartCore.notYetImplemented();
      // DartNode node= (DartNode) value;
      // return new PropertyLocation(node.getParent(),
      // node.getLocationInParent());
    }
    return null;
  }

  public final TextEditGroup getTrackedNodeData(DartNode node) {
    if (trackedNodes != null) {
      return trackedNodes.get(node);
    }
    return null;
  }

  public boolean hasChangedProperties(DartNode parent) {
    List<EventHolder> entriesList = eventLookup.get(parent);
    if (entriesList != null) {
      for (int i = 0; i < entriesList.size(); i++) {
        EventHolder holder = entriesList.get(i);
        if (holder.event.getChangeKind() != RewriteEvent.UNCHANGED) {
          return true;
        }
      }
    }
    return false;
  }

  public CopySourceInfo[] internalGetCopySources(List<CopySourceInfo> copySources, DartNode node) {
    ArrayList<CopySourceInfo> res = new ArrayList<CopySourceInfo>(3);
    for (int i = 0; i < copySources.size(); i++) {
      CopySourceInfo curr = copySources.get(i);
      if (curr.getNode() == node) {
        res.add(curr);
      }
    }
    if (res.isEmpty()) {
      return null;
    }

    CopySourceInfo[] arr = res.toArray(new CopySourceInfo[res.size()]);
    Arrays.sort(arr);
    return arr;
  }

  public boolean isInsertBoundToPrevious(DartNode node) {
    if (insertBoundToPrevious != null) {
      return insertBoundToPrevious.contains(node);
    }
    return false;
  }

  public final boolean isRangeCopyPlaceholder(DartNode node) {
    DartCore.notYetImplemented();
    // return node.getProperty(INTERNAL_PLACEHOLDER_PROPERTY) != null;
    return false;
  }

  public final CopySourceInfo markAsCopySource(DartNode parent,
      StructuralPropertyDescriptor property, DartNode node, boolean isMove) {
    return createCopySourceInfo(new PropertyLocation(parent, property), node, isMove);
  }

  /**
   * Marks a node as tracked. The edits added to the group editGroup can be used to get the position
   * of the node after the rewrite operation.
   * 
   * @param node The node to track
   * @param editGroup Collects the range markers describing the node position.
   */
  public final void markAsTracked(DartNode node, TextEditGroup editGroup) {
    if (getTrackedNodeData(node) != null) {
      throw new IllegalArgumentException("Node is already marked as tracked"); //$NON-NLS-1$
    }
    setTrackedNodeData(node, editGroup);
  }

  public void prepareMovedNodes(TargetSourceRangeComputer sourceRangeComputer) {
    if (nodeCopySources != null) {
      prepareSingleNodeCopies();
    }

    if (nodeRangeInfos != null) {
      prepareNodeRangeCopies(sourceRangeComputer);
    }
  }

  public void revertMovedNodes() {
    if (nodeRangeInfos != null) {
      removeMoveRangePlaceholders();
    }
  }

  public void setEventEditGroup(RewriteEvent event, TextEditGroup editGroup) {
    if (editGroups == null) {
      editGroups = new IdentityHashMap<RewriteEvent, TextEditGroup>(5);
    }
    editGroups.put(event, editGroup);
  }

  public void setInsertBoundToPrevious(DartNode node) {
    if (insertBoundToPrevious == null) {
      insertBoundToPrevious = new HashSet<DartNode>();
    }
    insertBoundToPrevious.add(node);
  }

  /**
   * Override the default way how to access children from a parent node.
   * 
   * @param nodePropertyMapper The new <code>INodePropertyMapper</code> or <code>null</code>. to use
   *          the default.
   */
  public void setNodePropertyMapper(INodePropertyMapper nodePropertyMapper) {
    this.nodePropertyMapper = nodePropertyMapper;
  }

  public void setTrackedNodeData(DartNode node, TextEditGroup editGroup) {
    if (trackedNodes == null) {
      trackedNodes = new IdentityHashMap<DartNode, TextEditGroup>();
    }
    trackedNodes.put(node, editGroup);
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (Iterator<List<EventHolder>> iter = eventLookup.values().iterator(); iter.hasNext();) {
      List<EventHolder> events = iter.next();
      for (int i = 0; i < events.size(); i++) {
        buf.append(events.get(i).toString()).append('\n');
      }
    }
    return buf.toString();
  }

  /*
   * Gets an original child from the AST. Temporarily overridden to port the old rewriter to the new
   * infrastructure.
   */
  private Object accessOriginalValue(DartNode parent, StructuralPropertyDescriptor childProperty) {
    if (nodePropertyMapper != null) {
      return nodePropertyMapper.getOriginalValue(parent, childProperty);
    }
    return PropertyDescriptorHelper.getStructuralProperty(parent, childProperty);
  }

  private void assertNoOverlap(ListRewriteEvent listEvent, int indexFirst, int indexLast,
      List<NodeRangeInfo> innerList) {
    for (Iterator<NodeRangeInfo> iter = innerList.iterator(); iter.hasNext();) {
      NodeRangeInfo curr = iter.next();
      int currStart = listEvent.getIndex(curr.getStartNode(), ListRewriteEvent.BOTH);
      int currEnd = listEvent.getIndex(curr.getEndNode(), ListRewriteEvent.BOTH);
      if (currStart < indexFirst && currEnd < indexLast && currEnd >= indexFirst
          || currStart > indexFirst && currStart <= currEnd && currEnd > indexLast) {
        throw new IllegalArgumentException("Range overlapps with an existing copy or move range"); //$NON-NLS-1$
      }
    }
  }

  private final CopySourceInfo createCopySourceInfo(PropertyLocation location, DartNode node,
      boolean isMove) {
    CopySourceInfo copySource = new CopySourceInfo(location, node, isMove);

    if (nodeCopySources == null) {
      nodeCopySources = new ArrayList<CopySourceInfo>();
    }
    nodeCopySources.add(copySource);
    return copySource;
  }

  private void doMarkMovedAsRemoved(CopySourceInfo curr, DartNode parent,
      StructuralPropertyDescriptor childProperty) {
    if (childProperty.isChildListProperty()) {
      ListRewriteEvent event = getListEvent(parent, childProperty, true);
      int index = event.getIndex(curr.getNode(), ListRewriteEvent.OLD);
      if (index != -1 && event.getChangeKind(index) == RewriteEvent.UNCHANGED) {
        event.setNewValue(null, index);
      }
    } else {
      NodeRewriteEvent event = getNodeEvent(parent, childProperty, true);
      if (event.getChangeKind() == RewriteEvent.UNCHANGED) {
        event.setNewValue(null);
      }
    }
  }

  private boolean isNodeInEvent(RewriteEvent event, Object value, int kind) {
    if ((kind & NEW) != 0 && event.getNewValue() == value) {
      return true;
    }
    if ((kind & ORIGINAL) != 0 && event.getOriginalValue() == value) {
      return true;
    }
    return false;
  }

  private void prepareNodeRangeCopies(TargetSourceRangeComputer sourceRangeComputer) {
    for (Iterator<Map.Entry<PropertyLocation, List<NodeRangeInfo>>> iter = nodeRangeInfos.entrySet().iterator(); iter.hasNext();) {
      Map.Entry<PropertyLocation, List<NodeRangeInfo>> entry = iter.next();
      List<NodeRangeInfo> rangeInfos = entry.getValue(); // list of
                                                         // CopySourceRange
      Collections.sort(rangeInfos); // sort by start index, length, move or copy

      PropertyLocation loc = entry.getKey();
      RewriteEvent[] children = getListEvent(loc.getParent(), loc.getProperty(), true).getChildren();

      RewriteEvent[] newChildren = processListWithRanges(rangeInfos, children, sourceRangeComputer);
      addEvent(loc.getParent(), loc.getProperty(), new ListRewriteEvent(newChildren)); // replace the current edits
    }
  }

  /**
   * Make sure all moved nodes are marked as removed or replaced.
   */
  private void prepareSingleNodeCopies() {
    for (int i = 0; i < nodeCopySources.size(); i++) {
      CopySourceInfo curr = nodeCopySources.get(i);
      if (curr.isMove && curr.location != null) {
        doMarkMovedAsRemoved(curr, curr.location.getParent(), curr.location.getProperty());
      }
    }

  }

  private RewriteEvent[] processListWithRanges(List<NodeRangeInfo> rangeInfos,
      RewriteEvent[] childEvents, TargetSourceRangeComputer sourceRangeComputer) {
    List<RewriteEvent> newChildEvents = new ArrayList<RewriteEvent>(childEvents.length);
    NodeRangeInfo topInfo = null;
    Stack<List<RewriteEvent>> newChildrenStack = new Stack<List<RewriteEvent>>();
    Stack<NodeRangeInfo> topInfoStack = new Stack<NodeRangeInfo>();

    Iterator<NodeRangeInfo> rangeInfoIterator = rangeInfos.iterator();
    NodeRangeInfo nextInfo = rangeInfoIterator.next();

    for (int k = 0; k < childEvents.length; k++) {
      RewriteEvent event = childEvents[k];
      DartNode node = (DartNode) event.getOriginalValue();
      // check for ranges and add a placeholder for them
      while (nextInfo != null && node == nextInfo.getStartNode()) { // is this
                                                                    // child the
                                                                    // beginning
                                                                    // of a
                                                                    // range?
        nextInfo.updatePlaceholderSourceRanges(sourceRangeComputer);

        DartBlock internalPlaceholder = nextInfo.getInternalPlaceholder();
        RewriteEvent newEvent;
        if (nextInfo.isMove()) {
          newEvent = new NodeRewriteEvent(internalPlaceholder, nextInfo.replacingNode); // remove or replace
        } else {
          newEvent = new NodeRewriteEvent(internalPlaceholder, internalPlaceholder); // unchanged
        }
        newChildEvents.add(newEvent);
        if (nextInfo.editGroup != null) {
          setEventEditGroup(newEvent, nextInfo.editGroup);
        }

        newChildrenStack.push(newChildEvents);
        topInfoStack.push(topInfo);

        newChildEvents = new ArrayList<RewriteEvent>(childEvents.length);
        topInfo = nextInfo;

        nextInfo = rangeInfoIterator.hasNext() ? (NodeRangeInfo) rangeInfoIterator.next() : null;
      }

      newChildEvents.add(event);

      while (topInfo != null && node == topInfo.getEndNode()) {
        DartCore.notYetImplemented();
        // RewriteEvent[] placeholderChildEvents = newChildEvents.toArray(new
        // RewriteEvent[newChildEvents.size()]);
        // DartBlock internalPlaceholder = topInfo.getInternalPlaceholder();
        // addEvent(internalPlaceholder, DartBlock.STATEMENTS_PROPERTY, new
        // ListRewriteEvent(placeholderChildEvents));

        newChildEvents = newChildrenStack.pop();
        topInfo = topInfoStack.pop();
      }
    }
    return newChildEvents.toArray(new RewriteEvent[newChildEvents.size()]);
  }

  private void removeMoveRangePlaceholders() {
    for (Iterator<Map.Entry<PropertyLocation, List<NodeRangeInfo>>> iter = nodeRangeInfos.entrySet().iterator(); iter.hasNext();) {
      Map.Entry<PropertyLocation, List<NodeRangeInfo>> entry = iter.next();
      Set<DartBlock> placeholders = new HashSet<DartBlock>(); // collect all
                                                              // placeholders
      List<NodeRangeInfo> rangeInfos = entry.getValue(); // list of
                                                         // CopySourceRange
      for (int i = 0; i < rangeInfos.size(); i++) {
        placeholders.add(rangeInfos.get(i).getInternalPlaceholder());
      }

      PropertyLocation loc = entry.getKey();

      RewriteEvent[] children = getListEvent(loc.getParent(), loc.getProperty(), true).getChildren();
      List<RewriteEvent> revertedChildren = new ArrayList<RewriteEvent>();
      revertListWithRanges(children, placeholders, revertedChildren);
      RewriteEvent[] revertedChildrenArr = revertedChildren.toArray(new RewriteEvent[revertedChildren.size()]);
      addEvent(loc.getParent(), loc.getProperty(), new ListRewriteEvent(revertedChildrenArr)); // replace the current edits
    }
  }

  private void revertListWithRanges(RewriteEvent[] childEvents, Set<DartBlock> placeholders,
      List<RewriteEvent> revertedChildren) {
    for (int i = 0; i < childEvents.length; i++) {
      RewriteEvent event = childEvents[i];
      DartNode node = (DartNode) event.getOriginalValue();
      if (placeholders.contains(node)) {
        DartCore.notYetImplemented();
        // RewriteEvent[] placeholderChildren= getListEvent(node,
        // DartBlock.STATEMENTS_PROPERTY, false).getChildren();
        // revertListWithRanges(placeholderChildren, placeholders,
        // revertedChildren);
      } else {
        revertedChildren.add(event);
      }
    }
  }

  private void validateHasChildProperty(DartNode parent, StructuralPropertyDescriptor property) {
    DartCore.notYetImplemented();
    // if (!parent.structuralPropertiesForType().contains(property)) {
    //      String message= Signature.getSimpleName(parent.getClass().getName()) + " has no property " + property.getId(); //$NON-NLS-1$
    // throw new IllegalArgumentException(message);
    // }
  }

  private void validateIsListProperty(StructuralPropertyDescriptor property) {
    if (!property.isChildListProperty()) {
      String message = property.getId() + " is not a list property"; //$NON-NLS-1$
      throw new IllegalArgumentException(message);
    }
  }

  private void validateIsNodeProperty(StructuralPropertyDescriptor property) {
    if (property.isChildListProperty()) {
      String message = property.getId() + " is not a node property"; //$NON-NLS-1$
      throw new IllegalArgumentException(message);
    }
  }
}
