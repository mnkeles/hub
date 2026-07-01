package etiya.omniAutomation.service;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SshTunnelService {

    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> localPorts = new ConcurrentHashMap<>();

    public Integer createTunnel(String sshHost, int sshPort, String sshUser, String sshPassword, 
                                String remoteHost, int remotePort) {
        String key = sshHost + ":" + sshPort + ":" + remoteHost + ":" + remotePort;
        
        if (activeSessions.containsKey(key) && activeSessions.get(key).isConnected()) {
            log.info("SSH tunnel already exists for: {}", key);
            return localPorts.get(key);
        }

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);
            
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            session.connect();
            
            int localPort = session.setPortForwardingL(0, remoteHost, remotePort);
            
            activeSessions.put(key, session);
            localPorts.put(key, localPort);
            
            log.info("SSH tunnel created: localhost:{} -> {}:{} via {}@{}:{}", 
                    localPort, remoteHost, remotePort, sshUser, sshHost, sshPort);
            
            return localPort;
        } catch (Exception e) {
            log.error("Failed to create SSH tunnel: {}", e.getMessage(), e);
            throw new RuntimeException("SSH tunnel creation failed: " + e.getMessage(), e);
        }
    }

    public void closeTunnel(String sshHost, int sshPort, String remoteHost, int remotePort) {
        String key = sshHost + ":" + sshPort + ":" + remoteHost + ":" + remotePort;
        
        Session session = activeSessions.get(key);
        if (session != null && session.isConnected()) {
            session.disconnect();
            activeSessions.remove(key);
            localPorts.remove(key);
            log.info("SSH tunnel closed for: {}", key);
        }
    }

    public void closeAllTunnels() {
        activeSessions.values().forEach(session -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        activeSessions.clear();
        localPorts.clear();
        log.info("All SSH tunnels closed");
    }
}
