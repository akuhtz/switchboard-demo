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
public final class StringToUnsignedLongConverter implements BindingConverter<Number, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringToUnsignedLongConverter.class);

    /**
     * Holds the {@code Format} used to format and parse.
     */
    private final Format format;

    // Instance Creation **************************************************

    /**
     * Constructs a {@code StringConverter} on the given subject using the specified {@code Format}.
     * 
     * @param format
     *            the {@code Format} used to format and parse
     * @throws NullPointerException
     *             if the subject or the format is null.
     */
    public StringToUnsignedLongConverter(Format format) {
        this.format = Preconditions.checkNotNull(format, "The format must not be null.");
    }

    /**
     * Constructs a {@code StringConverter} on the given subject using the default DecimalFormat.
     */
    public StringToUnsignedLongConverter() {
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
    public String targetValue(Number sourceValue) {
        if (sourceValue != null) {
            // int value = ((Number) sourceValue).intValue();
            // long lValue = ((long) value) & 0xffffffffL;
            long value = ((Number) sourceValue).longValue();
            long lValue = ((long) value) & 0xffffffffL;
            return format.format(lValue);
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
    public Number sourceValue(String targetValue) {
        try {
            if (StringUtils.isNotBlank((String) targetValue)) {
                // return format.parseObject((String) targetValue);
                return Long.valueOf((String) targetValue);
            }
        }
        catch (NumberFormatException e) {
            LOGGER.warn("Cannot parse the target value {}", targetValue);
        }
        return null;
    }
}
