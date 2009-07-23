package org.math.R;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Properties;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class RserverConf {

    RConnection connection;
    public String host;
    public int port;
    public String login;
    public String password;
    public String RLibPath;
    public Properties properties;
    //public String http_proxy;

    public RserverConf(String RserverHostName, int RserverPort, String login, String password,Properties props) {
        this.host = RserverHostName;
        this.port = RserverPort;
        this.login = login;
        this.password = password;
        properties = props;
    }

    public RConnection connect() {
        
        System.out.println("Connecting " + toString());
        try {
            if (host == null) {
                if (port > 0) {
                    connection = new RConnection("localhost", port);
                } else {
                    connection = new RConnection();
                }
                if (connection.needLogin()) {
                    connection.login(login, password);
                }
            } else {
                if (port > 0) {
                    connection = new RConnection(host, port);
                } else {
                    connection = new RConnection(host);
                }
                if (connection.needLogin()) {
                    connection.login(login, password);
                }
            }
        } catch (RserveException ex) {
            //ex.printStackTrace();
            return null;
        }
        if (connection != null && connection.isConnected()) {
            if (properties != null) {
                for (String p : properties.stringPropertyNames()) {
                    try {
                        connection.eval("Sys.setenv(" + p + "=" + properties.getProperty(p) + ")");
                    } catch (RserveException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            try {
                //if (RLibPath == null) {
                boolean isWindows = connection.eval("as.logical(Sys.info()[1]=='Windows')").asInteger() == 1;
                RLibPath = "paste(Sys.getenv(\"HOME\"),\"Rserve\",sep=\"" + (isWindows ? "\\\\" : "/") + "\")";
                //}
                if (RLibPath != null) {
                    connection.eval("if(!file.exists(" + RLibPath + ")) dir.create(" + RLibPath + ")");
                    connection.eval(".libPaths(new=" + RLibPath + ")");
                }
            } catch (REXPMismatchException r) {
                r.printStackTrace();
            } catch (RserveException r) {
                r.printStackTrace();
            }

            return connection;
        } else {
            return null;
        }
    }
    public final static int RserverDefaultPort = 6311;
    private static int RserverPort = RserverDefaultPort; //used for windows multi-session emulation. Incremented at each new Rscript instance.

    public static boolean isPortAvailable(int p) {
        try {
            ServerSocket test = new ServerSocket(p);
            test.close();
        } catch (BindException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static RserverConf newLocalInstance(Properties p) {
        RserverConf server = null;
        if (System.getProperty("os.name").contains("Win") || !Rsession.UNIX_OPTIMIZE) {
            while (!isPortAvailable(RserverPort)) {
                RserverPort++;
            //System.out.println("RserverPort++ = " + RserverPort);
            }
            server = new RserverConf(null, RserverPort, null, null,p);
        } else { // Unix supports multi-sessions natively, so no need to open a different Rserve on a new port
            server = new RserverConf(null, -1, null, null,p);
        }
        return server;
    }

    public boolean isLocal() {
        return host == null || host.equals("localhost") || host.equals("127.0.0.1");
    }

    @Override
    public String toString() {
        return "R://" + (login != null ? (login + ":" + password + "@") : "") + (host == null ? "localhost" : host) + (port > 0 ? ":" + port : "") /*+ " http_proxy=" + http_proxy + " RLibPath=" + RLibPath*/;
    }

    public static RserverConf parse(String RURL) {
        String login = null;
        String passwd = null;
        String host = null;
        int port = -1;
        try {
            String hostport = null;
            if (RURL.contains("@")) {
                String loginpasswd = RURL.split("@")[0].substring(("R://").length());
                login = loginpasswd.split(":")[0];
                if (login.equals("user.name")) {
                    login = System.getProperty("user.name");
                }
                passwd = loginpasswd.split(":")[1];
                hostport = RURL.split("@")[1];
            } else {
                hostport = RURL.substring(("R://").length());
            }

            if (hostport.contains(":")) {
                host = hostport.split(":")[0];
                port = Integer.parseInt(hostport.split(":")[1]);
            } else {
                host = hostport;
            }

            return new RserverConf(host, port, login, passwd,null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible to parse " + RURL + ":\n  host=" + host + "\n  port=" + port + "\n  login=" + login + "\n  password=" + passwd);
        }

    }
}