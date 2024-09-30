package com.biszku.BloggingPlatformAPI;

public class FullPost {
    private Post post;
    private Tag[] tags;

    public FullPost(Post post, Tag[] tags) {
        this.post = post;
        this.tags = tags;
    }
}
