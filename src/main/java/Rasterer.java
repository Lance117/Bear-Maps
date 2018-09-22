import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private Boolean query_success;
    private static final double ROOT_W = MapServer.ROOT_LRLON - MapServer.ROOT_ULLON;
    private static final double ROOT_H = MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT;
    private static final double ROOT_LONDPP = ROOT_W / MapServer.TILE_SIZE;

    public Rasterer() {
        // YOUR CODE HERE
        query_success = true;
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
        System.out.println(params);
        double lonDPP = (params.get("lrlon") - params.get("ullon")) / params.get("w");
        int depth = getDepth(lonDPP);
        int[] x_range = getXrange(params.get("ullon"), params.get("lrlon"), depth);
        int[] y_range = getYrange(params.get("ullat"), params.get("lrlat"), depth);
        Map<String, Object> results = new HashMap<>();

        // Check for valid query box
        if (params.get("ullon") > params.get("lrlon") || params.get("lrlat") > params.get("ullat") ||
                params.get("lrlon") <= MapServer.ROOT_ULLON || params.get("ullon") >= MapServer.ROOT_LRLON ||
                params.get("lrlat") >= MapServer.ROOT_ULLAT || params.get("ullat") <= MapServer.ROOT_LRLAT) {
            query_success = false;
        }

        // Fill results map with keys and values
        double x_step = ROOT_W / Math.pow(2, depth);
        double y_step = ROOT_H / Math.pow(2, depth);
        String[][] render_grid = getRenderGrid(depth, x_range, y_range);
        results.put("raster_ul_lon", MapServer.ROOT_ULLON + x_range[0] * x_step);
        results.put("raster_lr_lon", MapServer.ROOT_ULLON + (1.0 + x_range[1]) * x_step);
        results.put("raster_ul_lat", MapServer.ROOT_ULLAT - y_range[0] * y_step);
        results.put("raster_lr_lat", MapServer.ROOT_ULLAT - (1.0 + y_range[1]) * y_step);
        results.put("render_grid", render_grid);
        results.put("depth", depth);
        results.put("query_success", query_success);

        return results;
    }

    /**
     * @req_lonDPP - lonDPP of the requested window
     * Return: depth good enough to meet user's request. Max is D7
     */
    private int getDepth(double req_lonDPP) {
        int depth = 0;
        while (ROOT_LONDPP > req_lonDPP) {
            depth++;
            req_lonDPP *= 2;
        }
        return Math.min(depth, 7);
    }

    /**
     * @req_ullon - ullon of query box
     * @req_lrlon - lrlon of query box
     * @depth - requested depth
     * Return: array of start and end horizontal tiles
     */
    private int[] getXrange(double req_ullon, double req_lrlon, int depth) {
       int x;
       int[] res = new int[2];
       double step = ROOT_W / Math.pow(2, depth);
       double lrlon = MapServer.ROOT_ULLON + step;
       for (x = 0; lrlon < req_ullon; x++) {
           lrlon += step;
       }
       res[0] = x;
       for ( ; lrlon < req_lrlon; x++) {
           lrlon += step;
           if (lrlon > MapServer.ROOT_LRLON) {
               break;
           }
       }
        res[1] = x;
        return res;
    }

    private int[] getYrange(double req_ullat, double req_lrlat, int depth) {
        int y;
        int [] res = new int[2];
        double step = ROOT_H / Math.pow(2, depth);
        double lrlat = MapServer.ROOT_ULLAT - step;
        for (y = 0; lrlat > req_ullat; y++) {
            lrlat -= step;
        }
        res[0] = y;
        for ( ; lrlat > req_lrlat; y++) {
            lrlat -= step;
            if (lrlat < MapServer.ROOT_LRLAT) {
                break;
            }
        }
        res[1] = y;
        return res;
    }

    private String[][] getRenderGrid(int depth, int[] x_range, int[] y_range) {
        String[][] res = new String[y_range[1] - y_range[0] + 1][x_range[1] - x_range[0] + 1];
        for (int j = 0; j + y_range[0] <= y_range[1]; j++) {
            for (int i = 0; i + x_range[0] <= x_range[1]; i++) {
                String filename = "d" + Integer.toString(depth) + "_x" + Integer.toString(i + x_range[0])
                        + "_y" + Integer.toString(j + y_range[0]) + ".png";
                res[j][i] = filename;
            }
        }
        return res;
    }
}
