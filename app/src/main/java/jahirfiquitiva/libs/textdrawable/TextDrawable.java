package jahirfiquitiva.libs.textdrawable;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class TextDrawable {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private int width = 200;
        private int height = 200;
        private int color = Color.GRAY;
        
        public Builder square() { return this; }
        
        public Builder size(int w, int h) {
            this.width = w;
            this.height = h;
            return this;
        }
        
        public Builder color(int c) {
            this.color = c;
            return this;
        }
        
        public Builder color(int[] colors) {
            return this;
        }
        
        public TextDrawable buildRect(String text) {
            return new TextDrawable(width, height, color);
        }
        
        public TextDrawable buildRound(String text) {
            return new TextDrawable(width, height, color);
        }
        
        public TextDrawable buildRoundRect(String text, int radius) {
            return new TextDrawable(width, height, color);
        }
    }
    
    private final int width;
    private final int height;
    private final int color;
    
    private TextDrawable(int w, int h, int c) {
        this.width = w;
        this.height = h;
        this.color = c;
    }
    
    public int getColor() { return color; }
    
    public GradientDrawable toShapeDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        return d;
    }
}
