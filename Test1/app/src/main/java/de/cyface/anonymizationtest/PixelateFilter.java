package de.cyface.anonymizationtest;

import android.graphics.Bitmap;
import android.graphics.Color;

public class PixelateFilter extends ImageFilter {

    int pixelSize;
    int[] colors;

    /**
     * @param _pixels
     * @param _width
     * @param _height
     */
    public PixelateFilter(int[] _pixels, int _width, int _height) {
        this(_pixels, _width, _height, 10);
    }

    public PixelateFilter(int[] _pixels, int _width, int _height, int _pixelSize) {
        this(_pixels, _width, _height, _pixelSize, null);
    }

    public PixelateFilter(int[] _pixels, int _width, int _height, int _pixelSize, int[] _colors) {
        super(_pixels, _width, _height);
        pixelSize = _pixelSize;
        colors = _colors;
    }

    /* (non-Javadoc)
     * @see imageProcessing.ImageFilter#procImage()
     */
    @Override
    public int[] procImage() {
        for (int i = 0; i < width; i += pixelSize) {
            for (int j = 0; j < height; j += pixelSize) {
                int rectColor = getRectColor(i, j);
                fillRectColor(rectColor, i, j);
            }
        }
        return pixels;
    }

    private int getRectColor(int col, int row) {
        int r = 0, g = 0, b = 0;
        int sum = 0;
        for (int x = col; x < col + pixelSize; x++) {
            for (int y = row; y < row + pixelSize; y++) {
                int index = x + y * width;
                if (index < width * height) {
                    int color = pixels[x + y * width];
                    r += Color.red(color);
                    g += Color.green(color);
                    b += Color.blue(color);
                }

            }
        }
        sum = pixelSize * pixelSize;
        int newColor = Color.rgb(r / sum, g / sum, b / sum);
        if (colors != null)
            newColor = getBestMatch(newColor);
        return newColor;
    }

    private int getBestMatch(int color) {
        double diff = Double.MAX_VALUE;
        int res = color;
        for (int c : colors) {
            double currDiff = colorDistance(color, c);
            if (currDiff < diff) {
                diff = currDiff;
                res = c;
            }
        }
        return res;
    }

    private void fillRectColor(int color, int col, int row) {
        for (int x = col; x < col + pixelSize; x++) {
            for (int y = row; y < row + pixelSize; y++) {
                int index = x + y * width;
                if (x < width && y < height && index < width * height) {
                    pixels[x + y * width] = color;
                }

            }
        }
    }

    public static final Bitmap changeToPixelate(Bitmap bitmap, int pixelSize, int [] colors) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        PixelateFilter pixelateFilter = new PixelateFilter(pixels, width, height, pixelSize, colors);

        int[] returnPixels = pixelateFilter.procImage();
        Bitmap returnBitmap = Bitmap.createBitmap(returnPixels, width, height, Bitmap.Config.ARGB_8888);

        return returnBitmap;

    }
}
