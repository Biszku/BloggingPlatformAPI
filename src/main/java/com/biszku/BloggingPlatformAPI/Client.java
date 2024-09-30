package com.biszku.BloggingPlatformAPI;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Client {

    public static void main(String[] args) {

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            HttpClient client = HttpClient.newHttpClient();
            PostToSave user = new PostToSave("My first post",
                    "This is my first post",
                    "Technology",
                    new String[]{"Java", "Spring"});

            String jsonString = objectMapper.writeValueAsString(user);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/posts"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
