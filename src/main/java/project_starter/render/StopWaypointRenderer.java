package project_starter.render;

import java.awt.Graphics2D;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointRenderer;

import project_starter.model.StopWaypoint;

public class StopWaypointRenderer implements WaypointRenderer<StopWaypoint> {

    private final Image originalImage;
    private final Map<Integer, Image> sizeCache = new HashMap<>();

    public StopWaypointRenderer() {
        originalImage = new ImageIcon(
            getClass().getResource("/sprites/stop.png")
        ).getImage();
    }

    /** Ottieni immagine scalata con caching per evitare lag */
    private Image getScaled(int size) {
        return sizeCache.computeIfAbsent(size,
            s -> originalImage.getScaledInstance(s, s, Image.SCALE_SMOOTH)
        );
    }

    @Override
    public void paintWaypoint(Graphics2D g, JXMapViewer map, StopWaypoint wp) {

        var point = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());

        int zoom = map.getZoom();

        int size = 50 - (zoom * 2);
        size = Math.max(16, Math.min(size, 48));

        // Immagine scalata MA CACHATA → ZERO LAG
        Image img = getScaled(size);

        g.drawImage(
            img,
            (int) (point.getX() - size / 2),
            (int) (point.getY() - size / 2),
            null
        );
    }
}
