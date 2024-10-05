package com.biszku.BloggingPlatformAPI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public class Server {
    private HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public static void main(String[] args) {
        new Server().start();
    }

    private void start() {
        server = creationOfServer();
        createServerEndPoints();

        server.setExecutor(null);
        server.start();

        System.out.println("Server is listening on port 8080...");
    }

    private void createServerEndPoints() {
        server.createContext("/posts", exchange -> {
            String requestMethod = exchange.getRequestMethod();
            Session session = HibernateUtil.getSessionFactory().openSession();

            switch (requestMethod) {
                case "POST" -> {
                    Transaction transaction = session.beginTransaction();
                    String response = "";

                    try {
                        String data = new String(exchange.getRequestBody().readAllBytes());
                        Post post = creationOfPost(data);

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
                }
                case "GET" -> {
                    Transaction transaction = session.beginTransaction();

                    String response = "";
                    try {
                        List<Post> posts = session.createQuery("SELECT p FROM Post p", Post.class).getResultList();
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
            }
        });
    }

    private Post creationOfPost(String data) {
        try {
            return createPost(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Post createPost(String data) throws JsonProcessingException {
            PostToSave postFields = objectMapper.readValue(data, PostToSave.class);

            String title = postFields.title();
            String content = postFields.content();
            String category = postFields.category();

            Post post = new Post(title, content, category);

            List<Tag> tags = Arrays.stream(postFields.tags()).map(Tag::new).toList();
            post.setTags(tags);
            return post;
    }

    private HttpServer creationOfServer() {
        try {
            return createServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpServer createServer() throws IOException {
        return HttpServer.create(new InetSocketAddress(8080), 0);
    }
}

class HibernateUtil {
    private static final SessionFactory sessionFactory;

    static {
        sessionFactory = new Configuration().configure().buildSessionFactory();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}