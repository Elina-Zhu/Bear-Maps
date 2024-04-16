import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;


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
    /**
     * Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc.
     */
    private final Map<Long, Node> nodes = new LinkedHashMap<>();
    private final Map<Long, Way> ways = new LinkedHashMap<>();
    private final Trie trieForNodeName = new Trie();
    private final Map<Long, NameNode> nameNodes = new LinkedHashMap<>();
    private final Map<String, List<Long>> locations = new LinkedHashMap<>();
    private final KdTree kdTreeForNearestNeighbor = new KdTree();

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

        for (long node : nodes.keySet()) {
            addToKdTree(nodes.get(node));
        }
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
        Iterator<Long> nodesIterator = nodes.keySet().iterator();
        while (nodesIterator.hasNext()) {
            Long node = nodesIterator.next();
            if (nodes.get(node).adjs.isEmpty()) {
                nodesIterator.remove();
            }
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        return nodes.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return nodes.get(v).adjs;
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
        return kdTreeForNearestNeighbor.nearest(lon, lat);
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return nodes.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return nodes.get(v).lat;
    }

    void addNode(Node n) {
        nodes.put(n.id, n);
    }

    void addWay(Way w) {
        ways.put(w.id, w);
    }

    void addAdj(Long node1, Long node2) {
        nodes.get(node1).adjs.add(node2);
    }

    public void addToKdTree(GraphDB.Node n) {
        kdTreeForNearestNeighbor.insert(n);
    }

    public void addCleanNameToTrie(String cleanName, String name) {
        trieForNodeName.add(cleanName, name);
    }

    public List<String> collectFromTrie(String prefix) {
        Trie.TrieNode prefixEnd = trieForNodeName.findNode(prefix);
        List<String> results = new ArrayList<>();
        if (prefixEnd != null) {
            collectHelper(prefix, results, prefixEnd);
        }
        return results;
    }

    private void collectHelper(String s, List<String> results, Trie.TrieNode node) {
        if (node.isWord()) {
            results.addAll(node.getNames());
        }
        for (char c : node.getChildren().keySet()) {
            collectHelper(s + c, results, node.getChildren().get(c));
        }
    }

    public void addLocation(String name, long id) {
        if (locations.containsKey(name)) {
            locations.get(name).add(id);
        } else {
            locations.put(name, new ArrayList<>(Arrays.asList(id)));
        }
    }

    public List<String> getLocationsByPrefix(String prefix) {
        return collectFromTrie(prefix);
    }

    public List<Map<String, Object>> getLocations(String locationName) {
        List<Map<String, Object>> results = new LinkedList<>();
        if (locations.containsKey(locationName)) {
            for (long id : locations.get(locationName)) {
                results.add(getNameNodeAsMap(id));
            }
        }
        return results;
    }

    /** The helper class and methods of Node. */
    static class Node {
        long id;
        double lon;
        double lat;
        Set<Long> adjs;
        double priority = 0;
        double distTo = 0;
        List<Long> wayIds;

        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.adjs = new LinkedHashSet<>();
            this.wayIds = new ArrayList<>();
        }
    }

    public double getDistTo(long v) {
        return nodes.get(v).distTo;
    }

    public void changeDistTo(long v, double newDistTo) {
        nodes.get(v).distTo = newDistTo;
    }

    public void changePriority(long v, double newPriority) {
        nodes.get(v).priority = newPriority;
    }

    class NodeComparator implements Comparator<Long> {
        @Override
        public int compare(Long v, Long w) {
            return Double.compare(nodes.get(v).priority, nodes.get(w).priority);
        }
    }

    public Comparator<Long> getNodeComparator() {
        return new NodeComparator();
    }

    public Node getNode(long nodeId) {
        return nodes.get(nodeId);
    }

    /** The helper class and methods of Way. */
    static class Way {
        long id;
        String maxSpeed;
        String name;
        String highway;
        List<Long> locations;

        Way(long id) {
            this.id = id;
            this.locations = new ArrayList<>();
        }
    }

    public List<Long> getWays(long nodeId) {
        return nodes.get(nodeId).wayIds;
    }

    public String getWayName(long wayId) {
        return ways.get(wayId).name;
    }

    /** The helper class and methods of NameNode. */
    static class NameNode {
        long id;
        double lon;
        double lat;
        String name;

        public NameNode(long id, double lon, double lat, String name) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
            this.name = name;
        }
    }

    public void addNameNode(NameNode n) {
        nameNodes.put(n.id, n);
    }

    public String getNodeName(long id) {
        return nameNodes.get(id).name;
    }

    public Map<String, Object> getNameNodeAsMap(long id) {
        NameNode n = nameNodes.get(id);
        Map<String, Object> results = new HashMap<>();
        results.put("id", n.id);
        results.put("lon", n.lon);
        results.put("lat", n.lat);
        results.put("name", n.name);
        return results;
    }
}
