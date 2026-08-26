package com.syncturtle.services.email.support.smtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A minimal SMTP server intended for integration tests.
 * 
 * <p>
 * The server opens a real TCP port on the local machine and accepts one
 * SMTP client connection.
 */
public final class TestSmtpServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final CountDownLatch messageReceived = new CountDownLatch(1);
    private final List<String> messageLines = Collections.synchronizedList(new ArrayList<>());

    private TestSmtpServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.submit(this::serverOneConnection);
    }

    public static TestSmtpServer start() throws IOException {
        return new TestSmtpServer(new ServerSocket(0));
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public boolean awaitMessage(Duration timeout) throws InterruptedException {
        return messageReceived.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * {@code String.join()} must traverse the entire collection, during traversal
     * another thread could modify the list. Therefore its synchronized so it locsk
     * the list during the entire traversal
     * 
     * @return The email message as a 1 liner
     */
    public String getMessageData() {
        synchronized (messageLines) {
            return String.join("\n", messageLines);
        }
    }

    private void serverOneConnection() {
        try (Socket socket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII))) {
            reply(writer, "220 localhost ESMTP test server");

            boolean readingData = false;
            String line;

            while ((line = reader.readLine()) != null) {
                if (readingData) {
                    if (".".equals(line)) {
                        readingData = false;
                        messageReceived.countDown();
                        reply(writer, "250 message accepted");
                    } else {
                        messageLines.add(line);
                    }
                    continue;
                }

                String command = line.toUpperCase(Locale.ROOT);

                if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                    writer.write("250-localhost\r\n");
                    writer.write("250 8BITMIME\r\n");
                    writer.flush();
                } else if (command.startsWith("MAIL FROM") || command.startsWith("RCPT TO")) {
                    reply(writer, "250 OK");
                } else if (command.equals("DATA")) {
                    readingData = true;
                    reply(writer, "354 end data with <CR><LF>.<CR><LF>");
                } else if (command.equals("QUIT")) {
                    reply(writer, "221 bye");
                    return;
                } else {
                    reply(writer, "250 OK");
                }
            }
        } catch (IOException ignored) {
            // closing the server during cleanup may interrupt accept/read.
        }
    }

    private static void reply(BufferedWriter writer, String response) throws IOException {
        writer.write(response);
        writer.write("\r\n");
        writer.flush();
    }

    @Override
    public void close() throws Exception {
        serverSocket.close();
        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

}
