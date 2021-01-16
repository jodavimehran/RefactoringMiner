package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.replacement.VariableDeclarationReplacement;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;
import org.refactoringminer.api.Refactoring;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class VariableChangeAnalysis {
    private final HashMap<AbstractCodeFragment, AbstractCodeFragment> statementMapping = new HashMap<>();
    private final Set<VariableDeclaration> removedVariables = new LinkedHashSet<>();
    private final Set<VariableDeclaration> addedVariables = new LinkedHashSet<>();
    private final Set<VariableDeclarationReplacement> changedVariable = new LinkedHashSet<>();
    private final Set<VariableDeclaration> leftSideMappedVariables = new HashSet<>();
    private final Set<VariableDeclaration> rightSideMappedVariables = new HashSet<>();
    private final Set<Refactoring> refactorings;
    private final UMLOperation operation1;
    private final UMLOperation operation2;

    public VariableChangeAnalysis(UMLOperationBodyMapper mapper, Set<Refactoring> refactorings) {
        this.refactorings = refactorings;
        operation1 = mapper.getOperation1();
        operation2 = mapper.getOperation2();

        findVariableScope(operation1.getBody());
        findVariableScope(operation2.getBody());

        for (AbstractCodeMapping abstractCodeMapping : mapper.getMappings()) {
            statementMapping.put(abstractCodeMapping.getFragment1(), abstractCodeMapping.getFragment2());
        }

        for (AbstractCodeMapping mapping : mapper.getMappings()) {
            mapVariables(mapping.getFragment1(), mapping.getFragment2());
        }
        mapVariablesByRefactoring(leftSideMappedVariables, rightSideMappedVariables);

        removedVariables.addAll(operation1.getAllVariableDeclarations());
        removedVariables.removeAll(leftSideMappedVariables);

        addedVariables.addAll(operation2.getAllVariableDeclarations());
        addedVariables.removeAll(rightSideMappedVariables);

        mapVariables(removedVariables, addedVariables, this::sameScope);

        removedVariables.removeAll(leftSideMappedVariables);
        addedVariables.removeAll(rightSideMappedVariables);
    }

    public static double getSimilarity(int s1, int s2, int intersection) {
        if (s1 == 0 && s2 == 0)
            return 1;
        double b = s1 - intersection;
        double c = s2 - intersection;
        return (double) intersection / ((double) intersection + b + c);
    }

    public Set<VariableDeclaration> getRemovedVariables() {
        return removedVariables;
    }

    public Set<VariableDeclaration> getAddedVariables() {
        return addedVariables;
    }

    public Set<VariableDeclarationReplacement> getChangedVariable() {
        return changedVariable;
    }

    private void findVariableScope(OperationBody body) {
        if (body == null)
            return;
        Set<VariableScope> variableScopeSet = new HashSet<>();
        findVariableScope(body.getCompositeStatement(), variableScopeSet);
    }

    private void findVariableScope(CompositeStatementObject compositeStatementObject, Set<VariableScope> variableScopeSet) {
        variableScopeSet.addAll(compositeStatementObject.getVariableDeclarations().stream().map(VariableDeclaration::getScope).collect(Collectors.toList()));
        for (AbstractStatement statement : compositeStatementObject.getStatements()) {
            for (VariableScope variableScope : variableScopeSet) {
                variableScope.addStatement(statement);
            }

            if (statement instanceof CompositeStatementObject) {
                CompositeStatementObject compositeStatement = (CompositeStatementObject) statement;
                findVariableScope(compositeStatement, variableScopeSet);
            } else {
                variableScopeSet.addAll(statement.getVariableDeclarations().stream().map(VariableDeclaration::getScope).collect(Collectors.toList()));
            }
        }
        variableScopeSet.removeAll(compositeStatementObject.getAllVariableDeclarations().stream().map(VariableDeclaration::getScope).collect(Collectors.toList()));
    }

    private void mapVariables(AbstractCodeFragment leftSide, AbstractCodeFragment rightSide) {
        Set<VariableDeclaration> leftSideExcludeVariables = leftSide.getAnonymousClassDeclarations().stream().flatMap(anonymousClassDeclarationObject -> anonymousClassDeclarationObject.getVariableDeclarations().stream()).collect(Collectors.toSet());
        Set<VariableDeclaration> rightSideExcludeVariables = rightSide.getAnonymousClassDeclarations().stream().flatMap(anonymousClassDeclarationObject -> anonymousClassDeclarationObject.getVariableDeclarations().stream()).collect(Collectors.toSet());
//
        mapVariables(leftSide.getVariableDeclarations().stream().filter(variableDeclaration -> !leftSideExcludeVariables.contains(variableDeclaration)).collect(Collectors.toList()), rightSide.getVariableDeclarations().stream().filter(variableDeclaration -> !rightSideExcludeVariables.contains(variableDeclaration)).collect(Collectors.toList()), (a, b) -> true);
        mapVariables(leftSideExcludeVariables, rightSideExcludeVariables, (a, b) -> false);

    }

    private void mapVariables(Collection<VariableDeclaration> leftSideVariables, Collection<VariableDeclaration> rightSideVariables, BiFunction<VariableDeclaration, VariableDeclaration, Boolean> function) {
        if (leftSideVariables.size() == 1 && rightSideVariables.size() == 1) {
            VariableDeclaration leftSideVar = leftSideVariables.iterator().next();
            VariableDeclaration rightSideVar = leftSideVariables.iterator().next();

            mapVariables(leftSideVar, rightSideVar, function);
            return;
        }
        for (VariableDeclaration leftSideVar : leftSideVariables) {
            for (VariableDeclaration rightSideVar : rightSideVariables) {
                if (rightSideMappedVariables.contains(rightSideVar)) {
                    continue;
                }
                if (leftSideVar.getVariableName().equals(rightSideVar.getVariableName()) && leftSideVar.getType().equals(rightSideVar.getType())) {
                    mapVariables(leftSideVar, rightSideVar, function);
                    break;
                }
            }
        }
    }

    private void mapVariables(VariableDeclaration leftSideVar, VariableDeclaration rightSideVar, BiFunction<VariableDeclaration, VariableDeclaration, Boolean> function) {
        if (!leftSideVar.getScope().getParentSignature().equals(rightSideVar.getScope().getParentSignature()) && function.apply(leftSideVar, rightSideVar)) {
            changedVariable.add(new VariableDeclarationReplacement(leftSideVar, rightSideVar, operation1, operation2));
        }
        leftSideMappedVariables.add(leftSideVar);
        rightSideMappedVariables.add(rightSideVar);
    }

    private void mapVariablesByRefactoring(Set<VariableDeclaration> leftSideMappedVariables, Set<VariableDeclaration> rightSideMappedVariables) {
        for (Refactoring ref : refactorings) {
            switch (ref.getRefactoringType()) {
                case RENAME_VARIABLE: {
                    RenameVariableRefactoring renameVariableRefactoring = (RenameVariableRefactoring) ref;
                    leftSideMappedVariables.add(renameVariableRefactoring.getOriginalVariable());
                    rightSideMappedVariables.add(renameVariableRefactoring.getRenamedVariable());
                    break;
                }
                case CHANGE_VARIABLE_TYPE: {
                    ChangeVariableTypeRefactoring changeVariableTypeRefactoring = (ChangeVariableTypeRefactoring) ref;
                    leftSideMappedVariables.add(changeVariableTypeRefactoring.getOriginalVariable());
                    rightSideMappedVariables.add(changeVariableTypeRefactoring.getChangedTypeVariable());
                    break;
                }
                case MERGE_VARIABLE: {
                    MergeVariableRefactoring mergeVariableRefactoring = (MergeVariableRefactoring) ref;
                    leftSideMappedVariables.addAll(mergeVariableRefactoring.getMergedVariables());
                    rightSideMappedVariables.add(mergeVariableRefactoring.getNewVariable());
                    break;
                }
                case SPLIT_VARIABLE: {
                    SplitVariableRefactoring splitVariableRefactoring = (SplitVariableRefactoring) ref;
                    leftSideMappedVariables.add(splitVariableRefactoring.getOldVariable());
                    rightSideMappedVariables.addAll(splitVariableRefactoring.getSplitVariables());
                    break;
                }
            }
        }
    }

    private boolean sameScope(VariableDeclaration leftSideVar, VariableDeclaration rightSideVar) {
        List<AbstractCodeFragment> leftSideUsedVariable = leftSideVar.getScope().getStatementList().stream().filter(abstractCodeFragment -> abstractCodeFragment.getVariables().stream().anyMatch(leftSideVar.getVariableName()::equals)).collect(Collectors.toList());
        List<AbstractCodeFragment> rightSideUsedVariable = rightSideVar.getScope().getStatementList().stream().filter(abstractCodeFragment -> abstractCodeFragment.getVariables().stream().anyMatch(rightSideVar.getVariableName()::equals)).collect(Collectors.toList());

        double score = computeSimilarityScore(leftSideUsedVariable, rightSideUsedVariable);
        return score > 0;
    }

    public double computeSimilarityScore(List<AbstractCodeFragment> leftSide, List<AbstractCodeFragment> rightSide) {
        int s1 = leftSide.size();
        int s2 = rightSide.size();
        int intersection = s1 - minus(leftSide, rightSide).size();
        return getSimilarity(s1, s2, intersection);
    }

    private List<AbstractCodeFragment> minus(List<AbstractCodeFragment> left, List<AbstractCodeFragment> right) {
        List<AbstractCodeFragment> leftCopy = left.stream().map(abstractCodeFragment -> statementMapping.getOrDefault(abstractCodeFragment, abstractCodeFragment)).collect(Collectors.toList());

        right.forEach(leftCopy::remove);
        return leftCopy;
    }
}
