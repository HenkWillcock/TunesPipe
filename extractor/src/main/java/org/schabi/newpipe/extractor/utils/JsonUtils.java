package org.schabi.newpipe.extractor.utils;

import org.schabi.newpipe.extractor.nanojson.JsonArray;
import org.schabi.newpipe.extractor.nanojson.JsonObject;
import org.schabi.newpipe.extractor.nanojson.JsonParser;
import org.schabi.newpipe.extractor.nanojson.JsonParserException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class JsonUtils {
    private JsonUtils() {
    }

    @NotNull
    public static Object getValue(@NotNull final JsonObject object,
                                  @NotNull final String path) throws ParsingException {

        final List<String> keys = Arrays.asList(path.split("\\."));
        final JsonObject parentObject = getObject(object, keys.subList(0, keys.size() - 1));
        if (parentObject == null) {
            throw new ParsingException("Unable to get " + path);
        }

        final Object result = parentObject.get(keys.get(keys.size() - 1));
        if (result == null) {
            throw new ParsingException("Unable to get " + path);
        }
        return result;
    }

    private static <T> T getInstanceOf(@NotNull final JsonObject object,
                                       @NotNull final String path,
                                       @NotNull final Class<T> klass) throws ParsingException {
        final Object value = getValue(object, path);
        if (klass.isInstance(value)) {
            return klass.cast(value);
        } else {
            throw new ParsingException("Wrong data type at path " + path);
        }
    }

    @NotNull
    public static String getString(@NotNull final JsonObject object, @NotNull final String path)
            throws ParsingException {
        return getInstanceOf(object, path, String.class);
    }

    @NotNull
    public static Boolean getBoolean(@NotNull final JsonObject object,
                                     @NotNull final String path) throws ParsingException {
        return getInstanceOf(object, path, Boolean.class);
    }

    @NotNull
    public static Number getNumber(@NotNull final JsonObject object,
                                   @NotNull final String path)
            throws ParsingException {
        return getInstanceOf(object, path, Number.class);
    }

    @NotNull
    public static JsonObject getObject(@NotNull final JsonObject object,
                                       @NotNull final String path) throws ParsingException {
        return getInstanceOf(object, path, JsonObject.class);
    }

    @NotNull
    public static JsonArray getArray(@NotNull final JsonObject object, @NotNull final String path)
            throws ParsingException {
        return getInstanceOf(object, path, JsonArray.class);
    }

    @NotNull
    public static List<Object> getValues(@NotNull final JsonArray array, @NotNull final String path)
            throws ParsingException {

        final List<Object> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            final JsonObject obj = array.getObject(i);
            result.add(getValue(obj, path));
        }
        return result;
    }

    @Nullable
    private static JsonObject getObject(@NotNull final JsonObject object,
                                        @NotNull final List<String> keys) {
        JsonObject result = object;
        for (final String key : keys) {
            result = result.getObject(key);
            if (result == null) {
                break;
            }
        }
        return result;
    }

    public static JsonArray toJsonArray(final String responseBody) throws ParsingException {
        try {
            return JsonParser.array().from(responseBody);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse JSON", e);
        }
    }

    public static JsonObject toJsonObject(final String responseBody) throws ParsingException {
        try {
            return JsonParser.object().from(responseBody);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse JSON", e);
        }
    }

    /**
     * <p>Get an attribute of a web page as JSON
     *
     * <p>Originally a part of bandcampDirect.</p>
     * <p>Example HTML:</p>
     * <pre>
     * {@code
     * <p data-town="{&quot;name&quot;:&quot;Mycenae&quot;,&quot;country&quot;:&quot;Greece&quot;}">
     * This is Sparta!</p>
     * }
     * </pre>
     * <p>Calling this function to get the attribute <code>data-town</code> returns the JsonObject
     * for</p>
     * <pre>
     * {@code
     *   {
     *     "name": "Mycenae",
     *     "country": "Greece"
     *   }
     * }
     * </pre>
     *
     * @param html     The HTML where the JSON we're looking for is stored inside a
     *                 variable inside some JavaScript block
     * @param variable Name of the variable
     * @return The JsonObject stored in the variable with this name
     */
    public static JsonObject getJsonData(final String html, final String variable)
            throws JsonParserException, ArrayIndexOutOfBoundsException {
        final Document document = Jsoup.parse(html);
        final String json = document.getElementsByAttribute(variable).attr(variable);
        return JsonParser.object().from(json);
    }

    public static List<String> getStringListFromJsonArray(@NotNull final JsonArray array) {
        return array.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }
}
