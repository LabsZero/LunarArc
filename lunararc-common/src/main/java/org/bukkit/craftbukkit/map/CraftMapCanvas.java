package org.bukkit.craftbukkit.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapPalette;

public final class CraftMapCanvas implements MapCanvas {
    private final byte[] buffer = new byte[128 * 128];
    private final CraftMapView view;
    private byte[] base = new byte[128 * 128];
    private MapCursorCollection cursors = new MapCursorCollection();

    CraftMapCanvas(CraftMapView view) {
        this.view = view;
        Arrays.fill(this.buffer, (byte) -1);
    }

    @Override
    public CraftMapView getMapView() {
        return this.view;
    }

    @Override
    public MapCursorCollection getCursors() {
        return this.cursors;
    }

    @Override
    public void setCursors(MapCursorCollection cursors) {
        this.cursors = Objects.requireNonNull(cursors, "cursors");
    }

    @Override
    public void setPixelColor(int x, int y, Color color) {
        this.setPixel(x, y, color == null ? (byte) -1 : MapPalette.matchColor(color));
    }

    @Override
    public Color getPixelColor(int x, int y) {
        byte color = this.getPixel(x, y);
        return color == -1 ? null : MapPalette.getColor(color);
    }

    @Override
    public Color getBasePixelColor(int x, int y) {
        return MapPalette.getColor(this.getBasePixel(x, y));
    }

    @Override
    @Deprecated
    public void setPixel(int x, int y, byte color) {
        if (x < 0 || y < 0 || x >= 128 || y >= 128) return;
        this.buffer[y * 128 + x] = color;
    }

    @Override
    @Deprecated
    public byte getPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= 128 || y >= 128) return 0;
        return this.buffer[y * 128 + x];
    }

    @Override
    @Deprecated
    public byte getBasePixel(int x, int y) {
        if (x < 0 || y < 0 || x >= 128 || y >= 128) return 0;
        return this.base[y * 128 + x];
    }

    void setBase(byte[] base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    byte[] getBuffer() {
        return this.buffer;
    }

    @Override
    public void drawImage(int x, int y, Image image) {
        Objects.requireNonNull(image, "image");
        int sourceWidth = image.getWidth(null);
        int sourceHeight = image.getHeight(null);
        int sourceX = Math.max(-x, 0);
        int sourceY = Math.max(-y, 0);
        int destX = Math.max(x, 0);
        int destY = Math.max(y, 0);
        int width = Math.min(sourceWidth - sourceX, 128 - destX);
        int height = Math.min(sourceHeight - sourceY, 128 - destY);
        if (width <= 0 || height <= 0) return;

        BufferedImage rendered;
        if (image instanceof BufferedImage buffered) {
            rendered = sourceX == 0 && sourceY == 0 && width == sourceWidth && height == sourceHeight
                    ? buffered
                    : buffered.getSubimage(sourceX, sourceY, width, height);
        } else {
            rendered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = rendered.createGraphics();
            try {
                graphics.drawImage(image, -sourceX, -sourceY, null);
            } finally {
                graphics.dispose();
            }
        }

        byte[] colors = MapPalette.imageToBytes(rendered);
        for (int row = 0; row < height; row++) {
            System.arraycopy(colors, row * width, this.buffer, (destY + row) * 128 + destX, width);
        }
    }

    @Override
    public void drawText(int x, int y, MapFont font, String text) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(text, "text");
        if (!font.isValid(text)) throw new IllegalArgumentException("text contains invalid characters");
        int startX = x;
        byte color = MapPalette.DARK_GRAY;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                x = startX;
                y += font.getHeight() + 1;
                continue;
            }
            if (character == '§') {
                int end = text.indexOf(';', i);
                if (end < 0) throw new IllegalArgumentException("unterminated map color sequence");
                try {
                    color = Byte.parseByte(text.substring(i + 1, end));
                    i = end;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            MapFont.CharacterSprite sprite = font.getChar(character);
            for (int row = 0; row < font.getHeight(); row++) {
                for (int column = 0; column < sprite.getWidth(); column++) {
                    if (sprite.get(row, column)) this.setPixel(x + column, y + row, color);
                }
            }
            x += sprite.getWidth() + 1;
        }
    }
}
