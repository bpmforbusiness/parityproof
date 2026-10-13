*> TXN-POST: batch transaction posting. For each transaction record, computes the
      *> account's end-of-day balance (opening balance + signed amount), flags overdrafts,
      *> and keeps a running batch total. Balances are held as packed decimal (COMP-3).
      *> Copybook: txn-post.cpy.   Compile: cobc -free -x TXN-POST.cbl -o txn_post
IDENTIFICATION DIVISION.
PROGRAM-ID. TXN-POST.
ENVIRONMENT DIVISION.
INPUT-OUTPUT SECTION.
FILE-CONTROL.
    SELECT IN-FILE ASSIGN TO "TXNS.DAT"
        ORGANIZATION IS LINE SEQUENTIAL.
    SELECT OUT-FILE ASSIGN TO "OUT-TXN-COBOL.DAT"
        ORGANIZATION IS LINE SEQUENTIAL.
DATA DIVISION.
FILE SECTION.
FD  IN-FILE.
01  IN-REC.
    05 T-ACCT      PIC 9(9).
    05 T-OPENBAL   PIC S9(9)V99 SIGN LEADING SEPARATE.
    05 T-AMT       PIC S9(7)V99 SIGN LEADING SEPARATE.
FD  OUT-FILE.
01  OUT-REC.
    05 O-ACCT      PIC 9(9).
    05 O-AMT       PIC S9(7)V99 SIGN LEADING SEPARATE.
    05 O-ENDBAL    PIC S9(9)V99 SIGN LEADING SEPARATE.
    05 O-ODFLAG    PIC X.
    05 O-BATCHTOT  PIC S9(9)V99 SIGN LEADING SEPARATE.
WORKING-STORAGE SECTION.
01  WS-ENDBAL   PIC S9(9)V99 COMP-3.
01  WS-BATCHTOT PIC S9(9)V99 COMP-3 VALUE 0.
01  WS-EOF      PIC X VALUE 'N'.
PROCEDURE DIVISION.
MAIN.
    OPEN INPUT IN-FILE.
    OPEN OUTPUT OUT-FILE.
    PERFORM UNTIL WS-EOF = 'Y'
        READ IN-FILE
            AT END MOVE 'Y' TO WS-EOF
            NOT AT END
                COMPUTE WS-ENDBAL = T-OPENBAL + T-AMT
                ADD T-AMT TO WS-BATCHTOT
                IF WS-ENDBAL < 0
                    MOVE 'Y' TO O-ODFLAG
                ELSE
                    MOVE 'N' TO O-ODFLAG
                END-IF
                MOVE T-ACCT TO O-ACCT
                MOVE T-AMT TO O-AMT
                MOVE WS-ENDBAL TO O-ENDBAL
                MOVE WS-BATCHTOT TO O-BATCHTOT
                WRITE OUT-REC
        END-READ
    END-PERFORM.
    CLOSE IN-FILE.
    CLOSE OUT-FILE.
    STOP RUN.

