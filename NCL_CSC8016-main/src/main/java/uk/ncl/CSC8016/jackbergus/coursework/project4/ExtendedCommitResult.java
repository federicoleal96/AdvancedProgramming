public class ExtendedCommitResult extends CommitResult {
    public final List<Operation> successfulOperations;
    public final List<Operation> unsuccessfulOperations;
    public final List<Operation> ignoredOperations;

    public ExtendedCommitResult(List<Operation> successfulOperations, List<Operation> unsuccessfulOperations, List<Operation> ignoredOperations, double totalAmount) {
        super(successfulOperations, ignoredOperations.isEmpty()? new ArrayList<>() : java.util.Collections.singletonList(ignoredOperations.get(0)), totalAmount);
        this.successfulOperations = successfulOperations;
        this.unsuccessfulOperations = unsuccessfulOperations;
        this.ignoredOperations = ignoredOperations;
    }
}