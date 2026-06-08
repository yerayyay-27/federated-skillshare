package it.unibo;

import com.google.gwt.dom.client.Element;

public class ImageReader {

    // Reads the first file of a file input as a base64 data URL,
    // using the browser's FileReader API.
    public static native void readAsDataUrl(Element fileInput, ImageReadCallback callback) /*-{
        var files = fileInput.files;
        if (!files || files.length === 0) {
            callback.@it.unibo.ImageReadCallback::onError(Ljava/lang/String;)("No file selected");
            return;
        }
        var file = files[0];
        if (file.type.indexOf("image/") !== 0) {
            callback.@it.unibo.ImageReadCallback::onError(Ljava/lang/String;)("Please select an image file");
            return;
        }
        var reader = new FileReader();
        reader.onload = function(e) {
            callback.@it.unibo.ImageReadCallback::onRead(Ljava/lang/String;)(e.target.result);
        };
        reader.onerror = function(e) {
            callback.@it.unibo.ImageReadCallback::onError(Ljava/lang/String;)("Could not read the file");
        };
        reader.readAsDataURL(file);
    }-*/;
}