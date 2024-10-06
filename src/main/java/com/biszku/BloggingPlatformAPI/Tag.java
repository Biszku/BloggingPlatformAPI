package com.biszku.BloggingPlatformAPI;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TagID")
    private int id;

    @Column(name = "Val")
    private String val;

    @ManyToOne (cascade = CascadeType.ALL)
    @JoinColumn(name = "PostID")
    @JsonBackReference
    private Post post;

    public Tag() {
    }

    public Tag(String val) {
        this.val = val;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @JsonValue
    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }
}
