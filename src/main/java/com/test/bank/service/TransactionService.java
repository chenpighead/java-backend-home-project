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

    private boolean validateWallet(int userId, int amount) {
        int wallet = DSL.using(this.jooqConfiguration)
                .select()
                .from(USER)
                .where(USER.ID.eq(UInteger.valueOf(userId)))
                .fetchOne()
                .getValue(USER.WALLET);
        if (wallet >= amount) {
            return true;
        } else {
            log.debug("Not enough money, credit " + amount + " with wallet: " + wallet);
            return false;
        }
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
        if (fromUserId == toUserId) {
            // XXX: transfer to same user, so this is a creditAndDebit transaction!
            //      or, should we avoid / throw this?
            log.debug("transfer to same user, actually a creditAndDebit, calling it");
            creditAndDebit(fromUserId, amount, adminId);
            return;
        }
        if (amount < 0) {
            log.debug("Invalid amount, can't transfer with negative amount: " + amount);
            return;
        }
        if (!validateWallet(fromUserId, amount)) {
            return;
        }
        DSLContext dsl = DSL.using(this.jooqConfiguration);
        // Use jOOQ transaction API to help with transaction / rollback management
        // ref: https://www.jooq.org/doc/3.11/manual/sql-execution/transaction-management/
        try {
            dsl.transaction(configuration -> {
                DSLContext ctx = DSL.using(configuration);
                int fromUserWallet = ctx
                        .select()
                        .from(USER)
                        .where(USER.ID.eq(UInteger.valueOf(fromUserId)))
                        .fetchOne()
                        .getValue(USER.WALLET);
                int toUserWallet = ctx
                        .select()
                        .from(USER)
                        .where(USER.ID.eq(UInteger.valueOf(toUserId)))
                        .fetchOne()
                        .getValue(USER.WALLET);
                ctx
                        .insertInto(USER, USER.ID, USER.WALLET)
                        .values(UInteger.valueOf(fromUserId), fromUserWallet - amount)
                        .onDuplicateKeyUpdate()
                        .set(USER.WALLET, fromUserWallet - amount)
                        .execute();
                ctx
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
        if (!validateUserId(userId)) {
            log.debug("Invalid userId: " + userId);
            return;
        }
        if (amount < 0 && !validateWallet(userId, -amount)) {
            return;
        }
        DSLContext dsl = DSL.using(this.jooqConfiguration);
        // Use jOOQ transaction API to help with transaction / rollback management
        // ref: https://www.jooq.org/doc/3.11/manual/sql-execution/transaction-management/
        try {
            dsl.transaction(configuration -> {
                Integer wallet = DSL.using(configuration)
                        .select()
                        .from(USER)
                        .where(USER.ID.eq(UInteger.valueOf((userId))))
                        .fetchOne()
                        .getValue(USER.WALLET);
                DSL.using(configuration)
                        .insertInto(USER, USER.ID, USER.WALLET)
                        .values(UInteger.valueOf(userId), wallet + amount)
                        .onDuplicateKeyUpdate()
                        .set(USER.WALLET, wallet + amount)
                        .execute();
            });
        } catch (RuntimeException e) {
            log.debug("Transaction 'credit / debit' failed, rolls back the transaction");
            throw new RuntimeException(e.getMessage(), e);
        }
        log.debug("Transaction 'credit / debit' successful");
        dsl.insertInto(TRANSACTION,
                TRANSACTION.FROMUSERID, TRANSACTION.ACTION, TRANSACTION.ADMINID)
                .values(UInteger.valueOf(userId),
                        Byte.valueOf(Action.CREDIT_AND_DEBIT),
                        UInteger.valueOf(adminId))
                .execute();
    }
}
