package org.schabi.newpipe.extractor;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.localization.TimeAgoParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public abstract class Extractor {
    /**
     * {@link StreamingService} currently related to this extractor.<br>
     * Useful for getting other things from a service (like the url handlers for
     * cleaning/accepting/get id from urls).
     */
    private final StreamingService service;
    private final LinkHandler linkHandler;

    @Nullable
    private Localization forcedLocalization = null;
    @Nullable
    private ContentCountry forcedContentCountry = null;

    private boolean pageFetched = false;
    // called like this to prevent checkstyle errors about "hiding a field"
    private final Downloader downloader;

    protected Extractor(final StreamingService service, final LinkHandler linkHandler) {
        this.service = Objects.requireNonNull(service, "service is null");
        this.linkHandler = Objects.requireNonNull(linkHandler, "LinkHandler is null");
        this.downloader = Objects.requireNonNull(NewPipe.getDownloader(), "downloader is null");
    }

    /**
     * @return The {@link LinkHandler} of the current extractor object (e.g. a ChannelExtractor
     *         should return a channel url handler).
     */
    @NotNull
    public LinkHandler getLinkHandler() {
        return linkHandler;
    }

    /**
     * Fetch the current page.
     *
     * @throws IOException         if the page can not be loaded
     * @throws ExtractionException if the pages content is not understood
     */
    public void fetchPage() throws IOException, ExtractionException {
        if (pageFetched) {
            return;
        }
        onFetchPage(downloader);
        pageFetched = true;
    }

    protected void assertPageFetched() {
        if (!pageFetched) {
            throw new IllegalStateException("Page is not fetched. Make sure you call fetchPage()");
        }
    }

    protected boolean isPageFetched() {
        return pageFetched;
    }

    /**
     * Fetch the current page.
     *
     * @param downloader the downloader to use
     * @throws IOException         if the page can not be loaded
     * @throws ExtractionException if the pages content is not understood
     */
    @SuppressWarnings("HiddenField")
    public abstract void onFetchPage(@NotNull Downloader downloader)
            throws IOException, ExtractionException;

    @NotNull
    public String getId() throws ParsingException {
        return linkHandler.getId();
    }

    /**
     * Get the name
     *
     * @return the name
     * @throws ParsingException if the name cannot be extracted
     */
    @NotNull
    public abstract String getName() throws ParsingException;

    @NotNull
    public String getOriginalUrl() throws ParsingException {
        return linkHandler.getOriginalUrl();
    }

    @NotNull
    public String getUrl() throws ParsingException {
        return linkHandler.getUrl();
    }

    @NotNull
    public String getBaseUrl() throws ParsingException {
        return linkHandler.getBaseUrl();
    }

    @NotNull
    public StreamingService getService() {
        return service;
    }

    public int getServiceId() {
        return service.getServiceId();
    }

    public Downloader getDownloader() {
        return downloader;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Localization
    //////////////////////////////////////////////////////////////////////////*/

    public void forceLocalization(final Localization localization) {
        this.forcedLocalization = localization;
    }

    public void forceContentCountry(final ContentCountry contentCountry) {
        this.forcedContentCountry = contentCountry;
    }

    @NotNull
    public Localization getExtractorLocalization() {
        return forcedLocalization == null ? getService().getLocalization() : forcedLocalization;
    }

    @NotNull
    public ContentCountry getExtractorContentCountry() {
        return forcedContentCountry == null ? getService().getContentCountry()
                : forcedContentCountry;
    }

    @NotNull
    public TimeAgoParser getTimeAgoParser() {
        return getService().getTimeAgoParser(getExtractorLocalization());
    }
}
