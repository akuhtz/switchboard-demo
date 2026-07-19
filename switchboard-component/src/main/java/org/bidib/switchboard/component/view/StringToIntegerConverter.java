package org.bidib.switchboard.component.view;

import java.text.DecimalFormat;
import java.text.Format;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.binding.value.BindingConverter;
import com.jgoodies.common.base.Preconditions;

/**
 * Converts Values to Strings and vice-versa using a given Format.
 */
public final class StringToIntegerConverter implements BindingConverter<Integer, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringToIntegerConverter.class);

    /**
     * Holds the {@code Format} used to format and parse.
     */
    private final Format format;

    private Integer defaultValue;

    // Instance Creation **************************************************

    /**
     * Constructs a {@code StringConverter} on the given subject using the specified {@code Format}.
     * 
     * @param format
     *            the {@code Format} used to format and parse
     * @throws NullPointerException
     *             if the subject or the format is null.
     */
    public StringToIntegerConverter(Format format) {
        this.format = Preconditions.checkNotNull(format, "The format must not be null.");
    }

    /**
     * Constructs a {@code StringConverter} on the given subject using the default DecimalFormat.
     */
    public StringToIntegerConverter() {
        this(new DecimalFormat("#"));
    }

    // Implementing Abstract Behavior *************************************

    /**
     * Formats the source value and returns a String representation.
     * 
     * @param sourceValue
     *            the source value
     * @return the formatted sourceValue
     */
    @Override
    public String targetValue(Integer sourceValue) {
        if (sourceValue != null) {
            return format.format(sourceValue.intValue());
        }

        if (this.defaultValue != null) {
            return format.format(defaultValue.intValue());
        }
        return null;
    }

    /**
     * Parses the given String encoding and sets it as the subject's new value. Silently catches {@code ParseException}.
     * 
     * @param targetValue
     *            the value to be converted and set as new subject value
     */
    @Override
    public Integer sourceValue(String targetValue) {
        try {
            if (StringUtils.isNotBlank(targetValue)) {
                return Integer.valueOf(targetValue);
            }
        }
        catch (NumberFormatException e) {
            LOGGER.warn("Cannot parse the target value {}", targetValue);
        }
        return this.defaultValue;
    }
}
