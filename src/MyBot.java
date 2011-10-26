import java.io.IOException;
import java.util.*;

/**
 * Starter bot implementation.
 */
public class MyBot extends Bot {
    /**
     * Main method executed by the game engine for starting the bot.
     *
     * @param args command line arguments
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            debug = true;
        }

        new MyBot().readSystemInput();
    }

    public MyBot() {
        debug("Starting");
    }

    /**
     * For every ant check every direction in fixed order (N, E, S, W) and move it if the tile is
     * passable.
     */
    @Override
    public void doSubTurn() {
        // strategy 1st
        // find closest food source for each ant
        // sort by distance
        // assign food source to closest ant
        // again until all ants have a job to do

        // strategy 2nd
        // something with enemy's

        Ants ants = getAnts();

        remainingAnts.addAll(ants.getMyAnts());

        //updateEnemyVisited(10);
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
                        debug("Ant "+tile+" following orders "+next);
                        ants.issueOrder(tile, getDirection(tile, next));
                        targetedTiles.add(next);

                        if (orders.size() > 1 && ants.getIlk(orders.get(0)).equals(Ilk.FOOD)) { // forget about the last step
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

        // remove already targeted tiles from list
        for (Tile hill : ants.getEnemyHills()) {
            for (Tile tile : standingOrders.keySet()) {
                List<Tile> orders = standingOrders.get(tile);

                if (!orders.isEmpty() && orders.get(orders.size() - 1).equals(hill)) {
                    foods.remove(hill);
                }
            }
        }

        for (Tile food : foods) {
            if (ants.getTimeRemaining() > 100) {
                Tile ant = findClosestAntWithinViewingDistance(remainingAnts, food);

                if (ant != null) {
                    List<Tile> path = findShortestPath(ant, food);

                    debug("Path from "+ant+" to "+food+" = "+path);

                    if (!path.isEmpty()) {
                        List<Tile> orders = new LinkedList<Tile>();
                        orders.addAll(path);
                        Tile first = orders.get(0);
                        Aim aim = getDirection(ant, first);
                        if (goodOrder(ant, aim)) {
                            orders.remove(0);
                            debug("Ant "+ant+" moving to food "+getDirection(ant, first));
                            executeOrder(ant, getDirection(ant, first));
                            standingOrders.put(first, orders);
                        }
                    }
                }
            }
        }

        for (Tile ehill : ants.getEnemyHills()) {
            if (ants.getTimeRemaining() > 100) {
                List<Tile> attackers = findAntsWithinViewingDistance(remainingAnts, ehill);

                for (Tile ant : attackers) {
                    List<Tile> path = findShortestPath(ant, ehill);

                    if (!path.isEmpty() && path.size() < ants.getViewRadius2()) {
                        List<Tile> orders = new LinkedList<Tile>();
                        orders.addAll(path);
                        Tile first = orders.get(0);
                        Aim aim = getDirection(ant, first);
                        if (goodOrder(ant, aim)) {
                            orders.remove(0);
                            debug("Ant "+ant+" moving to hill "+getDirection(ant, first));
                            executeOrder(ant, getDirection(ant, first));
                            standingOrders.put(first, orders);
                        }
                    }
                }
            }
        }

        for (Tile eant : ants.getEnemyAnts()) {
            if (ants.getTimeRemaining() > 100) {
                List<Tile> attackers = findAntsWithinViewingDistance(remainingAnts, eant);

                for (Tile ant : attackers) {
                    if (goodOrder(ant, ants.getDirections(ant, eant).get(0))) {
                        executeOrder(ant, ants.getDirections(ant, eant).get(0));
                    }
                }
            }
        }

        List<Tile> antsToMove = new LinkedList<Tile>();
        /*
        antsToMove.addAll(remainingAnts);

        for (Tile myAnt : antsToMove) {
            Aim aim = getTileFurtherstAwayFromMyAnts(myAnt);

            if (goodOrder(myAnt, aim)) {
                debug("Ant "+myAnt+" moving away from other ants "+aim);
                executeOrder(myAnt, aim);
            }
        }*/

        antsToMove = new LinkedList<Tile>();
        antsToMove.addAll(remainingAnts);
        for (Tile myAnt : antsToMove) {
            if (ants.getTimeRemaining() > 10) {
                //debug("Ant: "+myAnt+" looking for least visited move.");
                List<Tile> sortByLeastVisited = getNeighborsByLeastVisited(myAnt);
                
                for (Tile targetedTile : sortByLeastVisited) {
                    Aim aim = getDirection(myAnt, targetedTile);
                    //debug("Ant: "+myAnt+" looking for least visited move, aim: "+aim);

                    if (goodOrder(myAnt, aim)) {
                        debug("Ant "+myAnt+" moving to least visited "+aim);
                        executeOrder(myAnt, aim);
                        break;
                    }
                }

            }
        }

        debug("Remaining ants "+remainingAnts);

    }

    private Aim getDirection(Tile t1, Tile t2) {
        for (Aim aim : Aim.values()) {
            if (getAnts().getTile(t1, aim).equals(t2)) {
                return aim;
            }
        }

        return null;
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
