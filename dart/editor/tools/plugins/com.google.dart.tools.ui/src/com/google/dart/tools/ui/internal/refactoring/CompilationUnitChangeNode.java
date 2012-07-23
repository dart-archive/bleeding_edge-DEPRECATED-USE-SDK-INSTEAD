package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ltk.core.refactoring.MultiStateTextFileChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChange;
import org.eclipse.ltk.core.refactoring.TextEditBasedChangeGroup;
import org.eclipse.ltk.ui.refactoring.LanguageElementNode;
import org.eclipse.ltk.ui.refactoring.TextEditChangeNode;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompilationUnitChangeNode extends TextEditChangeNode {

  private static class DartLanguageNode extends LanguageElementNode {

    private DartElement element;
    private static DartElementImageProvider fgImageProvider = new DartElementImageProvider();

    public DartLanguageNode(ChildNode parent, DartElement element) {
      super(parent);
      this.element = element;
      Assert.isNotNull(element);
    }

    public DartLanguageNode(TextEditChangeNode parent, DartElement element) {
      super(parent);
      this.element = element;
      Assert.isNotNull(element);
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
      return fgImageProvider.getDartImageDescriptor(element, DartElementImageProvider.OVERLAY_ICONS
          | DartElementImageProvider.SMALL_ICONS);
    }

    @Override
    public String getText() {
      return DartElementLabels.getElementLabel(element, DartElementLabels.ALL_DEFAULT);
    }

    @Override
    public IRegion getTextRange() throws CoreException {
      SourceRange range = ((SourceReference) element).getSourceRange();
      return new Region(range.getOffset(), range.getLength());
    }
  }

  private static class OffsetComparator implements Comparator<TextEditBasedChangeGroup> {
    @Override
    public int compare(TextEditBasedChangeGroup c1, TextEditBasedChangeGroup c2) {
      int p1 = getOffset(c1);
      int p2 = getOffset(c2);
      if (p1 < p2) {
        return -1;
      }
      if (p1 > p2) {
        return 1;
      }
      // same offset
      return 0;
    }

    private int getOffset(TextEditBasedChangeGroup edit) {
      return edit.getRegion().getOffset();
    }
  }

  static final ChildNode[] EMPTY_CHILDREN = new ChildNode[0];

  public CompilationUnitChangeNode(TextEditBasedChange change) {
    super(change);
  }

  @Override
  protected ChildNode[] createChildNodes() {
    TextEditBasedChange change = getTextEditBasedChange();
    if (change instanceof MultiStateTextFileChange) {
      return new ChildNode[0]; // no edit preview & edit disabling possible in the MultiStateTextFileChange (edits must be applied in sequence)
    }

    CompilationUnit cunit = (CompilationUnit) change.getAdapter(CompilationUnit.class);
    if (cunit != null) {
      List<ChildNode> children = new ArrayList<ChildNode>(5);
      Map<DartElement, DartLanguageNode> map = new HashMap<DartElement, DartLanguageNode>(20);
      TextEditBasedChangeGroup[] changes = getSortedChangeGroups(change);
      for (int i = 0; i < changes.length; i++) {
        TextEditBasedChangeGroup tec = changes[i];
        try {
          DartElement element = getModifiedDartElement(tec, cunit);
          if (element.equals(cunit)) {
            children.add(createTextEditGroupNode(this, tec));
          } else {
            DartLanguageNode pjce = getChangeElement(map, element, children, this);
            pjce.addChild(createTextEditGroupNode(pjce, tec));
          }
        } catch (DartModelException e) {
          children.add(createTextEditGroupNode(this, tec));
        }
      }
      return children.toArray(new ChildNode[children.size()]);
    } else {
      return EMPTY_CHILDREN;
    }
  }

  private boolean coveredBy(TextEditBasedChangeGroup group, IRegion sourceRegion) {
    int sLength = sourceRegion.getLength();
    if (sLength == 0) {
      return false;
    }
    int sOffset = sourceRegion.getOffset();
    int sEnd = sOffset + sLength - 1;
    TextEdit[] edits = group.getTextEdits();
    for (int i = 0; i < edits.length; i++) {
      TextEdit edit = edits[i];
      if (edit.isDeleted()) {
        return false;
      }
      int rOffset = edit.getOffset();
      int rLength = edit.getLength();
      int rEnd = rOffset + rLength - 1;
      if (rLength == 0) {
        if (!(sOffset < rOffset && rOffset <= sEnd)) {
          return false;
        }
      } else {
        if (!(sOffset <= rOffset && rEnd <= sEnd)) {
          return false;
        }
      }
    }
    return true;
  }

  private DartLanguageNode getChangeElement(Map<DartElement, DartLanguageNode> map,
      DartElement element, List<ChildNode> children, TextEditChangeNode cunitChange) {
    DartLanguageNode result = map.get(element);
    if (result != null) {
      return result;
    }
    DartElement parent = element.getParent();
    if (parent instanceof CompilationUnit) {
      result = new DartLanguageNode(cunitChange, element);
      children.add(result);
      map.put(element, result);
    } else {
      DartLanguageNode parentChange = getChangeElement(map, parent, children, cunitChange);
      result = new DartLanguageNode(parentChange, element);
      parentChange.addChild(result);
      map.put(element, result);
    }
    return result;
  }

  private DartElement getModifiedDartElement(TextEditBasedChangeGroup edit, CompilationUnit cunit)
      throws DartModelException {
    IRegion range = edit.getRegion();
    if (range.getOffset() == 0 && range.getLength() == 0) {
      return cunit;
    }
    DartElement result = cunit.getElementAt(range.getOffset());
    if (result == null) {
      return cunit;
    }

    try {
      while (true) {
        SourceReference ref = (SourceReference) result;
        IRegion sRange = new Region(
            ref.getSourceRange().getOffset(),
            ref.getSourceRange().getLength());
        if (result.getElementType() == DartElement.COMPILATION_UNIT || result.getParent() == null
            || coveredBy(edit, sRange)) {
          break;
        }
        result = result.getParent();
      }
    } catch (DartModelException e) {
      // Do nothing, use old value.
    } catch (ClassCastException e) {
      // Do nothing, use old value.
    }
    return result;
  }

  private TextEditBasedChangeGroup[] getSortedChangeGroups(TextEditBasedChange change) {
    TextEditBasedChangeGroup[] edits = change.getChangeGroups();
    List<TextEditBasedChangeGroup> result = new ArrayList<TextEditBasedChangeGroup>(edits.length);
    for (int i = 0; i < edits.length; i++) {
      if (!edits[i].getTextEditGroup().isEmpty()) {
        result.add(edits[i]);
      }
    }
    Comparator<TextEditBasedChangeGroup> comparator = new OffsetComparator();
    Collections.sort(result, comparator);
    return result.toArray(new TextEditBasedChangeGroup[result.size()]);
  }
}
