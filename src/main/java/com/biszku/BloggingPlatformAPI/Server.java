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
            Transaction transaction = session.beginTransaction();
            String response = "";

            switch (requestMethod) {
                case "POST" -> {
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
                    }
                }
                case "GET" -> {

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
                        exchange.sendResponseHeaders(404, response.getBytes().length);
                    }
                }
                case "PUT" -> {
                    String id = exchange.getRequestURI().toString().split("/")[2];

                    if (id == null) {
                        response = "{\"message\": \"Incorrect \"}";
                        exchange.sendResponseHeaders(400, response.getBytes().length);
                        break;
                    }

                    try {
                        String data = new String(exchange.getRequestBody().readAllBytes());
                        Post post = session.get(Post.class, Integer.parseInt(id));

                        Post updatedPost = updatePost(post, data);

                        session.merge(updatedPost);
                        transaction.commit();

                        response = objectMapper.writeValueAsString(post);
                        exchange.sendResponseHeaders(201, response.getBytes().length);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        response = "{\"message\": \"Error occured while updating post\"}";
                        exchange.sendResponseHeaders(404, response.getBytes().length);
                    }
                }
            }
            session.close();
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
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
        List<String> tags = List.of(postFields.tags());

        return new Post(title, content, category, tags);
    }

    private Post updatePost(Post post, String data) throws JsonProcessingException {
        PostToSave postFields = objectMapper.readValue(data, PostToSave.class);

        post.setTitle(postFields.title());
        post.setContent(postFields.content());
        post.setCategory(postFields.category());
        post.setTags(List.of(postFields.tags()));

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