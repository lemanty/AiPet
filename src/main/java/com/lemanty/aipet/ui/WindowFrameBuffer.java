package com.lemanty.aipet.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/** Buffer the entire window, including Window.paint's transparent background clear. */
public final class WindowFrameBuffer {
    private BufferedImage frame;

    public void paint(Graphics graphics, int width, int height, Consumer<Graphics2D> scene) {
        if (width <= 0 || height <= 0) return;
        Graphics2D destination = (Graphics2D) graphics.create();
        try {
            double scaleX = Math.max(1, Math.abs(destination.getTransform().getScaleX()));
            double scaleY = Math.max(1, Math.abs(destination.getTransform().getScaleY()));
            int pixelWidth = (int)Math.ceil(width * scaleX);
            int pixelHeight = (int)Math.ceil(height * scaleY);
            if (frame == null || frame.getWidth() != pixelWidth || frame.getHeight() != pixelHeight)
                frame = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D back = frame.createGraphics();
            try {
                back.setComposite(AlphaComposite.Clear);
                back.fillRect(0, 0, pixelWidth, pixelHeight);
                back.setComposite(AlphaComposite.SrcOver);
                back.scale(scaleX, scaleY);
                scene.accept(back);
            } finally { back.dispose(); }
            // No separate clear reaches the screen. SRC replaces the old frame, including
            // transparent pixels vacated by a moving character, in this single draw operation.
            destination.setComposite(AlphaComposite.Src);
            destination.drawImage(frame, 0, 0, width, height, null);
        } finally { destination.dispose(); }
    }
}
