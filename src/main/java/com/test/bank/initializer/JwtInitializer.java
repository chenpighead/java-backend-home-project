package com.test.bank.initializer;

import com.fuan.admin.filter.JwtAuthFilter;
import com.test.bank.constant.Role;
import com.test.bank.model.admin.AdminUserVo;
import com.test.bank.tool.config.EnvConfigManager;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.HmacKey;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ForbiddenException;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;

public class JwtInitializer {

    EnvConfigManager envConfigManager;
    RedissonClient redissonClient;

    public JwtInitializer(EnvConfigManager envConfigManager,
                          RedissonInitializer redissonInitializer) {
        this.redissonClient = redissonInitializer.getMainRedissonClient();
        this.envConfigManager = envConfigManager;
    }

    public void initialize(Environment environment) {
        final byte[] key = envConfigManager.getConfigAsString("jwtTokenSecret").getBytes();

        final JwtConsumer consumer = new JwtConsumerBuilder()
                .setAllowedClockSkewInSeconds(30)
                .setRequireExpirationTime()
                .setRequireSubject()
                .setVerificationKey(new HmacKey(key))
                .setRelaxVerificationKeyValidation()
                .build();

        environment.jersey().register(new AuthDynamicFeature(
                new JwtAuthFilter.Builder<AdminUserVo>()
                        .setJwtConsumer(consumer)
                        .setRealm("realm")
                        .setPrefix("Bearer")
                        .setAuthenticator(new AdminAuthenticator(this.redissonClient))
                        .setAuthorizer(new AdminAuthorizer())
                        .buildAuthFilter()));

        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(Principal.class));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
    }

    private static class AdminAuthenticator implements Authenticator<JwtContext, AdminUserVo> {

        Logger log = LoggerFactory.getLogger(AdminAuthenticator.class);
        RedissonClient redissonClient;

        public AdminAuthenticator(RedissonClient redissonClient) {
            this.redissonClient = redissonClient;
        }

        @Override
        public Optional<AdminUserVo> authenticate(JwtContext context) {
            // TODO add token authentication from redis
            try {
                String subject = context.getJwtClaims().getSubject();
                RMap<String, AdminUserVo> map = redissonClient.getMap("anyMap");
                return Optional.ofNullable(map.get(subject));
            } catch (MalformedClaimException exception) {
                log.debug("Cannot read malformed claim");
                throw new ForbiddenException();
            }
        }
    }

    private static class AdminAuthorizer implements Authorizer<AdminUserVo> {
        Logger log = LoggerFactory.getLogger(AdminAuthorizer.class);

        @Override
        public boolean authorize(AdminUserVo user, String role)
        {
            // TODO add other role authorize, thinking how to retrieve the role information
            Set<String> roles = user.getRoleSet();
            if (roles.contains(role) || roles.contains(Role.SUPER_ADMIN)) {
                log.debug("Successfully authorize " + user.getAccount() + " with role: " + role);
                return true;
            }
            log.debug("Fail to authorize " + user.getAccount() + " with role: " + role);
            return false;
        }
    }
}
