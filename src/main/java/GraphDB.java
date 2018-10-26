import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    private final Map<Long, Location> locations = new HashMap<>();
    private final Map<Long, Node> nodes = new HashMap<>();
    private final Map<String, List<Long>> names = new HashMap<>();
    private final TrieST<Long> st = new TrieST<>();

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        // TODO: Your code here.
        Iterator<Map.Entry<Long, Node>> nodes_iterator = nodes.entrySet().iterator();
        while (nodes_iterator.hasNext()) {
            Map.Entry<Long, Node> item = nodes_iterator.next();
            if (item.getValue().adj.isEmpty()) {
                nodes_iterator.remove();
            }
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return nodes.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        validateVertex(v);
        return nodes.get(v).adj;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double shortest = Double.MAX_VALUE;
        long ret = -117;
        for (long id : nodes.keySet()) {
            Node x = nodes.get(id);
            double current_dist = distance(lon(id), lat(id), lon, lat);
            if (current_dist < shortest) {
                shortest = current_dist;
                ret = id;
            }
        }
        return ret;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        validateVertex(v);
        return nodes.get(v).lon;
    }

    double locLon(long v) {
        validateLocation(v);
        return locations.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        validateVertex(v);
        return nodes.get(v).lat;
    }

    double locLat(long v) {
        validateLocation(v);
        return locations.get(v).lat;
    }

    /**
     * Gets the tagged name of a vertex.
     * @param v The id of the vertex.
     * @return The tagged name of the vertex.
     */
    String getName(long v) {
        validateLocation(v);
        return locations.get(v).name;
    }

    /**
     * Adds node to the graph.
     * @param id The id of the node
     * @param lon Longitude of the node
     * @param lat Latitude of the node
     */
    void addNode(long id, double lon, double lat) {
        Node node = new Node(lon, lat);
        nodes.put(id, node);
    }

    /**
     * Keeps track of a new location with a name.
     * @param id id of the location
     * @param lon longitude of the node
     * @param lat latitude of the location
     * @param name name of the location
     */
    void addLocation(long id, double lon, double lat, String name) {
        Location loc = new Location(lon, lat, name);
        locations.put(id, loc);
    }

    /**
     * Adds edge v-w to this graph.
     * @param v one vertex in the edge
     * @param w another vertex in the edge
     */
    void addEdge(long v, long w) {
        validateVertex(v);
        validateVertex(w);
        nodes.get(v).adj.add(w);
        nodes.get(w).adj.add(v);
    }

    /**
     * Adds a location name to locations map and Trie.
     * @param id id of node for the given location name
     * @param locationName node's cleaned name
     */
    void addName(long id, double lon, double lat, String locationName) {
        String cleanedName = cleanString(locationName);

        if (!names.containsKey(cleanedName)) {
            names.put(cleanedName, new LinkedList<>());
        }
        names.get(cleanedName).add(id);
        addLocation(id, lon, lat, locationName);
        st.put(cleanedName, id);
    }

    /**
     * Adds all edges in a way.
     * @param way list of nodes
     */
    void addWay(List<Long> way, String wayName) {
        nodes.get(way.get(0)).wayNames.add(wayName);
        for (int i = 1; i < way.size(); i++) {
            addEdge(way.get(i - 1), way.get(i));
            nodes.get(way.get(i)).wayNames.add(wayName);
        }
    }

    /**
     * @param v vertex in the way
     * @return set of ways this vertex belongs to
     */
    Set<String> getWayNames(long v) {
        Set<String> result = new HashSet<>();
        for (String way : nodes.get(v).wayNames) {
            result.add(way);
        }
        return result;
    }

    /**
     * Returns a list of keys that share a prefix.
     * @param prefix prefix entered in search box
     * @return list of keys
     */
    public List<String> keysWithPrefix(String prefix) {
        List<String> result = new LinkedList<>();
        for (String key : st.keysWithPrefix(cleanString(prefix))) {
            Long id = names.get(key).get(0);
            String fullName = getName(id);
            result.add(fullName);
        }
        return result;
    }

    public List<Long> getLocations(String locationName) {
        List<Long> result = new LinkedList<>();
        for (long v : names.get(cleanString(locationName))) {
            result.add(v);
        }
        return result;
    }

    /**
     * throw an IllegalArgumentException if vertex not in graph
     * @param v vertex to validate
     */
    private void validateVertex(long v) {
        if (!nodes.containsKey(v)) {
            throw new IllegalArgumentException("Vertex " + v + " is not in the graph.");
        }
    }

    private void validateLocation(long v) {
        if (!locations.containsKey(v)) {
            throw new IllegalArgumentException("Vertex " + v + " does not have a name.");
        }
    }

    // Graph node that stores information about an OpenStreetMaps node
    private class Node {
        double lon;
        double lat;
        List<Long> adj;
        Set<String> wayNames;

        Node(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
            adj = new LinkedList<>();
            wayNames = new HashSet<>();
        }
    }

    // Class to store information about locations with names
    private class Location {
        double lon;
        double lat;
        String name = "";

        Location(double lon, double lat, String name) {
            this.lon = lon;
            this.lat = lat;
            this.name = name;
        }
    }
}
