package com.miamish.connectors.rds.iam;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class RdsConnectionInformation implements Serializable {

    private String host;
    private String region;
    private int port;
    private String username;

    private RdsConnectionInformation() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("host=\"").append(host).append("\", ")
                .append("region=\"").append(region).append("\", ")
                .append("port=\"").append(port).append("\", ")
                .append("username=\"").append(username).append("\"")
                .toString();
    }

    public static class Builder {

        private RdsConnectionInformation rdsConnectionInformation = new RdsConnectionInformation();

        public Builder(PoolConfiguration poolConfiguration) {
            this.rdsConnectionInformation.setHost(getDbHost(poolConfiguration.getUrl()));
            this.rdsConnectionInformation.setPort(getDbPort(poolConfiguration.getUrl()));
            this.rdsConnectionInformation.setRegion(getDbRegion(poolConfiguration.getUrl()));
            this.rdsConnectionInformation.setUsername(getDbUsername(poolConfiguration.getUsername()));
        }

        public RdsConnectionInformation build() {
            return rdsConnectionInformation;
        }

        private String getDbUsername(String username) {
            return Optional.ofNullable(username)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Username is null"
                    ));
        }

        private String getDbRegion(String url) {
            return Optional.ofNullable(getDbHost(url))
                    .map(h -> StringUtils.split(h, "."))
                    .filter(a -> a.length > 2)
                    .map(a -> a[2])
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "Region for url=\"%s\" is invalid", url
                    )));
        }

        private int getDbPort(String url) {
            return Optional.ofNullable(getDbUrl(url))
                    .map(URI::getPort)
                    .filter(port -> port > 0)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "Port for url=\"%s\" is invalid", url
                    )));

        }

        private String getDbHost(String url) {
            return Optional.ofNullable(getDbUrl(url))
                    .map(URI::getHost)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                            "Host for url=\"%s\" is invalid", url
                    )));
        }

        private URI getDbUrl(String url) {
            return Optional.ofNullable(url)
                    .filter(jdbcUrl -> jdbcUrl.startsWith("jdbc:"))
                    .map(jdbcUrl -> jdbcUrl.substring(5))
                    .map(jdbcUrl -> {
                        try {
                            return new URI(jdbcUrl);
                        } catch (URISyntaxException e) {
                            throw new IllegalArgumentException(String.format("JDBC url \"%s\" is invalid", jdbcUrl), e);
                        }
                    }).orElseThrow(() -> new IllegalArgumentException(String.format("Url \"%s\" is invalid", url)));
        }

    }

}
