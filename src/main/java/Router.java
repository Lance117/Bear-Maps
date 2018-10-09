import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {

    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        /* Initialize structures to get shortest path */
        Map<Long, Long> edgeTo = new HashMap<>();
        Map<Long, Double> distTo = new HashMap<>();
        List<Long> route = new LinkedList<>();
        long src = g.closest(stlon, stlat);
        long dest = g.closest(destlon, destlat);
        PriorityQueue<Long> fringe = new PriorityQueue<Long>(new Comparator<Long>() {
            @Override
            public int compare(Long w, Long v) {
                double wCost = distTo.get(w) + g.distance(w, dest);
                double vCost = distTo.get(v) + g.distance(v, dest);
                if (wCost < vCost) {
                    return -1;
                }
                if (wCost > vCost) {
                    return 1;
                }
                return 0;
            }
        });

        /* Add initial values to edgeTo, distTo, and fringe */
        for (long v : g.vertices()) {
            distTo.put(v, Double.POSITIVE_INFINITY);
            edgeTo.put(v, (long) -117);
        }
        distTo.replace(src, 0.0);
        edgeTo.put(src, (long) 0);
        fringe.add(src);

        /* A* search algorithm */
        while (!fringe.isEmpty()) {
            long curr = fringe.poll();
            if (curr == dest) {
                break;
            }
            for (long neighbor : g.adjacent(curr)) {
                double distance = distTo.get(curr) + g.distance(curr, neighbor);
                if (distance < distTo.get(neighbor)) {
                    distTo.put(neighbor, distance);
                    edgeTo.put(neighbor, curr);
                    fringe.add(neighbor);
                }
            }
        }

        /* Get path to destination. */
        for (long e = dest; e != 0; e = edgeTo.get(e)) {
            route.add(0, e);
        }

        return route;
    }

    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigationDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        List<NavigationDirection> result = new ArrayList<>();
        long startNode = route.get(0);
        double distance = 0;
        double relativeBearing = 0;
        double prevBearing = g.bearing(route.get(0), route.get(1));
        int currentDirection = NavigationDirection.START;
        String currentWay = "";

        if (route.size() < 2) {
            return null;
        }

        for (int i = 1; i < route.size(); i++) {
            long prevNode = route.get(i - 1);
            long currNode = route.get(i);
            double currBearing = g.bearing(prevNode, currNode);
            relativeBearing = currBearing - prevBearing;
            //System.out.println(relativeBearing);

            /* Get name of the current way */
            if (prevNode == startNode) {
                currentWay = getCurrentWay(g, prevNode, currNode);
            }
            else {
                prevBearing = currBearing;
            }

            if (g.getWayNames(currNode).contains(currentWay) && i != route.size() - 1) {
                distance += g.distance(prevNode, currNode);
                continue;
            }

            /* Add last stretch of distance if reached last node */
            if (i == route.size() - 1) {
                distance += g.distance(prevNode, currNode);
            }

            /* Get distance traveled along current way and add nav direction to result */
            NavigationDirection turn = new NavigationDirection();
            turn.direction = currentDirection;
            turn.distance = distance;
            turn.way = currentWay;
            result.add(turn);

            /* Start the next way and get direction to turn */
            startNode = currNode;
            distance = g.distance(prevNode, currNode);
            currentDirection = getDirection(relativeBearing);
        }
        return result;
    }

    /**
     * @param g The graph to use
     * @param v Previous vertex to check
     * @param w Current vertex to check for current way
     * @return current way
     */
    private static String getCurrentWay(GraphDB g, long v, long w) {
        for (String a : g.getWayNames(v)) {
            for (String b : g.getWayNames(w)) {
                if (a.equals(b)) {
                    return a;
                }
            }
        }
        return "";
    }

    /**
     * @param relativeBearing Relative bearing between two points
     * @return
     */
    private static int getDirection(double relativeBearing) {
        double absBearing = Math.abs(relativeBearing);
        if (absBearing > 180) {
            absBearing = 360 - absBearing;
            relativeBearing *= -1;
        }
        if (absBearing <= 15) {
            return NavigationDirection.STRAIGHT;
        }
        if (absBearing <= 30) {
            return relativeBearing < 0 ? NavigationDirection.SLIGHT_LEFT : NavigationDirection.SLIGHT_RIGHT;
        }
        if (absBearing <= 100) {
            return relativeBearing < 0 ? NavigationDirection.LEFT : NavigationDirection.RIGHT;
        }
        else {
            return relativeBearing < 0 ? NavigationDirection.SHARP_LEFT : NavigationDirection.SHARP_RIGHT;
        }
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
