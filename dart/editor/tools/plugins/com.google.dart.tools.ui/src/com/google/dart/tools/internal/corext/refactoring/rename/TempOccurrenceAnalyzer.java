package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.tools.internal.corext.dom.ASTNodes;

import org.eclipse.core.runtime.Assert;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TempOccurrenceAnalyzer extends ASTVisitor<Void> {
  private Set<DartIdentifier> fReferenceNodes;
//  private Set<DartIdentifier> fJavadocNodes;

  private DartVariable fTempDeclaration;
  private Element fTempBinding;

//  private boolean fAnalyzeJavadoc;

//  private boolean fIsInJavadoc;

  public TempOccurrenceAnalyzer(DartVariable tempDeclaration/* , boolean analyzeJavadoc */) {
    Assert.isNotNull(tempDeclaration);
    fReferenceNodes = new HashSet<DartIdentifier>();
//    fJavadocNodes = new HashSet<DartIdentifier>();
//    fAnalyzeJavadoc = analyzeJavadoc;
    fTempDeclaration = tempDeclaration;
    fTempBinding = tempDeclaration.getElement();
//    fIsInJavadoc = false;
  }

//  @Override
//  public void endVisit(Javadoc node) {
//    fIsInJavadoc = false;
//  }

//  public DartIdentifier[] getJavadocNodes() {
//    return fJavadocNodes.toArray(new DartIdentifier[fJavadocNodes.size()]);
//  }

  public int getNumberOfReferences() {
    return fReferenceNodes.size();
  }

  public DartIdentifier[] getReferenceAndDeclarationNodes() {
    DartIdentifier[] nodes = fReferenceNodes.toArray(new DartIdentifier[fReferenceNodes.size() + 1]);
    nodes[fReferenceNodes.size()] = fTempDeclaration.getName();
    return nodes;
  }

  public int[] getReferenceAndJavadocOffsets() {
    int[] offsets = new int[fReferenceNodes.size()];
    addOffsets(offsets, 0, fReferenceNodes);
//    int[] offsets = new int[fReferenceNodes.size() + fJavadocNodes.size()];
//    addOffsets(offsets, 0, fReferenceNodes);
//    addOffsets(offsets, fReferenceNodes.size(), fJavadocNodes);
    return offsets;
  }

  public DartIdentifier[] getReferenceNodes() {
    return fReferenceNodes.toArray(new DartIdentifier[fReferenceNodes.size()]);
  }

  public int[] getReferenceOffsets() {
    int[] offsets = new int[fReferenceNodes.size()];
    addOffsets(offsets, 0, fReferenceNodes);
    return offsets;
  }

  public void perform() {
    DartNode cuNode = ASTNodes.getParent(fTempDeclaration, DartUnit.class);
    cuNode.accept(this);
  }

  //------- visit ------ (don't call)

  @Override
  public Void visitIdentifier(DartIdentifier node) {
    if (node.getParent() instanceof DartVariable) {
      if (((DartVariable) node.getParent()).getName() == node) {
        return super.visitIdentifier(node);
//        return true; //don't include declaration
      }
    }

    if (fTempBinding != null && fTempBinding == node.getElement()) {
      fReferenceNodes.add(node);
//      if (fIsInJavadoc) {
//        fJavadocNodes.add(node);
//      } else {
//        fReferenceNodes.add(node);
//      }
    }

    return super.visitIdentifier(node);
  }

//  @Override
//  public boolean visit(Javadoc node) {
//    if (fAnalyzeJavadoc) {
//      fIsInJavadoc = true;
//    }
//    return fAnalyzeJavadoc;
//  }

  private void addOffsets(int[] offsets, int start, Set<DartIdentifier> nodeSet) {
    int i = start;
    for (Iterator<DartIdentifier> iter = nodeSet.iterator(); iter.hasNext(); i++) {
      DartNode node = iter.next();
      offsets[i] = node.getSourceInfo().getOffset();
    }
  }
}
