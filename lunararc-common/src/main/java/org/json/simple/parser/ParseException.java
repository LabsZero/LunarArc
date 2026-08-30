package org.json.simple.parser;

public class ParseException extends Exception {
    public static final int ERROR_UNEXPECTED_CHAR = 0;
    public static final int ERROR_UNEXPECTED_TOKEN = 1;
    public static final int ERROR_UNEXPECTED_EXCEPTION = 2;

    private final int position;
    private final int errorType;
    private final Object unexpectedObject;

    public ParseException(int position, int errorType, Object unexpectedObject) {
        super("Unexpected " + unexpectedObject + " at position " + position);
        this.position = position;
        this.errorType = errorType;
        this.unexpectedObject = unexpectedObject;
    }

    public int getPosition() { return position; }
    public int getErrorType() { return errorType; }
    public Object getUnexpectedObject() { return unexpectedObject; }
}
