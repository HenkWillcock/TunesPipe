package com.example.tunespipe;

import androidx.annotation.NonNull;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public final class DownloaderImpl extends Downloader {
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";
    public static final String YOUTUBE_RESTRICTED_MODE_COOKIE_KEY =
            "youtube_restricted_mode_key";
    public static final String YOUTUBE_DOMAIN = "youtube.com";

    private final Map<String, String> mCookies;
    private final OkHttpClient client;

    // --- START OF CHANGE ---
    // The constructor now accepts the shared OkHttpClient instance.
    public DownloaderImpl(OkHttpClient client) {
        this.client = client;
        this.mCookies = new HashMap<>();
    }
    // --- END OF CHANGE ---

    public String getCookies(final String url) {
        final String youtubeCookie = url.contains(YOUTUBE_DOMAIN)
                ? getCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY) : null;

        // Recaptcha cookie is always added TODO: not sure if this is necessary
        return Stream.of(youtubeCookie, getCookie("recaptcha_cookies"))
                .filter(Objects::nonNull)
                .flatMap(cookies -> Arrays.stream(cookies.split("; *")))
                .distinct()
                .collect(Collectors.joining("; "));
    }

    public String getCookie(final String key) {
        return mCookies.get(key);
    }

    @Override
    public Response execute(@NonNull final Request request)
            throws IOException, ReCaptchaException {
        final String httpMethod = request.httpMethod();
        final String url = request.url();
        final Map<String, List<String>> headers = request.headers();
        final byte[] dataToSend = request.dataToSend();

        RequestBody requestBody = null;
        if (dataToSend != null) {
            requestBody = RequestBody.create(dataToSend);
        }

        final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .method(httpMethod, requestBody)
                .url(url)
                .addHeader("User-Agent", USER_AGENT);

        final String cookies = getCookies(url);
        if (!cookies.isEmpty()) {
            requestBuilder.addHeader("Cookie", cookies);
        }

        headers.forEach((headerName, headerValueList) -> {
            requestBuilder.removeHeader(headerName);
            headerValueList.forEach(headerValue ->
                    requestBuilder.addHeader(headerName, headerValue));
        });

        try (
                okhttp3.Response response = client.newCall(requestBuilder.build()).execute()
        ) {
            if (response.code() == 429) {
                throw new ReCaptchaException("reCaptcha Challenge requested", url);
            }

            String responseBodyToReturn = null;
            try (ResponseBody body = response.body()) {
                if (body != null) {
                    responseBodyToReturn = body.string();
                }
            }

            final String latestUrl = response.request().url().toString();
            return new Response(
                    response.code(),
                    response.message(),
                    response.headers().toMultimap(),
                    responseBodyToReturn,
                    latestUrl);
        }
    }
}
