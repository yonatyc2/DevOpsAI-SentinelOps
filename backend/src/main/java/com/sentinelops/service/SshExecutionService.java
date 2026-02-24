package com.sentinelops.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import com.sentinelops.config.SshProperties;
import com.sentinelops.model.Server;
import com.sentinelops.repository.ServerRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Executes commands on a remote host via SSH.
 * Uses JSch for SSH connectivity with configurable timeouts.
 */
@Service
public class SshExecutionService {

    private final SshProperties sshProperties;
    private final ServerRepository serverRepository;
    private final CredentialEncryptionService encryptionService;

    public SshExecutionService(SshProperties sshProperties, ServerRepository serverRepository,
                               CredentialEncryptionService encryptionService) {
        this.sshProperties = sshProperties;
        this.serverRepository = serverRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Execute a single command on the configured SSH host (default from config).
     */
    public Optional<SshCommandResult> execute(String command) {
        return doExecute(sshProperties.getHost(), sshProperties.getPort(),
                sshProperties.getUsername(), sshProperties.getPassword(), null, command,
                sshProperties.getPrivateKeyPath());
    }

    /**
     * Execute on a specific server by id. If serverId is null/blank, uses default config.
     */
    public Optional<SshCommandResult> executeWithServer(String serverId, String command) {
        if (serverId != null && !serverId.isBlank()) {
            Optional<Server> server = serverRepository.findById(serverId);
            if (server.isPresent()) {
                Server s = server.get();
                String cred = s.getEncryptedCredential() != null ? encryptionService.decrypt(s.getEncryptedCredential()) : null;
                String password = s.getAuthType() == Server.AuthType.PASSWORD ? cred : null;
                String keyContent = s.getAuthType() == Server.AuthType.PRIVATE_KEY ? cred : null;
                return executeWithCredentials(s.getHost(), s.getPort(), s.getUsername(), password, keyContent, command);
            }
        }
        return execute(command);
    }

    public Optional<SshCommandResult> execute(String host, int port, String username, String command) {
        return execute(host, port, username, null, null, command);
    }

    /**
     * Execute with optional key path or password (file-based key).
     */
    public Optional<SshCommandResult> execute(String host, int port, String username,
                                               String privateKeyPath, String password,
                                               String command) {
        return executeWithCredentials(host, port, username, password, null, command, privateKeyPath);
    }

    public Optional<SshCommandResult> executeWithCredentials(String host, int port, String username,
                                                              String password, String privateKeyContent,
                                                              String command, String privateKeyPath) {
        return doExecute(host, port, username, password, privateKeyContent, command, privateKeyPath);
    }

    /**
     * Execute with password or inline private key content (no key file path).
     */
    public Optional<SshCommandResult> executeWithCredentials(String host, int port, String username,
                                                              String password, String privateKeyContent,
                                                              String command) {
        return doExecute(host, port, username, password, privateKeyContent, command, null);
    }

    private Optional<SshCommandResult> doExecute(String host, int port, String username,
                                                  String password, String privateKeyContent,
                                                  String command, String privateKeyPath) {
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(username, host, port);
            if (password != null && !password.isBlank()) {
                session.setPassword(password);
            }
            if (privateKeyContent != null && !privateKeyContent.isBlank()) {
                jsch.addIdentity("key", privateKeyContent.getBytes(StandardCharsets.UTF_8), null, null);
            } else if (privateKeyPath != null && !privateKeyPath.isBlank()) {
                jsch.addIdentity(privateKeyPath);
            }
            session.setConfig("StrictHostKeyChecking", "no");
            boolean passwordOnlyAuth = password != null && !password.isBlank()
                    && (privateKeyContent == null || privateKeyContent.isBlank())
                    && (privateKeyPath == null || privateKeyPath.isBlank());
            applyLegacyCompatibleSshConfig(session, passwordOnlyAuth);
            if (passwordOnlyAuth) {
                session.setUserInfo(new PasswordUserInfo(password));
            }
            session.setTimeout(sshProperties.getConnectTimeoutMs());
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            channel.setOutputStream(stdout);
            channel.setErrStream(stderr);

            channel.connect(sshProperties.getCommandTimeoutMs());

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            int exitCode = channel.getExitStatus();
            String out = stdout.toString(StandardCharsets.UTF_8.name());
            String err = stderr.toString(StandardCharsets.UTF_8.name());
            channel.disconnect();
            return Optional.of(new SshCommandResult(exitCode, out, err));
        } catch (Exception e) {
            return Optional.of(SshCommandResult.error(e.getMessage()));
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Keep modern algorithms first, but include legacy fallbacks for older SSH daemons.
     * This helps when servers only support SHA-1 based KEX/host key/cipher suites.
     */
    private void applyLegacyCompatibleSshConfig(Session session, boolean passwordOnlyAuth) {
        if (passwordOnlyAuth) {
            // Force password path for environments where public key probes break auth flow.
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
            session.setConfig("PubkeyAuthentication", "no");
        } else {
            session.setConfig("PreferredAuthentications", "password,publickey,keyboard-interactive");
        }
        session.setConfig("kex",
                "curve25519-sha256,curve25519-sha256@libssh.org," +
                "ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521," +
                "diffie-hellman-group-exchange-sha256,diffie-hellman-group14-sha256," +
                "diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1," +
                "diffie-hellman-group1-sha1");
        session.setConfig("server_host_key", "ssh-ed25519,ecdsa-sha2-nistp256,ssh-rsa,rsa-sha2-512,rsa-sha2-256,ssh-dss");
        session.setConfig("PubkeyAcceptedAlgorithms", "ssh-ed25519,ecdsa-sha2-nistp256,rsa-sha2-512,rsa-sha2-256,ssh-rsa,ssh-dss");
        session.setConfig("cipher.c2s", "aes256-ctr,aes192-ctr,aes128-ctr,aes128-cbc,3des-cbc");
        session.setConfig("cipher.s2c", "aes256-ctr,aes192-ctr,aes128-ctr,aes128-cbc,3des-cbc");
        session.setConfig("mac.c2s",
                "hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com," +
                "hmac-sha2-512,hmac-sha2-256,hmac-sha1,hmac-md5");
        session.setConfig("mac.s2c",
                "hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com," +
                "hmac-sha2-512,hmac-sha2-256,hmac-sha1,hmac-md5");
    }

    private static final class PasswordUserInfo implements UserInfo, UIKeyboardInteractive {
        private final String password;

        private PasswordUserInfo(String password) {
            this.password = password;
        }

        @Override
        public String getPassphrase() { return null; }

        @Override
        public String getPassword() { return password; }

        @Override
        public boolean promptPassword(String message) { return true; }

        @Override
        public boolean promptPassphrase(String message) { return false; }

        @Override
        public boolean promptYesNo(String message) { return true; }

        @Override
        public void showMessage(String message) { }

        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction,
                                                  String[] prompt, boolean[] echo) {
            String[] responses = new String[prompt.length];
            for (int i = 0; i < prompt.length; i++) {
                responses[i] = password;
            }
            return responses;
        }
    }

    public static final class SshCommandResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public SshCommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout != null ? stdout : "";
            this.stderr = stderr != null ? stderr : "";
        }

        public static SshCommandResult error(String message) {
            return new SshCommandResult(-1, "", message);
        }

        public int getExitCode() { return exitCode; }
        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public boolean isSuccess() { return exitCode == 0; }
    }
}
