      * Copybook TXN-POST — record layouts for TXN-POST (all fixed-width, line sequential)
      * Input record (31 chars):
       01  TXN-REC.
           05 T-ACCT      PIC 9(9).                          *> account id
           05 T-OPENBAL   PIC S9(9)V99 SIGN LEADING SEPARATE. *> opening balance, 12 chars (+/- then 11 digits)
           05 T-AMT       PIC S9(7)V99 SIGN LEADING SEPARATE. *> signed txn amount, 10 chars (+ credit / - debit)
      * Output record (44 chars): ACCT(9) AMT(10) ENDBAL(12) ODFLAG(1) BATCHTOT(12)
      * Working storage (packed decimal, never written raw to the file):
      *    WS-ENDBAL   PIC S9(9)V99 COMP-3.   end balance
      *    WS-BATCHTOT PIC S9(9)V99 COMP-3.   running batch total
