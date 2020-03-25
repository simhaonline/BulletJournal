package com.bulletjournal.controller;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class Content {

    @NotNull
    private Long id;

    @NotBlank
    private String text;

    public Content() {
    }

    public Content(@NotNull Long id, @NotBlank String text) {
        this.id = id;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
