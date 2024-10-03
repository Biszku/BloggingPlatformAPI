package com.biszku.BloggingPlatformAPI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

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
                        PostToSave postFields = objectMapper.readValue(data, PostToSave.class);

                        String title = postFields.title();
                        String content = postFields.content();
                        String category = postFields.category();
                        List<String> tags = List.of(postFields.tags());

                        Post post = new Post(title, content, category, tags);
                        session.persist(post);
                        transaction.commit();

                        response = objectMapper.writeValueAsString(post);
                        exchange.sendResponseHeaders(201, response.getBytes().length);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        response = "{\"message\": \"Incorrect \"}";
                        exchange.sendResponseHeaders(400, response.getBytes().length);
                    } finally {
                        session.close();
                    }
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                } else if (requestMethod.equals("GET")) {
                    Session session = HibernateUtil.getSessionFactory().openSession();
                    Transaction transaction = session.beginTransaction();
                    String path = exchange.getRequestURI().getPath();

                    String response = "";
                    try {
                        List<Post> posts = session.createQuery("FROM Post", Post.class).getResultList();
                        transaction.commit();

                        response = objectMapper.writeValueAsString(posts);
                        exchange.sendResponseHeaders(201, response.getBytes().length);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        response = "{\"message\": \"Error occured while creating post\"}";
                        exchange.sendResponseHeaders(204, response.getBytes().length);
                    } finally {
                        session.close();
                    }
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