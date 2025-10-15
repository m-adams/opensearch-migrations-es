package org.opensearch.migrations.bulkload.common.http;

import java.nio.file.Path;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ConnectionContextTestParams implements ConnectionContext.IParams {
    private String host;
    private String username;
    private String password;
    private String apiKey;
    private String awsRegion;
    private String awsServiceSigningName;
    @Builder.Default
    private boolean insecure = true;
    @Builder.Default
    private boolean disableCompression = false;

    private Path caCert;
    private Path clientCert;
    private Path clientCertKey;

    @Override
    public ConnectionContext.TargetType getTargetType() {
        return ConnectionContext.TargetType.OPENSEARCH; // Default for test params
    }
}
