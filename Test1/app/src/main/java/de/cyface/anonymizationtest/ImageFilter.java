package de.cyface.anonymizationtest;

import android.graphics.Color;

public abstract class ImageFilter {

    protected int [] pixels;
    protected int width;
    protected int height;

    public ImageFilter (int [] _pixels, int _width,int _height) {
        setPixels(_pixels,_width,_height);
    }


    public void setPixels(int [] _pixels, int _width,int _height) {
        pixels = _pixels;
        width = _width;
        height = _height;
    }

    /**
     * a weighted Euclidean distance in RGB space
     * @param c1
     * @param c2
     * @return
     */
    public double colorDistance(int c1, int c2)
    {
        int red1 = Color.red(c1);
        int red2 = Color.red(c2);
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = Color.green(c1) - Color.green(c2);
        int b = Color.blue(c1) - Color.blue(c2);
        return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
    }

    public abstract int[]  procImage();
}
