import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

/**
 * Writes TXNS.DAT, the fixed-width input for TXN-POST (COBOL and Java sides).
 * Each line: T-ACCT (9) + T-OPENBAL (sign + 11 digits) + T-AMT (sign + 9 digits), implied 2 decimals.
 */
public final class GenTxnInput {

    record Test(String label, String acct, String openBal, String amt) {}

    public static void main(String[] args) throws Exception {
        List<Test> tests = List.of(
            new Test("credit",                 "000000001", "+00000100000", "+000025050"), // 1000.00 + 250.50 = 1250.50
            new Test("debit-into-overdraft",   "000000002", "+00000010000", "-000015025"), // 100.00 - 150.25 = -50.25 -> flag Y
            new Test("exact-zero-result",      "000000003", "+00000000500", "-000000500"), // 5.00 - 5.00 = 0.00, written '+', flag N
            new Test("negative-open-credit",   "000000004", "-00000002000", "+000000500"), // -20.00 + 5.00 = -15.00 -> flag Y
            new Test("zero-amount",            "000000005", "+00000005000", "+000000000"), // 50.00 + 0.00
            // Latent truncation hazard, preserved deliberately for parity (same class as ACCOUNT-INT):
            // the packed WS-ENDBAL is S9(9)V99, so a sum that needs a 10th integer digit loses it silently.
            new Test("overflow-positive",      "000000006", "+99999999999", "+000000001"), // 999999999.99 + 0.01 = 1,000,000,000.00 -> +0.00
            new Test("overflow-negative",      "000000007", "-99999999999", "-000000500"), // -999999999.99 - 5.00 -> -000000004.99, flag Y
            new Test("negative-zero-input",    "000000009", "-00000000000", "+000000000")  // -0.00 + 0.00 -> '+' (COBOL has no negative zero result)
        );
        try (BufferedWriter w = new BufferedWriter(new FileWriter("TXNS.DAT"))) {
            for (Test t : tests) {
                w.write(t.acct()); w.write(t.openBal()); w.write(t.amt());
                w.newLine();
            }
        }
        System.out.println("generated " + tests.size() + " input records -> TXNS.DAT");
    }
}
