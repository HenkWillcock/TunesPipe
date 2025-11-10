package org.schabi.newpipe.extractor;

import org.schabi.newpipe.extractor.stream.Description;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class MetaInfo implements Serializable {

    private String title = "";
    private Description content;
    private List<URL> urls = new ArrayList<>();
    private List<String> urlTexts = new ArrayList<>();

    public MetaInfo(@NotNull final String title,
                    @NotNull final Description content,
                    @NotNull final List<URL> urls,
                    @NotNull final List<String> urlTexts) {
        this.title = title;
        this.content = content;
        this.urls = urls;
        this.urlTexts = urlTexts;
    }

    public MetaInfo() {
    }

    /**
     * @return Title of the info. Can be empty.
     */
    @NotNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NotNull final String title) {
        this.title = title;
    }

    @NotNull
    public Description getContent() {
        return content;
    }

    public void setContent(@NotNull final Description content) {
        this.content = content;
    }

    @NotNull
    public List<URL> getUrls() {
        return urls;
    }

    public void setUrls(@NotNull final List<URL> urls) {
        this.urls = urls;
    }

    public void addUrl(@NotNull final URL url) {
        urls.add(url);
    }

    @NotNull
    public List<String> getUrlTexts() {
        return urlTexts;
    }

    public void setUrlTexts(@NotNull final List<String> urlTexts) {
        this.urlTexts = urlTexts;
    }

    public void addUrlText(@NotNull final String urlText) {
        urlTexts.add(urlText);
    }
}
