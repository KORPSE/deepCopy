package io.ics.deepcopy;

public class CopyException extends RuntimeException {

    private Exception reason;

    public CopyException(Exception reason) {
        super(reason);
        this.reason = reason;
    }

    public String getMessage() {
        return "Object copying has failed due to "
                + reason.getClass().getCanonicalName() + ": "
                + reason.getMessage();
    }
}
