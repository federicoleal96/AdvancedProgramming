package uk.ncl.CSC8016.jackbergus.coursework.project4;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Solution extends BankFacade {

    // +10%: I cannot open a transaction if the user does not appear in the initialization map.
    // +10%: I can always open a transaction if the user, on the other hand, appears on the initialization map.
    private HashMap<String, Double> hashMap;
    private AtomicBigInteger abi;
    private ReentrantLock lock;

    public Solution(HashMap<String, Double> userIdToTotalInitialAmount) {
        super(userIdToTotalInitialAmount);
        hashMap = userIdToTotalInitialAmount;
        abi = new AtomicBigInteger(BigInteger.ZERO);
        lock = new ReentrantLock();
    }

    @Override
    public Optional<TransactionCommands> openTransaction(String userId) {
        // +10%: I cannot open a transaction if the user does not appear in the initialization map.
        if (!hashMap.containsKey(userId)) {
            return Optional.empty();
        }

        // +10%: I can always open a transaction if the user, on the other hand, appears on the initialization map.
        return Optional.of(new TransactionCommands() {
            boolean isProcessDone;
            boolean isProcessAborted;
            boolean isProcessCommitted;
            double totalLocalOperations;
            BigInteger currentTransactionId;
            double initialAmount;
            List<Operation> operations = new ArrayList<>();

            {
                totalLocalOperations = 0;
                isProcessDone = isProcessAborted = isProcessCommitted = false;
                currentTransactionId = abi.incrementAndGet();
                initialAmount = hashMap.get(userId);
            }

            @Override
            public BigInteger getTransactionId() {
                return currentTransactionId;
            }

            @Override
            public double getTentativeTotalAmount() {
                // ■ When no further operations are performed (pay/withdraw), the returned totalAmount
                // reflects the amount on the Bank.
                return initialAmount + totalLocalOperations;
            }

            @Override
            public boolean withdrawMoney(double amount) {
                // +15%: No overdraft is allowed.
                // ■ I can always withdraw 0.0 money from my account.
                // ■ I can never withdraw an amount of money which is greater than the amount at my disposal.
                if (isProcessAborted || isProcessCommitted) {
                    return false;
                }
                if (amount < 0 || amount > getTentativeTotalAmount()) {
                    return false;
                }
                totalLocalOperations -= amount;
                operations.add(Operation.Withdraw(amount, operations.size()));
                return true;
            }

            @Override
            public boolean payMoneyToAccount(double amount) {
                // +15%: No overdraft is allowed.
                // ■ I can always withdraw 0.0 money from my account.
                // ■ I can never withdraw an amount of money which is greater than the amount at my disposal.
                if (isProcessAborted || isProcessCommitted) {
                    return false;
                }
                if (amount < 0) {
                    return false;
                }
                totalLocalOperations += amount;
                operations.add(Operation.Pay(amount, operations.size()));
                return true;
            }

            @Override
            public void abort() {
                // +10%: Handing aborted transactions:
                // ■ Aborted transactions apply no change to the bank account.
                // ■ In particular, aborted threads should leave the bank in a consistent state.
                if (isProcessCommitted) {
                    throw new RuntimeException("Transaction already committed");
                }
                isProcessAborted = true;
            }

            @Override
            public CommitResult commit() {
                // +15%: After committing a transaction, the results provides the total changes into the account.
                // ■ The returned commit information should contain relevant Operation of OperationType
                // on the bank account (pay/withdrawal) before the commit.
                // ■ After paying money into the account, the final total amount is the sum of the previous
                // amount of money and the amount being paid, as retrieved through the CommitResult.
                if (isProcessCommitted || isProcessAborted) {
                    throw new RuntimeException("Transaction already committed or aborted");
                }
                lock.lock();
                try {
                    double totalAmount = initialAmount + totalLocalOperations;
                    hashMap.put(userId, totalAmount);
                    List<Operation> successfulOperations = new ArrayList<>(operations);
                    List<Operation> unsuccessfulOperations = new ArrayList<>();
                    isProcessCommitted = true;
                    return new CommitResult(successfulOperations, unsuccessfulOperations, totalAmount);
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public void close() {
                // ■ I cannot re-commit or abort a closed transaction (I shall return null rather than a CommitResult).
                commit();
            }
        });
    }
}