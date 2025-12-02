package damose.ui.map;

import java.io.File;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.cache.FileBasedLocalCache;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import damose.config.AppConstants;

/**
 * Factory for creating and configuring JXMapViewer instances.
 */
public final class MapFactory {

    private MapFactory() {
        // Utility class
    }

    public static JXMapViewer createMapViewer() {
        TileFactoryInfo info = new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org");
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);

        File cacheDir = new File(System.getProperty("user.home"), ".jxmapviewer2");
        tileFactory.setLocalCache(new FileBasedLocalCache(cacheDir, false));

        JXMapViewer map = new JXMapViewer();
        map.setTileFactory(tileFactory);
        map.setAddressLocation(new GeoPosition(AppConstants.ROME_LAT, AppConstants.ROME_LON));
        map.setZoom(AppConstants.DEFAULT_ZOOM);

        // Interaction listeners
        map.addMouseWheelListener(e -> map.repaint());
        PanMouseInputListener pan = new PanMouseInputListener(map);
        map.addMouseListener(pan);
        map.addMouseMotionListener(pan);
        map.addMouseWheelListener(new ZoomMouseWheelListenerCursor(map));

        return map;
    }
}

