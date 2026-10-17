package com.example.health.mate.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Disposable loopback SMTP sink: messages travel through an actual TCP SMTP session. */
public final class LocalSmtpServer implements AutoCloseable {
    private final ServerSocket listener;
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
    private volatile boolean rejectMessages;
    private volatile boolean closed;

    public LocalSmtpServer() throws IOException {
        listener = new ServerSocket(0, 16, InetAddress.getLoopbackAddress());
        Thread server = new Thread(this::accept, "test-smtp");
        server.setDaemon(true);
        server.start();
    }

    public int port() { return listener.getLocalPort(); }
    public void rejectMessages(boolean reject) { rejectMessages = reject; }

    public String awaitMessage(Duration timeout) throws InterruptedException {
        return messages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void accept() {
        while (!closed) {
            try (Socket socket = listener.accept()) {
                socket.setSoTimeout(5000);
                receive(socket);
            } catch (IOException ex) {
                if (!closed) throw new IllegalStateException("Test SMTP server failed", ex);
            }
        }
    }

    private void receive(Socket socket) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        reply(output, "220 localhost test SMTP");
        String line;
        StringBuilder envelope = new StringBuilder();
        while ((line = input.readLine()) != null) {
            String command = line.toUpperCase(java.util.Locale.ROOT);
            if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                reply(output, "250 localhost");
            } else if (command.startsWith("MAIL FROM:") || command.startsWith("RCPT TO:")) {
                envelope.append(line).append('\n');
                reply(output, "250 OK");
            } else if (command.equals("DATA")) {
                if (rejectMessages) {
                    reply(output, "554 Message rejected by test server");
                    continue;
                }
                reply(output, "354 End with a dot");
                StringBuilder message = new StringBuilder(envelope);
                while ((line = input.readLine()) != null && !line.equals(".")) {
                    message.append(line.startsWith("..") ? line.substring(1) : line).append('\n');
                }
                messages.add(message.toString());
                reply(output, "250 Message accepted");
            } else if (command.equals("QUIT")) {
                reply(output, "221 Bye");
                return;
            } else if (command.equals("RSET") || command.equals("NOOP")) {
                reply(output, "250 OK");
            } else {
                reply(output, "502 Unsupported command");
            }
        }
    }

    private void reply(BufferedWriter output, String response) throws IOException {
        output.write(response + "\r\n");
        output.flush();
    }

    @Override public void close() throws IOException {
        closed = true;
        listener.close();
    }
}
