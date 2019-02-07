package com.test.bank.service;

import com.google.common.collect.Sets;
import com.test.bank.initializer.DataSourceInitializer;
import com.test.bank.model.admin.AdminUserVo;
import com.test.bank.tool.PasswordUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.Set;

import static com.test.bank.db.tables.Admin.ADMIN;
import static com.test.bank.db.tables.Adminrole.ADMINROLE;

@Singleton
public class AdminService {

    Logger log = LoggerFactory.getLogger(AdminService.class);

    DefaultConfiguration jooqConfiguration;

    @Inject
    public AdminService(DataSourceInitializer dataSourceInitializer) {
        this.jooqConfiguration = dataSourceInitializer.getJooqConfiguration();
    }

    public AdminUserVo login(String account, String password) {
        // TODO implement login with PasswordUtils
        DSLContext dsl = DSL.using(this.jooqConfiguration);
        Record result = dsl
                .select()
                .from(ADMIN)
                .where(ADMIN.ACCOUNT.eq(account))
                .fetchOne();
        if (result == null) {
            log.debug("login failed, account " + account + " does not exist");
            return null;
        }
        if (result.getValue(ADMIN.ID).longValue() > Integer.MAX_VALUE) {
            log.error("ADMIN.ID overflow, can't store it in AdminUserVo");
            return null;
        }
        AdminUserVo adminUser = new AdminUserVo();
        adminUser.setId(result.getValue(ADMIN.ID).intValue());
        adminUser.setAccount(result.getValue(ADMIN.ACCOUNT));
        adminUser.setPassword(result.getValue(ADMIN.PASSWORD));
        adminUser.setSalt(result.getValue(ADMIN.SALT));
        List<Long> roleIds = dsl
                .select()
                .from(ADMINROLE)
                .where(ADMINROLE.ADMINID.eq(result.getValue(ADMIN.ID)))
                .fetch(ADMINROLE.ROLEID, Long.class);
        Set<String> roles = Sets.newHashSet();
        for (Long r : roleIds) {
            roles.add(String.valueOf(r));
        }
        adminUser.setRoleSet(roles);
        if (PasswordUtils.verifyUserPassword(
                password, adminUser.getPassword(), adminUser.getSalt())) {
            log.debug("login successful with account: " + account);
            return adminUser;
        } else {
            log.debug("login failed, wrong password for account: " + account);
            return null;
        }
    }

}
