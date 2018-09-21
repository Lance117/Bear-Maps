import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private String[][] render_grid;
    private int depth;
    private Boolean query_success;

    public Rasterer() {
        // YOUR CODE HERE
        render_grid = new String[][]{
                {"d7_x84_y28.png", "d7_x85_y28.png", "d7_x86_y28.png"},
                {"d7_x84_y29.png", "d7_x85_y29.png", "d7_x86_y29.png"},
                {"d7_x84_y30.png", "d7_x85_y30.png", "d7_x86_y30.png"}
        };
        depth = 7;
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
        Map<String, Object> results = new HashMap<>();
        for (Map.Entry<String, Double> e : params.entrySet()) {
            String k = e.getKey();
            if (k.length() > 1) {
                String k_suffix = k.substring(0, 2).concat("_").concat(k.substring(2));
                results.put("raster_".concat(k_suffix), e.getValue());
            }
        }
        if (params.get("ullon") > params.get("lrlon") || params.get("lrlat") > params.get("ullat") ||
                params.get("ullon") < MapServer.ROOT_ULLON || params.get("lrlon") > MapServer.ROOT_LRLON ||
                params.get("ullat") > MapServer.ROOT_ULLAT || params.get("lrlat") < MapServer.ROOT_LRLAT) {
            query_success = false;
        }
        results.put("render_grid", render_grid);
        results.put("depth", depth);
        results.put("query_success", query_success);
        return results;
    }

}
