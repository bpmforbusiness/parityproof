import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

/**
 * Java 21 modernized twin of cobol/ACCOUNT-INT.cbl.
 * Reads the same fixed-width ACCOUNTS.DAT and writes OUT-JAVA.DAT with byte-identical
 * field layout, so the parity runner can diff against the COBOL reference output.
 * Copybook: cobol/copybook.cpy — ACCT-NO PIC 9(9), BAL PIC 9(9)V99.
 */
public final class AccountInterest {

    private static final BigDecimal RATE = new BigDecimal("0.035");
    private static final int INT_DIGITS = 9;
    private static final int SCALE = 2;

    public static void main(String[] args) throws Exception {
        List<String> lines;
        try (BufferedReader r = new BufferedReader(new FileReader("ACCOUNTS.DAT"))) {
            lines = r.lines().filter(l -> !l.isBlank()).toList();
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter("OUT-JAVA.DAT"))) {
            for (String line : lines) {
                String acct = line.substring(0, 9);                          // PIC 9(9)
                BigDecimal bal = implied(line.substring(9));                 // PIC 9(9)V99
                BigDecimal interest = bal.multiply(RATE).setScale(SCALE, RoundingMode.HALF_UP);
                BigDecimal newBal = bal.add(interest);
                w.write(zeroPad(acct, INT_DIGITS));
                w.write(zeroPad(bal, INT_DIGITS + SCALE));
                w.write(zeroPad(interest, INT_DIGITS + SCALE));
                w.write(zeroPad(newBal, INT_DIGITS + SCALE));
                w.newLine();
            }
        }
        System.out.println("wrote OUT-JAVA.DAT (" + lines.size() + " records)");
    }

    /** 11-digit implied-decimal (V99) string -> BigDecimal. */
    private static BigDecimal implied(String digits) {
        return new BigDecimal(new BigInteger(digits), SCALE);
    }

    /** Render a BigDecimal as zero-padded fixed-width digit string (no decimal point). */
    private static String zeroPad(BigDecimal v, int width) {
        String plain = v.setScale(SCALE, RoundingMode.HALF_UP).toPlainString().replace(".", "");
        if (plain.length() > width) {
            throw new IllegalStateException("Value too wide for PIC width " + width + ": " + v);
        }
        StringBuilder sb = new StringBuilder(plain);
        while (sb.length() < width) sb.insert(0, '0');
        return sb.toString();
    }

    /** Zero-pad a digit string (e.g. 9-digit ACCT-NO) to a fixed width. */
    private static String zeroPad(String digits, int width) {
        if (digits.length() > width) {
            throw new IllegalStateException("String too wide for PIC width " + width + ": " + digits);
        }
        StringBuilder sb = new StringBuilder(digits);
        while (sb.length() < width) sb.insert(0, '0');
        return sb.toString();
    }
}