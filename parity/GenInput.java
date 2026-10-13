import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes ACCOUNTS.DAT, the fixed-width input for ACCOUNT-INT (both the COBOL and
 * Java sides). Covers normal + edge cases: a half-cent rounding boundary, zero,
 * a tiny balance, and a near-max field size.
 *
 * Each line: ACCT-NO (9) + BAL (11, implied 2 decimals), no label.
 */
public final class GenInput {

    record Test(String label, String acct, String balanceImplied /* 11 chars */) {}

    public static void main(String[] args) throws Exception {
        List<Test> tests = List.of(
            // label, acct(9), balance as 11-digit implied-decimal string
            new Test("half-cent-boundary", "000000001", "00001234500"), // 12345.00 -> *0.035 = 432.075 -> ROUNDED 432.08
            new Test("zero-balance",       "000000002", "00000000000"), // 0.00
            new Test("tiny-balance",       "000000003", "00000000005"), // 0.05 -> 0.00175 -> 0.00
            new Test("sub-cent-interest",  "000000004", "00000000003"), // 0.03 -> 0.00105 -> 0.00
                        new Test("near-max-field",     "000000005", "90000000000"),  // 900000000.00 -> +31500000.00 = 931500000.00 (fits 9(9)V99)
                        // Latent truncation hazard in the ORIGINAL program, preserved deliberately for parity:
                        // BAL = 999999999.99 is the max 9(9)V99; NEW-BAL = +35000000.00 = 1,034,999,999.99
                        // overflows the field, so COBOL silently drops the high-order digit (writes 034,999,999.99).
                        // The Java twin reproduces this high-order truncation instead of throwing.
                        new Test("overflow-truncation", "000000009", "99999999999") // 999999999.99 -> NEW-BAL overflows -> truncated
                    );
        try (BufferedWriter w = new BufferedWriter(new FileWriter("ACCOUNTS.DAT"))) {
            for (Test t : tests) {
                w.write(t.acct());
                w.write(t.balanceImplied());
                w.newLine();
            }
        }
        System.out.println("generated " + tests.size() + " input records -> ACCOUNTS.DAT");
    }
}