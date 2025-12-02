package damose.ui.render;

import java.awt.Graphics2D;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.WaypointRenderer;

import damose.model.BusWaypoint;

/**
 * Renderer for bus waypoints on the map.
 */
public class BusWaypointRenderer implements WaypointRenderer<BusWaypoint> {

    private final Image originalImage;
    private final Map<Integer, Image> sizeCache = new HashMap<>();

    public BusWaypointRenderer() {
        originalImage = new ImageIcon(
            getClass().getResource("/sprites/bus.png")
        ).getImage();
    }

    private Image getScaled(int size) {
        return sizeCache.computeIfAbsent(size,
            s -> originalImage.getScaledInstance(s, s, Image.SCALE_SMOOTH)
        );
    }

    @Override
    public void paintWaypoint(Graphics2D g, JXMapViewer map, BusWaypoint wp) {
        var point = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());

        int zoom = map.getZoom();
        int size = 55 - (zoom * 2);
        size = Math.max(18, Math.min(size, 52));

        Image img = getScaled(size);

        g.drawImage(
            img,
            (int) (point.getX() - size / 2),
            (int) (point.getY() - size / 2),
            null
        );
    }
}

