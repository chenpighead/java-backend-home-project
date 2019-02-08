# Future work
------

#### Feature Development

- allow negative amount for an account? ***NO, NEGATIVE WALLET IS NOT ALLOWED***
- allow calling transfer with same user? with negative amount?
  - same user transfer, so far, we treat it like a `creditAndDebit` operation and redirecting the procedure there, however, not sure if we should forbid this kind of operation and return / throw to alert client side
  - followup, if we do allow redirecting same-user-transfer to `creditAndDebit`, does that means we allow negative amount transfer operation? ex. same user, transfer with negative amount, which is legal for `credit` operation. The point is, although we provide sort of tolerant mechanism for transfer operation, should we extend it to accept all available parameters that creditAndDebit accepts? Or, maybe we shouldn't be tolerant at all?
- client side response improvements, like status code, better structured response body
  - response body of transaction logs can be more well-structured and contain more semantics, ex. use `String` to represent transaction action, use admin's account instead of adminId... etc.
  - maybe further classify `creditAndDebit` transactions into `credit` and `debit`

#### Engineering and Code Quality

- testing: API unit tests, integration tests, thread-related tests (race condition)
- logging: maybe store production logs / exceptions / failed transaction operations etc. in a persistent storage, so we can easily debug if any future issue happens

