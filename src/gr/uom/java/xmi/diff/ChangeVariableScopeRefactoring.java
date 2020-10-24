package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.*;

public class ChangeVariableScopeRefactoring implements Refactoring {
    private final static RefactoringType TYPE = RefactoringType.CHANGE_VARIABLE_SCOPE;
    private final VariableDeclaration originalVariable;
    private final VariableDeclaration changedScopeVariable;
    private final UMLOperation operationBefore;
    private final UMLOperation operationAfter;

    public ChangeVariableScopeRefactoring(VariableDeclaration originalVariable, VariableDeclaration changedScopeVariable,
                                          UMLOperation operationBefore, UMLOperation operationAfter) {
        this.originalVariable = originalVariable;
        this.changedScopeVariable = changedScopeVariable;
        this.operationBefore = operationBefore;
        this.operationAfter = operationAfter;
    }

    public RefactoringType getRefactoringType() {
        return TYPE;
    }

    public String getName() {
        return this.getRefactoringType().getDisplayName();
    }

    public VariableDeclaration getOriginalVariable() {
        return originalVariable;
    }

    public VariableDeclaration getChangedScopeVariable() {
        return changedScopeVariable;
    }

    public UMLOperation getOperationBefore() {
        return operationBefore;
    }

    public UMLOperation getOperationAfter() {
        return operationAfter;
    }

    public String toString() {
        boolean qualified = originalVariable.getType().equals(changedScopeVariable.getType()) && !originalVariable.getType().equalsQualified(changedScopeVariable.getType());
        return getRefactoringType().getDescription(
                qualified ? originalVariable.toQualifiedString() : originalVariable.toString(),
                originalVariable.getScope(),
                changedScopeVariable.getScope(),
                qualified ? operationAfter.toQualifiedString() : operationAfter.toString(),
                operationAfter.getClassName()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeVariableScopeRefactoring that = (ChangeVariableScopeRefactoring) o;
        return Objects.equals(originalVariable, that.originalVariable) &&
                Objects.equals(changedScopeVariable, that.changedScopeVariable) &&
                Objects.equals(operationBefore, that.operationBefore) &&
                Objects.equals(operationAfter, that.operationAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalVariable, changedScopeVariable, operationBefore, operationAfter);
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationBefore().getLocationInfo().getFilePath(), getOperationBefore().getClassName()));
        return pairs;
    }

    public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
        Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<>();
        pairs.add(new ImmutablePair<>(getOperationAfter().getLocationInfo().getFilePath(), getOperationAfter().getClassName()));
        return pairs;
    }

    @Override
    public List<CodeRange> leftSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(originalVariable.codeRange()
                .setDescription("original variable declaration")
                .setCodeElement(originalVariable.toString()));
        return ranges;
    }

    @Override
    public List<CodeRange> rightSide() {
        List<CodeRange> ranges = new ArrayList<>();
        ranges.add(changedScopeVariable.codeRange()
                .setDescription("changed-scope variable declaration")
                .setCodeElement(changedScopeVariable.toString()));
        return ranges;
    }
}
