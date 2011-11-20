import java.io.IOException;
import java.util.*;

/**
 * Starter bot implementation.
 */
public class MyBotv4 extends Bot {
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

        MyBotv4 bot = new MyBotv4();
        bot.readSystemInput();
    }

    public MyBotv4() {
        debug("Starting ");
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

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
        }

        for (Tile ehill : ants.getEnemyHills()) {
            if (hasRemainingTime()) {
                List<Tile> attackers = findAntsWithinViewingDistance(remainingAnts, ehill);

                for (Tile ant : attackers) {
                    List<Tile> path = findShortestPath(ant, ehill);

                    if (path.size() > 1 && path.size() < ants.getViewRadius2()) {
                        remainingAnts.remove(path.get(0));
                        addCommand(new Move(path));
                    }
                }
            }
        }

        List<Tile> enemys = new LinkedList<Tile>(ants.getEnemyAnts());

        /* ----- */
        /*
        List<Tile> antsOnHill = new LinkedList<Tile>();

        for (Tile tile : remainingAnts) {
            if (ants.getMyHills().contains(tile)) {
                antsOnHill.add(tile);
            }
        }

        if (ants.getMyAnts().size() > 10) {
            for (Tile tile : antsOnHill) {
                if (getAntsDefendingHill(tile) < (ants.getMyAnts().size() - 10) / 10) {
                    debug("Ant "+tile+" defending "+tile);
                    addCommand(new Defend(tile));
                }
            }
        }*/


        /***************************************************************************/

        runCommands();

        /***************************************************************************/

        // find opportunities to kill

/*
        List<Tile> enemyAnts = new LinkedList<Tile>(ants.getEnemyAnts());
        for (Tile en : ants.getEnemyAnts()) {
            List<Tile> enemies = new LinkedList<Tile>(ants.getEnemyAnts());
            List<Tile> friends = new LinkedList<Tile>();

            enemies.add(en);
            for (Tile fr : remainingAnts) {
                if (ants.getDistance(en, fr) <= (ants.getAttackRadius2() + 5)) {
                    friends.add(fr);
                }

                for (Tile en2 : enemyAnts) {
                    if (ants.getDistance(en2, fr) <= (ants.getAttackRadius2() + 5) && !enemies.contains(en2)) {
                        enemies.add(en2);
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
        }*/

        // avoid & attack maps

        for (Tile tile : ants.getEnemyAnts()) {
            if (hasRemainingTime()) {
                boolean closeToHill = false;
                for (Tile hill : ants.getMyHills()) {
                    if (ants.getDistance(tile, hill) < ants.getViewRadius2() * 2) {
                        closeToHill = true;
                        break;
                    }
                }

                if (closeToHill) {
                    attack(tile);
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

        while (!antsToMove.isEmpty() && hasLittleRemainingTime() && --attempts > 0) {
            Tile myAnt = antsToMove.remove(0);

            List<Tile> candidates = getCandidates(myAnt);

            Collections.sort(candidates, new SortBy4Maps(visited, borders, avoidAnts, attackAnts, 1, 10, 30, 40));

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
