package com.miamish.connectors.rds.iam;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IamAuthenticationConnPool extends ConnectionPool implements Runnable {

    public static final int TOKEN_REFRESH_IN_MILLIS = 60 * 1000;

    private ScheduledExecutorService scheduledExecutorService;
    private RdsConnectionInformation rdsConnectionInformation;

    private static final Logger logger = LoggerFactory.getLogger(IamAuthenticationConnPool.class);

    public IamAuthenticationConnPool(PoolConfiguration prop) throws SQLException {
        super(prop);
    }

    @Override
    protected void init(PoolConfiguration poolConfiguration) {
        logger.info("Start connection pool initialization.");
        try {
            rdsConnectionInformation = new RdsConnectionInformation.Builder(poolConfiguration).build();
            updatePassword(poolConfiguration);
            enrichPoolConfiguration(poolConfiguration);
            super.init(poolConfiguration);
            this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(this, 0L, TOKEN_REFRESH_IN_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            logger.error("connection pool initialization failed", e);
        }
        logger.info("Initialized connection pool successfully for {}.", rdsConnectionInformation);
    }

    protected void enrichPoolConfiguration(PoolConfiguration poolConfiguration) {
        final Properties props = poolConfiguration.getDbProperties();
        props.setProperty("verifyServerCertificate","true");
        props.setProperty("user", rdsConnectionInformation.getUsername());
        props.setProperty("useSSL","true");
        props.setProperty("requireSSL","true");
        // If you need extra props, you may override this method
        // Common extra props:
        // props.setProperty("trustCertificateKeyStoreUrl",getClass().getResource(certs).toString());
        // props.setProperty("trustCertificateKeyStorePassword", keystorePassword);

    }

    @Override
    public void run() {
        try {
            updatePassword(getPoolProperties());
        } catch (Throwable t) {
            logger.error("SEVERE failed to update RDS IAM token.", t);
        }
    }

    private void updatePassword(PoolConfiguration props) {
        String token = generateAuthToken();
        logger.debug("Updated token for {}", rdsConnectionInformation);
        props.setPassword(token);
    }

    private String generateAuthToken() {
        RdsIamAuthTokenGenerator generator = RdsIamAuthTokenGenerator.builder()
                .credentials(new DefaultAWSCredentialsProviderChain())
                .region(rdsConnectionInformation.getRegion())
                .build();
        return generator.getAuthToken(GetIamAuthTokenRequest.builder()
                .hostname(rdsConnectionInformation.getHost())
                .port(rdsConnectionInformation.getPort())
                .userName(rdsConnectionInformation.getUsername())
                .build());
    }

    @Override
    protected void close(boolean force) {
        super.close(force);
        this.scheduledExecutorService.shutdown();
        logger.info("Closing scheduledExecutorService (isNull={})", Objects.isNull(scheduledExecutorService));
        Optional.ofNullable(this.scheduledExecutorService).ifPresent(service -> {
            service.shutdown();
            logger.info("Closed scheduledExecutorService");
        });
    }

}