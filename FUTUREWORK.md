# Future work
------

#### Feature Development

- Allow negative amount for an account? At present, the USER.WALLET's type is int, so technically speaking, negative amount is allowed. However, some scenerios might need to be clarified before going further.
  - Can or can't debit a user and cause a user's wallet become negative? (so far, yes)
  - Can or can't credit a negative wallet? (so far, yes)
  - Can or can't transfer from a negative wallet? (so far, no)
  - Can or can't allow a transfer to cause a negative wallet? (so far, no)
- client side response improvements, like status code, better structured response body
  - response body of transaction logs can be more well-structured and contain more semantics, ex. use `String` to represent transaction action, use admin's account instead of adminId... etc.
  - maybe further classify `creditAndDebit` transactions into `credit` and `debit`

#### Engineering and Code Quality

- Testing: API unit tests, integration tests

