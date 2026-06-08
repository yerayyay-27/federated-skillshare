package it.unibo;

public interface ImageReadCallback {
    void onRead(String dataUrl);
    void onError(String message);
}