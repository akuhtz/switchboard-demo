package org.bidib.switchboard.view;

import java.text.DecimalFormat;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class InputValidationDocument extends PlainDocument {
    /**
     * serial version uid
     */
    private static final long serialVersionUID = 1L;

    /**
     * maximum character count
     */
    private int limit = -1;

    /**
     * accepted chars, null means accept everything
     */
    protected String acceptedChars;

    /**
     * accepted chars that are used for validation
     */
    protected String validateAcceptedChars;

    /**
     * numeric string constant
     */
    public static final String BINARY = "01";

    /**
     * numeric string constant
     */
    public static final String NUMERIC = "0123456789";

    /**
     * numeric hex string constant
     */
    public static final String NUMERIC_HEX = "0123456789ABCDEF";

    /**
     * float string constant
     */
    public static final String FLOAT =
        "0123456789" + new DecimalFormat().getDecimalFormatSymbols().getDecimalSeparator();

    /**
     * signed numeric string constant
     */
    public static final String SIGNED_NUMERIC = "-0123456789";

    /**
     * alpha numeric and space
     */
    public static final String ALPHANUM_AND_SPACE = "abcdefghijklmnopqrstuvwxyz 0123456789";

    /**
     * ignore case
     */
    public boolean ignoreCase;

    /**
     * convert inserted characters to uppercase
     */
    private boolean upperCase;

    /**
     * Creates a new instance of InputValidationDocument unlimited and accept all input
     */
    public InputValidationDocument() {
    }

    /**
     * Creates a new instance of InputValidationDocument
     * 
     * @param limit
     *            character limit
     */
    public InputValidationDocument(int limit) {
        this(limit, null);
    }

    /**
     * Creates a new instance of InputValidationDocument
     * 
     * @param limit
     *            character limit
     * @param acceptedChars
     *            accepted chars
     */
    public InputValidationDocument(int limit, String acceptedChars) {
        super();
        this.limit = limit;
        this.acceptedChars = acceptedChars;

        setIgnoreCase(true);
    }

    /**
     * Creates a new instance of InputValidationDocument
     * 
     * @param acceptedChars
     *            accepted chars
     */
    public InputValidationDocument(String acceptedChars) {
        this(-1, acceptedChars);
    }

    /**
     * set the ignore case flag
     */
    public void setIgnoreCase(boolean ignoreCase) {

        this.ignoreCase = ignoreCase;
        if (acceptedChars != null && ignoreCase) {
            validateAcceptedChars = acceptedChars.toUpperCase();
        }
        else {
            validateAcceptedChars = acceptedChars;
        }

    }

    /**
     * @param limit
     *            the new character limit to set
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @param acceptedChars
     *            the new acceptedChars to set
     */
    public void setAcceptedChars(String acceptedChars) {
        this.acceptedChars = acceptedChars;
        setIgnoreCase(ignoreCase);
    }

    public void setUpperCase(boolean upperCase) {
        this.upperCase = upperCase;
    }

    public InputValidationDocument withUpperCase(boolean upperCase) {
        this.upperCase = upperCase;
        return this;
    }

    /**
     * Inserts a string.
     */
    @Override
    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
        if (str == null) {
            return;
        }

        if ((limit == -1) || (getLength() + str.length()) <= limit) {

            // check if we have a limited set of characters to accept
            if (validateAcceptedChars != null) {

                // if we ignore the case we convert the input to uppercase
                String checkNewInput = str;
                if (ignoreCase) {
                    checkNewInput = str.toUpperCase();
                }

                for (int i = 0; i < checkNewInput.length(); i++) {
                    if (validateAcceptedChars.indexOf(String.valueOf(checkNewInput.charAt(i))) == -1) {
                        return;
                    }
                }
            }

            super.insertString(offset, upperCase ? str.toUpperCase() : str, attr);
        }
    }

}
