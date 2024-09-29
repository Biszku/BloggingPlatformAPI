package com.biszku.BloggingPlatformAPI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {

    public static void main(String[] args) {

        ObjectMapper objectMapper = new ObjectMapper();

        try {

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/posts", exchange -> {
                String requestMethod = exchange.getRequestMethod();

                if (requestMethod.equals("POST")) {

                    Session session = HibernateUtil.getSessionFactory().openSession();
                    Transaction transaction = session.beginTransaction();

                    String response = "";
                    try {
                        String data = new String(exchange.getRequestBody().readAllBytes());
                        Post user = objectMapper.readValue(data, Post.class);

                        String title = user.getTitle();
                        String content = user.getContent();
                        String category = user.getCategory();

                        Post post = new Post(title, content, category);
                        session.save(post);
                        transaction.commit();

                        response = "{\"message\": \"Post created successfully\"}";
                        exchange.sendResponseHeaders(201, response.getBytes().length);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        response = "{\"message\": \"Error occured while creating post\"}";
                        exchange.sendResponseHeaders(500, response.getBytes().length);
                    } finally {
                        session.close();
                    }
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                }

                if (requestMethod.equals("GET")) {

                    String response = "{\"message\": \"User retrieved successfully\"}";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                }
            });

            server.setExecutor(null);
            server.start();

            System.out.println("Server is listening on port 8080...");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class HibernateUtil {
    private static SessionFactory sessionFactory;

    static {
        sessionFactory = new Configuration().configure().buildSessionFactory();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}