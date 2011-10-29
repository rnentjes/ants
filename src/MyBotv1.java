import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Starter bot implementation.
 */
public class MyBotv1 extends Bot {
    static boolean debug = false;
    PrintWriter out = null;

    public void debug(String message) {
        if (debug) {
            if (out == null) {
                try {
                    out = new PrintWriter(new File(".", "bot_log_"+ System.currentTimeMillis()+".log"));
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }

            out.println(message);
            out.flush();
        }
    }

    /**
     * Main method executed by the game engine for starting the bot.
     *
     * @param args command line arguments
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            debug = true;
        }

        new MyBotv1().readSystemInput();
    }

    public MyBotv1() {
        debug("Starting");
    }

    private List<Tile> assignedFood;
    private Map<Tile, List<Tile>> standingOrders = new HashMap<Tile, List<Tile>>();
    private int turn = 0;

    /**
     * For every ant check every direction in fixed order (N, E, S, W) and move it if the tile is
     * passable.
     */
    @Override
    public void doSubTurn() {
        long time = System.currentTimeMillis();
        turn++;
        try {
            // strategy 1st
            // find closest food source for each ant
            // sort by distance
            // assign food source to closest ant
            // again until all ants have a job to do

            // strategy 2nd
            // something with enemy's

            List<Tile> targetedTiles = new LinkedList<Tile>();
            List<Tile> antsDone = new LinkedList<Tile>();
            List<Tile> remainingAnts = new LinkedList<Tile>();

            Ants ants = getAnts();

            remainingAnts.addAll(ants.getMyAnts());

            updateEnemyVisited(10);
            updateVisited();

            Set<Tile> antsWithOrders = new HashSet<Tile>();
            antsWithOrders.addAll(standingOrders.keySet());

            for (Tile tile : antsWithOrders) {
                if (!ants.getMyAnts().contains(tile)) {
                    standingOrders.remove(tile);
                } else {
                    List<Tile> orders = standingOrders.get(tile);
                    standingOrders.remove(tile);

                    if (!orders.isEmpty()) {
                        Tile next = orders.get(0);
                        if (!targetedTiles.contains(next)) {
                            orders.remove(0);
                            ants.issueOrder(tile, getDirection(tile, next));
                            targetedTiles.add(next);

                            if (orders.size() > 1) { // forget about the last step
                                standingOrders.put(next, orders);
                            }
                            
                            remainingAnts.remove(tile);
                        }
                    }
                }
            }

            Set<Tile> foods = new HashSet<Tile>();
            foods.addAll(ants.getFoodTiles());

            // remove already targeted tiles from list
            for (Tile food : ants.getFoodTiles()) {
                for (Tile tile : standingOrders.keySet()) {
                    List<Tile> orders = standingOrders.get(tile);

                    if (!orders.isEmpty() && orders.get(orders.size() - 1).equals(food)) {
                        foods.remove(food);
                    }
                }
            }

            for (Tile food : foods) {
                if (ants.getTimeRemaining() > 100) {
                    Tile ant = findClosestAntWithinViewingDistance(remainingAnts, food);

                    if (ant != null) {
                        List<Tile> path = findShortestPath(ant, food);

                        if (!path.isEmpty() && path.size() < ants.getViewRadius2()) {
                            List<Tile> orders = new LinkedList<Tile>();
                            orders.addAll(path);
                            Tile first = orders.get(0);
                            if (!targetedTiles.contains(first)) {
                                orders.remove(0);
                                ants.issueOrder(ant, getDirection(ant, first));
                                targetedTiles.add(first);
                                remainingAnts.remove(ant);
                                standingOrders.put(first, orders);
                            }
                        }
                    }
                }
            }

            for (Tile myAnt : remainingAnts) {
                if (!antsDone.contains(myAnt) && ants.getTimeRemaining() > 10) {
                    boolean done = false;

                    if (!done) {
                        List<Tile> sortByLeastVisited = getCandidatesSortedByLeastVisited(myAnt);

                        for (Tile targetedTile : sortByLeastVisited) {
                            Aim aim = getDirection(myAnt, targetedTile);
                            if (aim != null && ants.getIlk(myAnt, aim).isPassable() && !targetedTiles.contains(targetedTile) && ants.getIlk(myAnt, aim).isUnoccupied()) {
                                ants.issueOrder(myAnt, aim);
                                targetedTiles.add(targetedTile);
                                antsDone.add(myAnt);
                                done = true;
                                break;
                            }
                        }

                        int attempts = 0;
                        while (attempts < 10 && !done) {
                            attempts++;
                            Aim direction = getRandomDirection();
                            Tile targetedTile = ants.getTile(myAnt, direction);
                            Ilk ilk = ants.getIlk(myAnt, direction);
                            if (!targetedTiles.contains(targetedTile) && ilk.isPassable() && ilk.isUnoccupied()) {
                                ants.issueOrder(myAnt, direction);
                                targetedTiles.add(targetedTile);
                                antsDone.add(myAnt);
                                done = true;
                            }
                        }
                    }

                    if (!done) {
                        for (Aim direction : Aim.values()) {
                            Tile targetedTile = ants.getTile(myAnt, direction);
                            Ilk ilk = ants.getIlk(myAnt, direction);
                            if (!targetedTiles.contains(targetedTile) && ilk.isPassable() && ilk.isUnoccupied()) {
                                ants.issueOrder(myAnt, direction);
                                targetedTiles.add(targetedTile);
                                antsDone.add(myAnt);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace(out);
            }
            throw new IllegalStateException(t);
        }

        time = System.currentTimeMillis() - time;
        debug("Turn "+turn+"/"+getAnts().getTurns()+" took - "+time+"/"+getAnts().getTurnTime());

    }

    public void showStandingOrders() {
        Set<Tile> antsWithOrders = standingOrders.keySet();

        for (Tile tile : antsWithOrders) {
            List<Tile> orders = standingOrders.get(tile);

            System.err.println("Orders Ant: " + tile);

            for (Tile t : orders) {
                System.err.println("\t" + t);
            }
        }
    }


}
