package com.lemanty.aipet;

import com.lemanty.aipet.ui.WindowFrameBuffer;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class WindowFrameBufferTest {
    public static void main(String[] args) {
        WindowFrameBuffer surface = new WindowFrameBuffer();
        for (int dpi : new int[]{1,2}) {
            BufferedImage screen = new BufferedImage(80*dpi, 60*dpi, BufferedImage.TYPE_INT_ARGB);
            Graphics2D out = screen.createGraphics(); out.scale(dpi,dpi);
            for(int frame=0;frame<100;frame++) {
                int x=frame%2==0?10:40;
                int before = java.util.Arrays.hashCode(((java.awt.image.DataBufferInt)screen.getRaster().getDataBuffer()).getData());
                surface.paint(out,80,60,g -> {
                    // This simulates Window.paint clearing its background before child painting.
                    // It must affect only the back buffer, not the previous visible frame.
                    g.setComposite(AlphaComposite.Clear); g.fillRect(0,0,80,60);
                    if (java.util.Arrays.hashCode(((java.awt.image.DataBufferInt)screen.getRaster().getDataBuffer()).getData()) != before)
                        throw new AssertionError("background clear reached the visible frame before composition completed");
                    g.setComposite(AlphaComposite.SrcOver); g.setColor(Color.MAGENTA); g.fillRect(x,10,20,30);
                });
                if(frameAlpha(screen,(x+5)*dpi,20*dpi)!=255) throw new AssertionError("completed frame missing character");
                int oldX=x==10?45:15;
                if(frameAlpha(screen,oldX*dpi,20*dpi)!=0) throw new AssertionError("previous frame left trails");
            }
            // Changing frame dimensions and scale must remain valid.
            surface.paint(out,40,30,g -> { g.setColor(Color.BLUE); g.fillRect(0,0,40,30); });
            if(screen.getRGB(5*dpi,5*dpi)!=Color.BLUE.getRGB()) throw new AssertionError("resize failed");
            out.dispose();
        }
        System.out.println("PASS: full-window frame composition, transparent trail removal, HiDPI, resize");
    }
    private static int frameAlpha(BufferedImage image,int x,int y) { return image.getRGB(x,y)>>>24; }
}
