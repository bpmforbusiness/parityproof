import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Java 21 modernized twin of cobol/TXN-POST.cbl.
 * Reads TXNS.DAT and writes OUT-TXN-JAVA.DAT in the same fixed-width layout as the COBOL
 * reference. Copybook: cobol/txn-post.cpy.
 *
 * Packed decimal (COMP-3) working-storage fields map to BigDecimal (never double):
 *   WS-ENDBAL   PIC S9(9)V99 COMP-3  ->  BigDecimal, scale 2, 9 integer digits
 *   WS-BATCHTOT PIC S9(9)V99 COMP-3  ->  BigDecimal, scale 2, 9 integer digits
 *
 * NOTE — latent truncation hazard, preserved deliberately for parity:
 * a COMPUTE/ADD into a S9(9)V99 field with no ON SIZE ERROR silently drops the high-order
 * digit(s) and keeps the sign (observed under GnuCOBOL: 999999999.99 + 0.01 stores 0.00).
 * fit() reproduces this instead of throwing.
 */
public final class TxnPost {

    private static final int INT_DIGITS = 9;
    private static final int SCALE = 2;

    public static void main(String[] args) throws Exception {
        List<String> lines;
        try (BufferedReader r = new BufferedReader(new FileReader("TXNS.DAT"))) {
            lines = r.lines().filter(l -> !l.isBlank()).toList();
        }
        BigDecimal batchTot = BigDecimal.ZERO.setScale(SCALE);                // WS-BATCHTOT VALUE 0
        try (BufferedWriter w = new BufferedWriter(new FileWriter("OUT-TXN-JAVA.DAT"))) {
            for (String line : lines) {
                String acct = line.substring(0, 9);                           // T-ACCT PIC 9(9)
                BigDecimal openBal = signed(line.substring(9, 21));           // T-OPENBAL PIC S9(9)V99 SIGN LEADING SEPARATE
                BigDecimal amt = signed(line.substring(21, 31));              // T-AMT PIC S9(7)V99 SIGN LEADING SEPARATE
                BigDecimal endBal = fit(openBal.add(amt));                    // COMPUTE WS-ENDBAL = T-OPENBAL + T-AMT
                batchTot = fit(batchTot.add(amt));                            // ADD T-AMT TO WS-BATCHTOT
                char odFlag = endBal.signum() < 0 ? 'Y' : 'N';                // IF WS-ENDBAL < 0
                w.write(acct);                                                // MOVE T-ACCT TO O-ACCT
                w.write(format(amt, 7));                                      // MOVE T-AMT TO O-AMT
                w.write(format(endBal, INT_DIGITS));                          // MOVE WS-ENDBAL TO O-ENDBAL
                w.write(odFlag);                                              // O-ODFLAG
                w.write(format(batchTot, INT_DIGITS));                        // MOVE WS-BATCHTOT TO O-BATCHTOT
                w.newLine();
            }
        }
        System.out.println("wrote OUT-TXN-JAVA.DAT (" + lines.size() + " records)");
    }

    /** Sign char (+/-) followed by implied-decimal digits -> BigDecimal. */
    private static BigDecimal signed(String field) {
        BigDecimal mag = new BigDecimal(new BigInteger(field.substring(1)), SCALE);
        return field.charAt(0) == '-' ? mag.negate() : mag;
    }

    /**
     * Store into S9(9)V99: keep the low-order 11 digits and the sign (COBOL high-order truncation).
     * Reproduces the silent overflow of COMPUTE WS-ENDBAL (line 40) and ADD T-AMT TO WS-BATCHTOT
     * (line 41) — both targets are S9(9)V99 with no ON SIZE ERROR; GnuCOBOL drops the high-order
     * digit(s) and keeps the sign (observed: +999999999.99 + 0.01 → +000000000.00;
     * -999999999.99 - 5.00 → -000000004.99).  Applies to WS-ENDBAL only in current test fixtures
     * (WS-BATCHTOT overflow is not exercised; see catalog/TXN-POST.md R003, R004).
     */
    private static BigDecimal fit(BigDecimal v) {
        BigInteger unscaled = v.unscaledValue().abs()
            .mod(BigInteger.TEN.pow(INT_DIGITS + SCALE));
        BigDecimal r = new BigDecimal(unscaled, SCALE);
        return v.signum() < 0 ? r.negate() : r;
    }

    /** SIGN LEADING SEPARATE rendering: '+'/'-' then zero-padded digits, no decimal point. A zero is always '+'. */
    private static String format(BigDecimal v, int intDigits) {
        String digits = v.abs().unscaledValue().toString();   // already within the PIC: fit() truncated it
        int width = intDigits + SCALE;
        return (v.signum() < 0 ? "-" : "+") + "0".repeat(width - digits.length()) + digits;
    }
}
