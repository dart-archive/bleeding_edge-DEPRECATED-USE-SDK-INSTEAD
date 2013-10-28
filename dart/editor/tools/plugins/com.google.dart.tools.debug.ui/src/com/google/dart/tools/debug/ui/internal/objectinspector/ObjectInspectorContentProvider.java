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

package com.google.dart.tools.debug.ui.internal.objectinspector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IIndexedValue;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.views.variables.IndexedVariablePartition;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * The content inspector for the object inspector.
 */
@SuppressWarnings("restriction")
public class ObjectInspectorContentProvider implements ITreeContentProvider {
  private static final Object[] EMPTY = new Object[0];

  public ObjectInspectorContentProvider() {

  }

  @Override
  public void dispose() {

  }

  @Override
  public Object[] getChildren(Object element) {
    IVariable variable = (IVariable) element;

    try {
      IValue value = variable.getValue();

      if (value instanceof IIndexedValue) {
        IIndexedValue indexedValue = (IIndexedValue) value;
        int partitionSize = computeParitionSize(indexedValue);
        if (partitionSize > 1) {
          int offset = indexedValue.getInitialOffset();
          int length = indexedValue.getSize();
          int numPartitions = length / partitionSize;
          int remainder = length % partitionSize;
          if (remainder > 0) {
            numPartitions++;
          }
          IVariable[] partitions = new IVariable[numPartitions];
          for (int i = 0; i < (numPartitions - 1); i++) {
            partitions[i] = new IndexedVariablePartition(
                variable,
                indexedValue,
                offset,
                partitionSize);
            offset = offset + partitionSize;
          }
          if (remainder == 0) {
            remainder = partitionSize;
          }
          partitions[numPartitions - 1] = new IndexedVariablePartition(
              variable,
              indexedValue,
              offset,
              remainder);
          return partitions;
        }
      }

      return value.getVariables();
    } catch (DebugException ex) {
      // TODO(devoncarew): determine the best way to present errors in the object inspector view
      ex.printStackTrace();

      return EMPTY;
    }
  }

  @Override
  public Object[] getElements(Object inputElement) {
    Object[] elements = (Object[]) inputElement;

    return elements == null ? EMPTY : elements;
  }

  @Override
  public Object getParent(Object element) {
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

  }

  /**
   * Returns the partition size to use for the given indexed value. The partition size is computed
   * by determining the number of levels that an indexed collection must be nested in order to
   * partition the collection sub-collections of the preferred partition size.
   * 
   * @param value indexed value
   * @return size of partitions the value should be subdivided into
   */
  protected int computeParitionSize(IIndexedValue value) {
    int partitionSize = 1;
    try {
      int length = value.getSize();
      int partitionDepth = 0;
      int preferredSize = getArrayPartitionSize();
      int remainder = length % preferredSize;
      length = length / preferredSize;
      while (length > 0) {
        if (remainder == 0 && length == 1) {
          break;
        }
        partitionDepth++;
        remainder = length % preferredSize;
        length = length / preferredSize;
      }
      for (int i = 0; i < partitionDepth; i++) {
        partitionSize = partitionSize * preferredSize;
      }
    } catch (DebugException e) {
    }
    return partitionSize;
  }

  protected int getArrayPartitionSize() {
    return 100;
  }
}
