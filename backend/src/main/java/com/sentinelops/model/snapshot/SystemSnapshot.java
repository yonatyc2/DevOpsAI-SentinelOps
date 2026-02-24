package com.sentinelops.model.snapshot;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Full system snapshot: Linux + Docker + Postgres.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemSnapshot {
    private Instant timestamp = Instant.now();
    private LinuxSnapshot linux;
    private DockerSnapshot docker;
    private PostgresSnapshot postgres;

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public LinuxSnapshot getLinux() { return linux; }
    public void setLinux(LinuxSnapshot linux) { this.linux = linux; }
    public DockerSnapshot getDocker() { return docker; }
    public void setDocker(DockerSnapshot docker) { this.docker = docker; }
    public PostgresSnapshot getPostgres() { return postgres; }
    public void setPostgres(PostgresSnapshot postgres) { this.postgres = postgres; }
}
