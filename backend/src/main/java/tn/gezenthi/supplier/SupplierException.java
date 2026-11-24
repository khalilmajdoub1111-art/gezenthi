package tn.gezenthi.supplier;

/** A supplier call failed. The message is safe to show to an admin (never contains credentials). */
public class SupplierException extends RuntimeException {

    public SupplierException(String message) {
        super(message);
    }

    public SupplierException(String message, Throwable cause) {
        super(message, cause);
    }
}
