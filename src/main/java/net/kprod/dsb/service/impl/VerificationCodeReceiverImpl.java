package net.kprod.dsb.service.impl;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.util.Throwables;
import com.sun.net.httpserver.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

@Service
public class VerificationCodeReceiverImpl implements VerificationCodeReceiver {
    private static final String LOCALHOST = "localhost";
    private static final String CALLBACK_PATH = "/Callback";
    private HttpServer server;
    String code;
    String error;
    final Semaphore waitUnlessSignaled;
    private int port;
    private final String host;
    private final String callbackPath;
    private String successLandingPageUrl;
    private String failureLandingPageUrl;

    public VerificationCodeReceiverImpl() {
        this("localhost", -1, "/Callback", (String)null, (String)null);
    }

    VerificationCodeReceiverImpl(String host, int port, String successLandingPageUrl, String failureLandingPageUrl) {
        this(host, port, "/Callback", successLandingPageUrl, failureLandingPageUrl);
    }

    VerificationCodeReceiverImpl(String host, int port, String callbackPath, String successLandingPageUrl, String failureLandingPageUrl) {
        this.waitUnlessSignaled = new Semaphore(0);
        this.host = host;
        this.port = port;
        this.callbackPath = callbackPath;
        this.successLandingPageUrl = successLandingPageUrl;
        this.failureLandingPageUrl = failureLandingPageUrl;
    }

    public String getRedirectUri() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(this.port != -1 ? this.port : this.findOpenPort()), 0);
        HttpContext context = this.server.createContext(this.callbackPath, new VerificationCodeReceiverImpl.CallbackHandler());
        this.server.setExecutor((Executor)null);

        try {
            this.server.start();
            this.port = this.server.getAddress().getPort();
        } catch (Exception e) {
            Throwables.propagateIfPossible(e);
            throw new IOException(e);
        }

        return "https://" + this.getHost() + ":" + this.port + this.callbackPath;
    }

    private int findOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            int var3 = socket.getLocalPort();
            return var3;
        } catch (IOException var15) {
            throw new IllegalStateException("No free TCP/IP port to start embedded HTTP Server on");
        }
    }

    public String waitForCode() throws IOException {
        this.waitUnlessSignaled.acquireUninterruptibly();
        if (this.error != null) {
            throw new IOException("User authorization failed (" + this.error + ")");
        } else {
            return this.code;
        }
    }

    public void stop() throws IOException {
        this.waitUnlessSignaled.release();
        if (this.server != null) {
            try {
                this.server.stop(0);
            } catch (Exception e) {
                Throwables.propagateIfPossible(e);
                throw new IOException(e);
            }

            this.server = null;
        }

    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getCallbackPath() {
        return this.callbackPath;
    }

    public static final class Builder {
        private String host = "localhost";
        private int port = -1;
        private String successLandingPageUrl;
        private String failureLandingPageUrl;
        private String callbackPath = "/Callback";

        public Builder() {
        }

        public VerificationCodeReceiverImpl build() {
            return new VerificationCodeReceiverImpl(this.host, this.port, this.callbackPath, this.successLandingPageUrl, this.failureLandingPageUrl);
        }

        public String getHost() {
            return this.host;
        }

        public VerificationCodeReceiverImpl.Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public int getPort() {
            return this.port;
        }

        public VerificationCodeReceiverImpl.Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public String getCallbackPath() {
            return this.callbackPath;
        }

        public VerificationCodeReceiverImpl.Builder setCallbackPath(String callbackPath) {
            this.callbackPath = callbackPath;
            return this;
        }

        public VerificationCodeReceiverImpl.Builder setLandingPages(String successLandingPageUrl, String failureLandingPageUrl) {
            this.successLandingPageUrl = successLandingPageUrl;
            this.failureLandingPageUrl = failureLandingPageUrl;
            return this;
        }
    }

    class CallbackHandler implements HttpHandler {
        CallbackHandler() {
        }

        public void handle(HttpExchange httpExchange) throws IOException {
            if (VerificationCodeReceiverImpl.this.callbackPath.equals(httpExchange.getRequestURI().getPath())) {
                new StringBuilder();

                try {
                    Map<String, String> parms = this.queryToMap(httpExchange.getRequestURI().getQuery());
                    VerificationCodeReceiverImpl.this.error = (String)parms.get("error");
                    VerificationCodeReceiverImpl.this.code = (String)parms.get("code");
                    Headers respHeaders = httpExchange.getResponseHeaders();
                    if (VerificationCodeReceiverImpl.this.error == null && VerificationCodeReceiverImpl.this.successLandingPageUrl != null) {
                        respHeaders.add("Location", VerificationCodeReceiverImpl.this.successLandingPageUrl);
                        httpExchange.sendResponseHeaders(302, -1L);
                    } else if (VerificationCodeReceiverImpl.this.error != null && VerificationCodeReceiverImpl.this.failureLandingPageUrl != null) {
                        respHeaders.add("Location", VerificationCodeReceiverImpl.this.failureLandingPageUrl);
                        httpExchange.sendResponseHeaders(302, -1L);
                    } else {
                        this.writeLandingHtml(httpExchange, respHeaders);
                    }

                    httpExchange.close();
                } finally {
                    VerificationCodeReceiverImpl.this.waitUnlessSignaled.release();
                }

            }
        }

        private Map<String, String> queryToMap(String query) {
            Map<String, String> result = new HashMap();
            if (query != null) {
                for(String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        result.put(pair[0], pair[1]);
                    } else {
                        result.put(pair[0], "");
                    }
                }
            }

            return result;
        }

        private void writeLandingHtml(HttpExchange exchange, Headers headers) throws IOException {
            try (OutputStream os = exchange.getResponseBody()) {
                exchange.sendResponseHeaders(200, 0L);
                headers.add("ContentType", "text/html");
                OutputStreamWriter doc = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                doc.write("<html>");
                doc.write("<head><title>OAuth 2.0 Authentication Token Received</title></head>");
                doc.write("<body>");
                doc.write("Received verification code. You may now close this window.");
                doc.write("</body>");
                doc.write("</html>\n");
                doc.flush();
            }

        }
    }
}
