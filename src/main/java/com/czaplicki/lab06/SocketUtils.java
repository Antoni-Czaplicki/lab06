package com.czaplicki.lab06;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketUtils {

    /**
     * Wysyła żądanie do serwera TCP i zwraca odpowiedź jako String.
     *
     * @param host    Host serwera (np. "127.0.0.1").
     * @param port    Port serwera.
     * @param request Żądanie do wysłania.
     * @param expectResponse Czy oczekujemy odpowiedzi od serwera.
     * @return Odpowiedź od serwera jako String.
     * @throws IOException Jeśli wystąpi problem z komunikacją.
     */
    public static String sendRequest(String host, int port, String request, boolean expectResponse) throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Wysłanie żądania do serwera
            out.println(request);

            if (!expectResponse) {
                return null;
            }
            // Oczekiwanie na odpowiedź od serwera
            return in.readLine();
        }
    }
    /**
     * Wysyła żądanie do serwera TCP i zwraca odpowiedź jako String.
     *
     * @param host    Host serwera (np. "127.0.0.1").
     * @param port    Port serwera.
     * @param request Żądanie do wysłania.
     * @return Odpowiedź od serwera jako String.
     * @throws IOException Jeśli wystąpi problem z komunikacją.
     */
    public static String sendRequest(String host, int port, String request) throws IOException {
        return sendRequest(host, port, request, true);
    }



    /**
     * Rozpoczyna prosty serwer TCP, który nasłuchuje na określonym porcie
     * i obsługuje przychodzące połączenia za pomocą podanej logiki obsługi.
     *
     * @param port        Port, na którym serwer nasłuchuje.
     * @param requestHandler Funkcja obsługująca żądanie (parametr: żądanie String, zwraca odpowiedź String).
     */
    public static void startServer(int port, RequestHandler requestHandler) {
        new Thread(() -> {
            try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {
                System.out.println("Serwer uruchomiony na porcie " + port);
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        // Odczytanie żądania od klienta
                        String request = in.readLine();
                        System.out.println("Otrzymano żądanie: " + request);

                        // Obsługa żądania
                        String response = requestHandler.handleRequest(request);

                        // Wysłanie odpowiedzi do klienta
                        out.println(response);

                    } catch (IOException e) {
                        System.err.println("Błąd obsługi klienta: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Błąd uruchamiania serwera: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Wykrywa host lokalny.
     */
    public static String getLocalHost() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * Interfejs definiujący obsługę żądań serwera.
     */
    @FunctionalInterface
    public interface RequestHandler {
        /**
         * Obsługuje żądanie i zwraca odpowiedź.
         *
         * @param request Żądanie w formie String.
         * @return Odpowiedź w formie String.
         */
        String handleRequest(String request);
    }
}