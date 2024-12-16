package com.example.mborper.breathbetter.activities;

import java.util.List;

class PageContent {
    private final String title;  // Page title
    private final List<String> texts;  // List of texts for this page
    private final List<Integer> icons;  // List of icon resources for this page

    public PageContent(String title, List<String> texts, List<Integer> icons) {
        this.title = title;
        this.texts = texts;
        this.icons = icons;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getTexts() {
        return texts;
    }

    public List<Integer> getIcons() {
        return icons;
    }
}

