package com.test.bank.service;

import com.google.common.collect.Lists;
import com.test.bank.constant.Action;
import com.test.bank.db.tables.pojos.Transaction;
import com.test.bank.initializer.DataSourceInitializer;
import com.test.bank.model.transaction.TransactionVo;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.types.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import static com.test.bank.db.tables.Transaction.TRANSACTION;
import static com.test.bank.db.tables.User.USER;

@Singleton
public class TransactionService {

    Logger log = LoggerFactory.getLogger(TransactionService.class);
    DefaultConfiguration jooqConfiguration;

    @Inject
    public TransactionService(DataSourceInitializer dataSourceInitializer) {
        this.jooqConfiguration = dataSourceInitializer.getJooqConfiguration();
    }

    private boolean validateUserId(int userId) {
        DSLContext dsl = DSL.using(this.jooqConfiguration);
        return dsl.fetchExists(dsl
                .selectOne()
                .from(USER)
                .where(USER.ID.eq((UInteger.valueOf(userId)))));
    }

    public void transfer(int fromUserId, int toUserId, int amount, int adminId) {
        // TODO implement transfer
        if (!validateUserId(fromUserId)) {
            log.debug("Invalid fromUserId: " + fromUserId);
            return;
        }
        if (!validateUserId(toUserId)) {
            log.debug("Invalid toUserId: " + toUserId);
            return;
        }
        DSLContext dsl = DSL.using(this.jooqConfiguration);
        int fromUserWallet = dsl
                .select()
                .from(USER)
                .where(USER.ID.eq(UInteger.valueOf(fromUserId)))
                .fetchOne()
                .getValue(USER.WALLET);
        if (fromUserWallet < amount) {
            log.debug("Not enough, transfer " + amount + " with wallet: " + fromUserWallet);
            return;
        }
        int toUserWallet = dsl
                .select()
                .from(USER)
                .where(USER.ID.eq(UInteger.valueOf(toUserId)))
                .fetchOne()
                .getValue(USER.WALLET);
        // Use jOOQ transaction API to help with transaction / rollback management
        // ref: https://www.jooq.org/doc/3.11/manual/sql-execution/transaction-management/
        try {
            dsl.transaction(configuration -> {
                DSL.using(configuration)
                        .insertInto(USER, USER.ID, USER.WALLET)
                        .values(UInteger.valueOf(fromUserId), fromUserWallet - amount)
                        .onDuplicateKeyUpdate()
                        .set(USER.WALLET, fromUserWallet - amount)
                        .execute();
                DSL.using(configuration)
                        .insertInto(USER, USER.ID, USER.WALLET)
                        .values(UInteger.valueOf(toUserId), toUserWallet + amount)
                        .onDuplicateKeyUpdate()
                        .set(USER.WALLET, toUserWallet + amount)
                        .execute();
            });
        } catch (RuntimeException e) {
            log.debug("Transaction 'transfer' failed, rolls back the transaction");
            throw new RuntimeException(e.getMessage(), e);
        }
        log.debug("Transaction 'transfer' successful");
        dsl.insertInto(TRANSACTION,
                TRANSACTION.FROMUSERID, TRANSACTION.TOUSERID, TRANSACTION.ACTION, TRANSACTION.ADMINID)
                .values(UInteger.valueOf(fromUserId),
                        UInteger.valueOf(toUserId),
                        Byte.valueOf(Action.TRANSFER),
                        UInteger.valueOf(adminId))
                .execute();
    }

    public TransactionVo getTransactionLog(int userId) {
        // TODO implement getTransactionLog
        if (!validateUserId(userId)) {
            log.debug("Invalid userId: " + userId);
            return null;
        }
        Result<?> records = DSL.using(this.jooqConfiguration)
                .select()
                .from(TRANSACTION)
                .where(TRANSACTION.FROMUSERID.eq(UInteger.valueOf(userId)))
                .or(TRANSACTION.TOUSERID.eq(UInteger.valueOf(userId)))
                .fetch();
        List<Transaction> transactionList = Lists.newArrayList();
        for (Record r : records) {
            transactionList.add(
                    new Transaction(
                    r.getValue(TRANSACTION.ID),
                    r.getValue(TRANSACTION.FROMUSERID),
                    r.getValue(TRANSACTION.TOUSERID),
                    r.getValue(TRANSACTION.ACTION),
                    r.getValue(TRANSACTION.ADMINID),
                    r.getValue(TRANSACTION.CREATEDAT)
            ));
        }
        return new TransactionVo(transactionList);
    }

    public void creditAndDebit(int userId, int amount, int adminId) {
        // TODO implement creditAndDebit
    }
}
