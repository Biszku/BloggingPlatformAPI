package com.biszku.BloggingPlatformAPI;

import com.biszku.BloggingPlatformAPI.Entity.Post;
import com.biszku.BloggingPlatformAPI.Entity.PostErrorResponse;
import com.biszku.BloggingPlatformAPI.Entity.PostToSave;
import com.biszku.BloggingPlatformAPI.Entity.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            String data = new String(exchange.getRequestBody().readAllBytes());
            String response = "";

            URI requestURI = exchange.getRequestURI();
            Map<String, String> queryParams = getQueryParams(requestURI.getQuery());

            switch (requestMethod) {
                case "POST" -> {
                    try {
                        Post post = creationOfPost(data);

                        session.persist(post);
//                        session.merge(post);
//                        session.flush();
                        transaction.commit();

                        Post savedPost = session.get(Post.class, post.getId());
                        System.out.println(savedPost.getId());
                        System.out.println(savedPost.getTags().get(0).getTag());
                        response = objectMapper.writeValueAsString(savedPost);
                        System.out.println(response);
                        exchange.sendResponseHeaders(201, response.getBytes().length);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        PostErrorResponse postErrorResponse = new PostErrorResponse(400,
                                "Invalid body data");
                        response = objectMapper.writeValueAsString(postErrorResponse);
                        exchange.sendResponseHeaders(400, response.getBytes().length);
                    }
                }
                case "PUT" -> {
                    String id = exchange.getRequestURI().toString().split("/")[2];

                    if (id == null) {
                        exchange.sendResponseHeaders(404, -1);
                        break;
                    }

                    try {
                        Post post = session.get(Post.class, Integer.parseInt(id));
                        Post updatedPost = updatePost(post, data);

                        session.merge(updatedPost);
                        transaction.commit();

                        response = objectMapper.writeValueAsString(post);
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        PostErrorResponse postErrorResponse = new PostErrorResponse(400,
                                "Invalid body data");
                        response = objectMapper.writeValueAsString(postErrorResponse);
                        exchange.sendResponseHeaders(400, response.getBytes().length);
                    }
                }
                case "DELETE" -> {
                    String id = exchange.getRequestURI().toString().split("/")[2];

                    try {
                        Post post = session.get(Post.class, Integer.parseInt(id));

                        session.remove(post);
                        transaction.commit();

                        exchange.sendResponseHeaders(204, -1);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        exchange.sendResponseHeaders(404, -1);
                    }
                }
                case "GET" -> {
                    try {
                        List<Post> posts = session.createQuery("SELECT p FROM Post p", Post.class).getResultList();
                        transaction.commit();
                        response = objectMapper.writeValueAsString(posts);

                        exchange.sendResponseHeaders(200, response.getBytes().length);
                    } catch (Exception e) {
                        if (transaction != null) {
                            transaction.rollback();
                        }
                        exchange.sendResponseHeaders(404, -1);
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
        System.out.println(data);
        PostToSave postFields = objectMapper.readValue(data, PostToSave.class);

        String title = postFields.title();
        String content = postFields.content();
        String category = postFields.category();
        List<Tag> tags = postFields.tags().stream().map(Tag::new).collect(Collectors.toList());

        Post post = new Post(title, content, category);
        post.addTags(tags);

        return post;
    }

    private Post updatePost(Post post, String data) throws JsonProcessingException {
        PostToSave postFields = objectMapper.readValue(data, PostToSave.class);

        post.setTitle(postFields.title());
        post.setContent(postFields.content());
        post.setCategory(postFields.category());

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

    private Map<String, String> getQueryParams(String query) {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                queryParams.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
            }
        }
        return queryParams;
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