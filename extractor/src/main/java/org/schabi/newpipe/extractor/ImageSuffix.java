package org.schabi.newpipe.extractor;

import org.schabi.newpipe.extractor.Image.ResolutionLevel;

import org.jetbrains.annotations.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Serializable class representing a suffix (which may include its format extension, such as
 * {@code .jpg}) which needs to be added to get an image/thumbnail URL with its corresponding
 * height, width and estimated resolution level.
 *
 * <p>
 * This class is used to construct {@link org.schabi.newpipe.extractor.Image Image}
 * instances from a single base URL/path, in order to get all or most image resolutions provided,
 * depending of the service and the resolutions provided.
 * </p>
 *
 * <p>
 * Note that this class is not intended to be used externally and so should only be used when
 * interfacing with the extractor.
 * </p>
 */
public final class ImageSuffix implements Serializable {
    @NotNull
    private final String suffix;
    private final int height;
    private final int width;
    @NotNull
    private final ResolutionLevel resolutionLevel;

    /**
     * Create a new {@link ImageSuffix} instance.
     *
     * @param suffix                   the suffix string
     * @param height                   the height corresponding to the image suffix
     * @param width                    the width corresponding to the image suffix
     * @param estimatedResolutionLevel the {@link ResolutionLevel} of the image suffix, which must
     *                                 not be null
     * @throws NullPointerException if {@code estimatedResolutionLevel} is {@code null}
     */
    public ImageSuffix(@NotNull final String suffix,
                       final int height,
                       final int width,
                       @NotNull final ResolutionLevel estimatedResolutionLevel)
            throws NullPointerException {
        this.suffix = suffix;
        this.height = height;
        this.width = width;
        this.resolutionLevel = Objects.requireNonNull(estimatedResolutionLevel,
                "estimatedResolutionLevel is null");
    }

    /**
     * @return the suffix which needs to be appended to get the full image URL
     */
    @NotNull
    public String getSuffix() {
        return suffix;
    }

    /**
     * @return the height corresponding to the image suffix, which may be unknown
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the width corresponding to the image suffix, which may be unknown
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the estimated {@link ResolutionLevel} of the suffix, which is never null.
     */
    @NotNull
    public ResolutionLevel getResolutionLevel() {
        return resolutionLevel;
    }

    /**
     * Get a string representation of this {@link ImageSuffix} instance.
     *
     * <p>
     * The representation will be in the following format, where {@code suffix}, {@code height},
     * {@code width} and {@code resolutionLevel} represent the corresponding properties:
     * <br>
     * <br>
     * {@code ImageSuffix {url=url, height=height, width=width, resolutionLevel=resolutionLevel}'}
     * </p>
     *
     * @return a string representation of this {@link ImageSuffix} instance
     */
    @NotNull
    @Override
    public String toString() {
        return "ImageSuffix {" + "suffix=" + suffix + ", height=" + height + ", width="
                + width + ", resolutionLevel=" + resolutionLevel + "}";
    }
}
