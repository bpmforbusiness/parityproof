* Copybook ACCOUNTS — record layout for ACCOUNT-INT
       01  ACCT-REC.
           05 ACCT-NO      PIC 9(9).    *> account id, 9 digits
           05 BAL          PIC 9(9)V99. *> balance, implied 2 decimals
      * Result record (OUT-REC): leading 9 acct + 11 balance + 11 interest + 11 new-bal