import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Provides basic game state handling.
 */
public abstract class Bot extends AbstractSystemInputParser {
    static boolean debug = false;
    PrintWriter out = null;

    public void debug(String message) {
        if (debug) {
            if (out == null) {
                try {
                    int count = 1;
                    File file = new File(".", "bot_log_"+ count+".log");

                    while (file.exists()) {
                        count++;
                        file = new File(".", "bot_log_"+ count+".log");
                    }

                    out = new PrintWriter(file);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }

            out.println(message);
            out.flush();
        }
    }

    private Ants ants;

    protected Map<Tile, Integer> visited = new HashMap<Tile, Integer>();
    protected Random r = new Random(1);
    
    protected Map<Tile, List<Tile>> standingOrders = new HashMap<Tile, List<Tile>>();
    protected int turn = 0;

    protected List<Tile> targetedTiles = new LinkedList<Tile>();
    protected List<Tile> antsDone = new LinkedList<Tile>();
    protected List<Tile> remainingAnts = new LinkedList<Tile>();

    public void doTurn() {
        long time = System.currentTimeMillis();
        turn++;
        debug("---------- Turn "+turn+", "+getAnts().getMyAnts().size()+" ants, "+getAnts().getMyHills()+" hills. ----------");

        try {
            targetedTiles = new LinkedList<Tile>();
            antsDone = new LinkedList<Tile>();
            remainingAnts = new LinkedList<Tile>();

            doSubTurn();
        } catch (Exception t) {
            if (debug) {
                t.printStackTrace(out);
                out.flush();
            }
            throw new IllegalStateException(t);
        }

        time = System.currentTimeMillis() - time;
        debug("********** Turn "+turn+"/"+getAnts().getTurns()+" took - "+time+"/"+getAnts().getTurnTime()+" **********");

    }

    public abstract void doSubTurn();

    protected boolean goodOrder(Tile myAnt, Aim aim) {
        if (aim == null) { return false; }
        Tile targetedTile = ants.getTile(myAnt, aim);
        Ilk ilk = ants.getIlk(myAnt, aim);

        //debug("goodOrder: "+targetedTile+"="+ilk+" -> "+!targetedTiles.contains(targetedTile)+" - "+ilk.isPassable()+" - "+ilk.isUnoccupied()+" - "+!getAnts().getMyHills().contains(targetedTile));

        return (!targetedTiles.contains(targetedTile) && ilk.isPassable() && !getAnts().getMyHills().contains(targetedTile));
    }

    protected void executeOrder(Tile myAnt, Aim aim) {
        ants.issueOrder(myAnt, aim);
        targetedTiles.add(getAnts().getTile(myAnt, aim));
        antsDone.add(myAnt);
        remainingAnts.remove(myAnt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
                      int attackRadius2, int spawnRadius2, int player_seed) {
        setAnts(new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
                spawnRadius2, player_seed));
    }

    /**
     * Returns game state information.
     *
     * @return game state information
     */
    public Ants getAnts() {
        return ants;
    }

    /**
     * Sets game state information.
     *
     * @param ants game state information to be set
     */
    protected void setAnts(Ants ants) {
        this.ants = ants;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeUpdate() {
        ants.setTurnStartTime(System.currentTimeMillis());
        ants.clearMyAnts();
        ants.clearEnemyAnts();
        ants.clearMyHills();
        ants.clearEnemyHills();
        ants.getFoodTiles().clear();
        ants.getOrders().clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addWater(int row, int col) {
        ants.update(Ilk.WATER, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnt(int row, int col, int owner) {
        ants.update(owner > 0 ? Ilk.ENEMY_ANT : Ilk.MY_ANT, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFood(int row, int col) {
        ants.update(Ilk.FOOD, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAnt(int row, int col, int owner) {
        ants.update(Ilk.DEAD, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHill(int row, int col, int owner) {
        ants.updateHills(owner, new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterUpdate() {
    }

    protected Aim getRandomDirection() {
        Aim result = null;

        int i = r.nextInt(4);

        switch (i) {
            case 0:
                result = Aim.NORTH;
                break;
            case 1:
                result = Aim.EAST;
                break;
            case 2:
                result = Aim.SOUTH;
                break;
            case 3:
                result = Aim.WEST;
                break;
            default:
                throw new IllegalStateException("Impossible value!");

        }

        return result;
    }

    protected List<Tile> getNeighborsByLeastVisited(Tile myAnt) {
        Ants ants = getAnts();

        Tile tileN = ants.getTile(myAnt, Aim.NORTH);
        Tile tileE = ants.getTile(myAnt, Aim.EAST);
        Tile tileS = ants.getTile(myAnt, Aim.SOUTH);
        Tile tileW = ants.getTile(myAnt, Aim.WEST);

        List<Tile> result = new LinkedList<Tile>();

        result.add(tileN);
        result.add(tileS);
        result.add(tileE);
        result.add(tileW);

        Collections.sort(result, new Comparator<Tile>() {
            public int compare(Tile o1, Tile o2) {
                Integer c1 = visited.get(o1);
                Integer c2 = visited.get(o2);

                if (c1 != null && c2 != null) {
                    return c1.compareTo(c2);
                } else if (c1 == null && c2 == null) {
                    return (r.nextInt(5) - 2);
                } else if (c1 != null) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        return result;
    }

    protected void updateEnemyVisited(int minimalArmy) {
        for (Tile enAnt : ants.getEnemyAnts()) {
            if (ants.getMyAnts().size() > minimalArmy) {
                Integer v = visited.get(enAnt);

                if (v != null) {
                    visited.put(enAnt, v - 1);
                }
            }
        }
    }

    protected void updateVisited() {
        for (Tile myAnt : ants.getMyAnts()) {
            Integer v = visited.get(myAnt);

            if (v == null) {
                visited.put(myAnt, 1);
            } else {
                visited.put(myAnt, v + 1);
            }
        }
    }

    public final class TileDistance {
        public Tile tile;
        public int distance;

        public TileDistance(Tile tile, int distance) {
            this.tile = tile;
            this.distance = distance;
        }
    }

    protected List<Tile> findShortestPath(Tile t1, Tile t2) {
        HashMap<Tile, Integer> tiles = new HashMap<Tile, Integer>();

        tiles.put(t1,0);

        //List<Tile> path = findShortestPath(new LinkedList<Tile>(), 0, t1, t2);
        List<Tile> path = findShortestPath(tiles, 1, t2);

        return path;
    }

    protected List<Tile> findShortestPath(List<Tile> visited, int currentDepth, Tile t1, Tile t2) {
        List<Tile> result = new LinkedList<Tile>();

        //System.err.println("CD: "+currentDepth);

        if (currentDepth > 100 || getAnts().getTimeRemaining() < 100) {
            return result;
        }

        List<Tile> candidates = new LinkedList<Tile>();
        for (Aim aim : Aim.values()) {
            Tile candidate = getAnts().getTile(t1, aim);

            if (ants.getIlk(candidate).isPassable() && !visited.contains(candidate)) {
                visited.add(candidate);
                candidates.add(candidate);
            }
        }

        sortByDistance(t2, candidates);

        for (Tile candidate : candidates) {
            if (candidate.equals(t2)) {
                result.add(candidate);

                return result;
            }

            List<Tile> restOfPath = findShortestPath(visited, ++currentDepth, candidate, t2);

            // found?
            if (!restOfPath.isEmpty() && restOfPath.get(restOfPath.size()-1).equals(t2)) {
                result.add(candidate);
                result.addAll(restOfPath);

                return result;
            }
        }

        return result;
    }

    protected List<Tile> findShortestPath(Map<Tile, Integer> visited, int currentDepth, Tile t2) {
        List<Tile> result = new LinkedList<Tile>();

        //System.err.println("CD: "+currentDepth);

        if (currentDepth > 50 || getAnts().getTimeRemaining() < 100) {
            return result;
        }

        Map<Tile, Integer> nextRound = new HashMap<Tile, Integer>(visited);

        for (Tile tile : visited.keySet()) {
            for (Aim aim : Aim.values()) {
                Tile candidate = getAnts().getTile(tile, aim);

                if (candidate.equals(t2)) {
                    return getShortestPathFromMap(visited, t2);
                }

                if (ants.getIlk(candidate).isPassable() && visited.get(candidate) == null) {
                    nextRound.put(candidate, currentDepth);
                }
            }
        }

        return findShortestPath(nextRound, ++currentDepth, t2);
    }

    protected List<Tile> getShortestPathFromMap(Map<Tile, Integer> visited, Tile t2) {
        List<Tile> result = new LinkedList<Tile>();

        result.add(t2);
        int step = 999999;
        if (debug) {
            debug(showMap(visited));
        }

        while(step > 1) {
            Tile target = null;
            int smallest = 999999;

            for (Aim aim : Aim.values()) {
                Tile current = result.get(0);
                Tile candidate = getAnts().getTile(current, aim);

                if (candidate != null && visited.get(candidate) != null && smallest > visited.get(candidate)) {
                    target = candidate;
                    smallest = visited.get(candidate);
                    step = smallest;
                }
            }

            result.add(0, target);
        }

        return result;
    }

    private String showMap(Map<Tile, Integer> visited) {
        StringBuilder result = new StringBuilder();

        for (int rows = 0; rows < getAnts().getRows(); rows++) {
            for (int cols = 0; cols < getAnts().getCols(); cols++) {
                Tile tile = new Tile(cols, rows);

                if (visited.get(tile)!=null) {
                    int value = visited.get(tile);

                    while (value > 9) {
                        value -= 10;
                    }

                    result.append(value);
                } else {
                    result.append(".");
                }
            }
            result.append("\n");
        }

        return result.toString();
    }

    protected void sortByDistance(final Tile target, List<Tile> candidates) {
        Collections.sort(candidates, new Comparator<Tile>() {
            public int compare(Tile o1, Tile o2) {
                Integer d1 = getAnts().getDistance(target, o1);
                Integer d2 = getAnts().getDistance(target, o2);

                return d1.compareTo(d2);
            }
        });
    }

    protected Tile findClosestAntWithinViewingDistance(List<Tile> candidates, Tile tile) {
        Tile result = null;
        List<Tile> withinDistance = findAntsWithinViewingDistance(candidates, tile);

        sortByDistance(tile, withinDistance);

        if (!withinDistance.isEmpty()) {
            result = withinDistance.get(0);
        }

        return result;
    }

    protected List<Tile> findAntsWithinViewingDistance(List<Tile> candidates, Tile tile) {
        List<Tile> result = new LinkedList<Tile>();

        for(Tile c : candidates) {
            if (ants.getDistance(tile, c) < ants.getViewRadius2()) {
                result.add(c);
            }
        }

        return result;
    }

    protected Aim getTileFurtherstAwayFromMyAnts(Tile tile) {
        Set<Tile> ants = getAnts().getMyAnts();
        final List<Tile> close = new LinkedList<Tile>();

        // remove target
        ants.remove(tile);

        for (Tile ant : ants) {
            if (getAnts().getDistance(tile, ant) < getAnts().getViewRadius2()) {
                close.add(ant);
            }
        }

        if (close.isEmpty()) {
            return null;
        }

        List<Tile> candidates = new LinkedList<Tile>();

        if (goodOrder(tile, Aim.NORTH)) {
            candidates.add(getAnts().getTile(tile, Aim.NORTH));
        }
        if (goodOrder(tile, Aim.EAST)) {
            candidates.add(getAnts().getTile(tile, Aim.EAST));
        }
        if (goodOrder(tile, Aim.SOUTH)) {
            candidates.add(getAnts().getTile(tile, Aim.SOUTH));
        }
        if (goodOrder(tile, Aim.WEST)) {
            candidates.add(getAnts().getTile(tile, Aim.WEST));
        }

        if (candidates.isEmpty()) {
            return null;
        } else {
            Collections.sort(candidates, new Comparator<Tile>() {
                public int compare(Tile o1, Tile o2) {
                    return getTotalDistanceFromOtherAnts(o2, close) - getTotalDistanceFromOtherAnts(o1, close);
                }
            });

            return getAnts().getDirections(tile, candidates.get(0)).get(0);
        }
    }

    protected int getTotalDistanceFromOtherAnts(Tile tile, List<Tile> ants) {
        int result = 0;

        for (Tile ant : ants) {
            result += getAnts().getDistance(ant, tile);
        }

        if (result == 0) {
            result = 999;
        }

        return result;
    }

    protected int getDistanceFromClosestAnts(Tile tile, List<Tile> ants) {
        int result = 999;

        for (Tile ant : ants) {
            result = Math.min(result, getAnts().getDistance(ant, tile));
        }

        return result;
    }
}
