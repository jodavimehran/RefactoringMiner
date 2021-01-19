package gr.uom.java.xmi.decomposition;

import gr.uom.java.xmi.LocationInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class VariableScope {
    private final String filePath;
    private final int startOffset;
    private final int endOffset;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final List<AbstractCodeFragment> statementList = new ArrayList<>();
    private String parentSignature = "";

    public VariableScope(CompilationUnit cu, String filePath, int startOffset, int endOffset) {
        //ASTNode parent = node.getParent();
        this.filePath = filePath;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        //this.startOffset = node.getStartPosition();
        //this.endOffset = parent.getStartPosition() + parent.getLength();

        //lines are 1-based
        this.startLine = cu.getLineNumber(startOffset);
        this.endLine = cu.getLineNumber(endOffset);

        if (cu.getColumnNumber(startOffset) > 0) {
            //convert to 1-based
            this.startColumn = cu.getColumnNumber(startOffset) + 1;
        } else {
            //columns are 0-based
            this.startColumn = cu.getColumnNumber(startOffset);
        }

        if (cu.getColumnNumber(endOffset) > 0) {
            //convert to 1-based
            this.endColumn = cu.getColumnNumber(endOffset) + 1;
        } else {
            this.endColumn = cu.getColumnNumber(endOffset);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, startOffset, endOffset, startLine, startColumn, endLine, endColumn);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        VariableScope other = (VariableScope) obj;
        return startOffset == other.startOffset &&
                endOffset == other.endOffset &&
                startLine == other.startLine &&
                startColumn == other.startColumn &&
                endLine == other.endLine &&
                endColumn == other.endColumn &&
                Objects.equals(filePath, other.filePath);
    }

    @Override
    public String toString() {
        return String.format("%d:%d-%d:%d", startLine, startColumn, endLine, endColumn);
    }

    public boolean subsumes(LocationInfo other) {
        return this.filePath.equals(other.getFilePath()) &&
                this.startOffset <= other.getStartOffset() &&
                this.endOffset >= other.getEndOffset();
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void addStatement(AbstractCodeFragment statement) {
        statementList.add(statement);
    }

    public List<AbstractCodeFragment> getStatementList() {
        return statementList;
    }

    public String getParentSignature() {
        return parentSignature;
    }

    public void setParentSignature(String parentSignature) {
        this.parentSignature = parentSignature;
    }
}
