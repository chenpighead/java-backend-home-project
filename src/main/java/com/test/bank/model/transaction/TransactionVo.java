package com.test.bank.model.transaction;

import com.test.bank.db.tables.pojos.Transaction;

import java.util.List;

public class TransactionVo {

    private List<Transaction> transactionList;

    public TransactionVo(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    public void setTransactions(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    public List<Transaction> getTransactions() {
        return transactionList;
    }
}
