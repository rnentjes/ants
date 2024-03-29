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

        MyBot bot = new MyBot();
        bot.readSystemInput();
    }

    public MyBot() {
        debug("Starting ");
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    Set<Tile> enemyHills = new HashSet<Tile>();
    Set<Tile> foodTiles = new HashSet<Tile>();

    /**
     * For every ant check every direction in fixed order (N, E, S, W) and move it if the tile is
     * passable.
     */
    @Override
    public void doSubTurn() {
        if (!init) {
            init();
        }

        Ants ants = getAnts();

        remainingAnts.addAll(ants.getMyAnts());

        //updateEnemyVisited(10);
        updateVisited();

        /*

        List<Tile> foods = new LinkedList<Tile>();
        foods.addAll(ants.getFoodTiles());

        // remove already targeted tiles from list
        for (Command command : commands) {
            for (Tile tile : command.skipTiles()) {
                if (remainingAnts.contains(tile)) {
                    remainingAnts.remove(tile);
                }

                if (foods.contains(tile)) {
                    foods.remove(tile);
                }
            }
        }
        */

        /*
        while (!foods.isEmpty() && hasRemainingTime()) {
            Tile food = foods.get(0);

            HashMap<Tile, Integer> map = new HashMap<Tile, Integer>();
            map.put(food, 0);
            List<Tile> path = findShortestPathToOne(map, 1, 15, remainingAnts);
            Collections.reverse(path);

            if (path.size() > 1) {
                HashMap<Tile, Integer> map2 = new HashMap<Tile, Integer>();
                map2.put(path.get(0), 0);
                List<Tile> antToFoodPath = findShortestPathToOne(map2, 1, 15, foods);

                if (antToFoodPath.size() < path.size()) {
                    food = antToFoodPath.get(antToFoodPath.size() - 1);
                    debug("Path (R) from " + antToFoodPath.get(0) + " to " + food + " = " + antToFoodPath);
                    remainingAnts.remove(antToFoodPath.get(0));
                    addCommand(new MoveToFood(antToFoodPath));
                    foods.remove(food);
                } else {
                    debug("Path from " + path.get(0) + " to " + food + " = " + path);
                    remainingAnts.remove(path.get(0));
                    addCommand(new MoveToFood(path));
                    foods.remove(food);
                }
            } else {
                foods.remove(food);
            }
        }*/

        // attack ants close to one of my hills

        for (Tile tile : ants.getEnemyAnts()) {
            if (hasRemainingTime()) {
                boolean closeToHill = false;
                for (Tile hill : ants.getMyHills()) {
                    if (ants.getDistance(tile, hill) < ants.getViewRadius2() * 3) {
                        for (Tile myAnt : ants.getMyAnts()) {
                            if (ants.getDistance(tile, myAnt) < ants.getViewRadius2() * 2) {
                                List<Tile> route = findShortestPath(myAnt, tile);

                                debug("Found route to enemy (" + tile + ") " + route);

                                if (route.size() > 1) {
                                    Aim aim = getDirection(myAnt, route.get(1));

                                    if (goodOrder(myAnt, aim)) {
                                        executeOrder(myAnt, aim);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

//        for (Tile food : ants.getFoodTiles()) {
//            moveToFood(food);
//        }

        for (Tile ant : ants.getMyAnts()) {
            enemyHills.remove(ant);
            foodTiles.remove(ant);
        }

        for (Tile ehill : ants.getEnemyHills()) {
            enemyHills.add(ehill);
        }

        for (Tile food : ants.getFoodTiles()) {
            foodTiles.add(food);
        }

        for (Tile ehill : enemyHills) {
            attack(ehill, viewDistance1 * 4);
        }

//        for (Tile food : foodTiles) {
//            attack(food, viewDistance1 * 2);
//        }


        /***************************************************************************/

        //runCommands();

        /***************************************************************************/

        // find opportunities to kill

        List<Tile> enemyAnts = new LinkedList<Tile>(ants.getEnemyAnts());
        for (Tile en : ants.getEnemyAnts()) {
            if (hasRemainingTime()) {
                List<Tile> enemies = new LinkedList<Tile>();
                List<Tile> friends = new LinkedList<Tile>();

                enemies.add(en);
                for (Tile fr : remainingAnts) {
                    if (ants.getDistance(en, fr) <= (ants.getAttackRadius2() * 2)) {
                        friends.add(fr);

                        for (Tile en2 : enemyAnts) {
                            if (ants.getDistance(en2, fr) <= (ants.getAttackRadius2() * 2) && !enemies.contains(en2)) {
                                enemies.add(en2);
                            }
                        }
                    }
                }

                if (!enemies.isEmpty() && !friends.isEmpty()) {
                    debug("Running scenarios for " + friends + " & " + enemies);
                    ScenarioRunner runner = new ScenarioRunner(this, ants, enemies, friends);

                    Map<Tile, Aim> best = runner.findBestScenario();

                    if (best != null) {
                        debug("Executing scenario " + best);
                        for (Tile ant : best.keySet()) {
                            if (goodOrder(ant, best.get(ant))) {
                                executeOrder(ant, best.get(ant));
                            }
                        }
                    }
                }
            }
        }

        // find food

        List<Tile> myAnts = new LinkedList<Tile>(remainingAnts);

        for (Tile ma : myAnts) {
            // find direction to food withing viewing range
            HashMap<Tile, Integer> v = new HashMap<Tile, Integer>();
            v.put(ma, 0);
            List<Tile> route = findShortestPathToOne(v, 1, viewDistance1, new LinkedList<Tile>(foodTiles));

            debug("Found route to food (" + ma + ") " + route);

            if (route.size() > 1) {
                Aim aim = getDirection(ma, route.get(1));

                if (goodOrder(ma, aim)) {
                    executeOrder(ma, aim);
                }
            }
        }

        // avoid & attack maps

        for (Tile tile : ants.getEnemyAnts()) {
            if (hasRemainingTime()) {
                boolean closeToHill = false;
                for (Tile hill : ants.getMyHills()) {
                    if (ants.getDistance(tile, hill) < ants.getViewRadius2() * 3) {
                        closeToHill = true;
                        break;
                    }
                }

                if (closeToHill && ants.getMyAnts().size() > 4) {
                    attack(tile, viewDistance1 * 3);
                } else {
                    avoid(tile);
                }
            }
        }

        /* ---------------------------- */

        List<Tile> antsToMove = new LinkedList<Tile>();
        antsToMove.addAll(remainingAnts);

        debug("Moving " + antsToMove.size() + " ants around.");
        int attempts = antsToMove.size() * 2;

        SortByMaps sort = new SortByMaps();

        sort.addMap(attackAnts, 32);
        sort.addMap(avoidAnts, 16);
        //sort.addMap(foodTiles, 8);

        Map<Tile, Integer> bordersAndVisited = addMaps(visited, 1, borders, 5);

        sort.addMap(bordersAndVisited, 2);

        while (!antsToMove.isEmpty() && hasLittleRemainingTime() && --attempts > 0) {
            Tile myAnt = antsToMove.remove(0);

            List<Tile> candidates = getCandidates(myAnt);

            Collections.sort(candidates, sort);

            for (Tile targetedTile : candidates) {
                Aim aim = getDirection(myAnt, targetedTile);
                //debug("Ant: "+myAnt+" looking for least visited move, aim: "+aim);

                if (goodOrder(myAnt, aim)) {
                    //debug("Ant " + myAnt + " following border map " + aim);
                    executeOrder(myAnt, aim);
                    break;
                }
            }

            if (!antsDone.contains(myAnt)) {
                antsToMove.add(myAnt);
            }
        }

        /* ------------------------------------------------------------------------ */

        updateBorderMap(4);
        if (debug && hasLittleRemainingTime()) {
            //debug(showBorderMap());
            //debug(sort.showMap(bordersAndVisited));
        }

        debug("Remaining ants " + remainingAnts);

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
