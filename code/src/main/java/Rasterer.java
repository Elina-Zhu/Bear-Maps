import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private static double[] depthLonDPP = new double[8];
    private static final double INITULLON = MapServer.ROOT_ULLON, INITULLAT = MapServer.ROOT_ULLAT,
            INITLRLON = MapServer.ROOT_LRLON, INITLRLAT = MapServer.ROOT_LRLAT;

    static {
        depthLonDPP[0] = (INITLRLON - INITULLON) / MapServer.TILE_SIZE;
        for (int i = 1; i < 8; i++) {
            depthLonDPP[i] = depthLonDPP[i - 1] / 2;
        }
    }

    public Rasterer() {
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        // System.out.println(params);
        Map<String, Object> results = new HashMap<>();

        double requestedULLon = params.get("ullon");
        double requestedULLat = params.get("ullat");
        double requestedLRLon = params.get("lrlon");
        double requestedLRLat = params.get("lrlat");

        if (requestedLRLat >= INITULLAT || requestedLRLon <= INITULLON
                || requestedULLat <= INITLRLAT || requestedULLon >= INITLRLON
                || requestedULLon >= requestedLRLon || requestedULLat <= requestedLRLat) {
            results.put("depth", 0);
            results.put("render_grid", null);
            results.put("raster_ul_lon", 0);
            results.put("raster_ul_lat", 0);
            results.put("raster_lr_lon", 0);
            results.put("raster_lr_lat", 0);
            results.put("query_success", false);
            return results;
        }

        double requestedLonDPP = (requestedLRLon - requestedULLon) / params.get("w");
        int depth = getDepth(requestedLonDPP);
        results.put("depth", depth);

        double maxLevel = Math.pow(2, depth);
        double xDiff = (INITLRLON - INITULLON) / maxLevel;
        double yDiff = (INITLRLAT - INITULLAT) / maxLevel;

        int xLeft = 0, xRight = 0, yUpper = 0, yLower = 0;
        for (double x = INITULLON; x <= INITLRLON; x += xDiff) {
            if (x <= requestedULLon) {
                xLeft++;
            }
            if (xRight < maxLevel && x <= requestedLRLon) {
                xRight++;
            }
        }
        for (double y = INITULLAT; y >= INITLRLAT; y += yDiff) {
            if (y >= requestedULLat) {
                yUpper++;
            }
            if (yLower < maxLevel && y >= requestedLRLat) {
                yLower++;
            }
        }
        if (xLeft != 0) {
            xLeft--;
        }
        if (yUpper != 0) {
            yUpper--;
        }
        if (xRight != 0) {
            xRight--;
        }
        if (yLower != 0) {
            yLower--;
        }

        String[][] files = new String[yLower - yUpper + 1][xRight - xLeft + 1];
        for (int y = yUpper; y <= yLower; y++) {
            for (int x = xLeft; x <= xRight; x++) {
                files[y - yUpper][x - xLeft] = "d" + depth + "_x" + x + "_y" + y + ".png";
            }
        }

        results.put("render_grid", files);
        results.put("raster_ul_lon", INITULLON + xLeft * xDiff);
        results.put("raster_ul_lat", INITULLAT + yUpper * yDiff);
        results.put("raster_lr_lon", INITULLON + (xRight + 1) * xDiff);
        results.put("raster_lr_lat", INITULLAT + (yLower + 1) * yDiff);
        results.put("query_success", true);
        return results;
    }

    private int getDepth(double requestedLonDPP) {
        int depth = 0;
        while (requestedLonDPP < depthLonDPP[depth]) {
            depth++;
            if (depth == depthLonDPP.length - 1) {
                break;
            }
        }
        return depth;
    }
}
