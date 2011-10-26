/**
 * Represents type of tile on the game map.
 */
public enum Ilk {
    /** Water tile. */
    WATER("W"),
    
    /** Food tile. */
    FOOD("F"),
    
    /** Land tile. */
    LAND("."),
    
    /** Dead ant tile. */
    DEAD("X"),
    
    /** My ant tile. */
    MY_ANT("A"),
    
    /** Enemy ant tile. */
    ENEMY_ANT("E");

    String symbol;

    Ilk(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Checks if this type of tile is passable, which means it is not a water tile.
     * 
     * @return <code>true</code> if this is not a water tile, <code>false</code> otherwise
     */
    public boolean isPassable() {
        return this.equals(LAND) || this.equals(DEAD) || this.equals(FOOD) || this.equals(MY_ANT) || this.equals(ENEMY_ANT);
    }
    
    /**
     * Checks if this type of tile is unoccupied, which means it is a land tile or a dead ant tile.
     * 
     * @return <code>true</code> if this is a land tile or a dead ant tile, <code>false</code>
     *         otherwise
     */
    public boolean isUnoccupied() {
        return this.equals(LAND) || this.equals(DEAD);
    }

    public String toString() {
        return symbol;
    }
}
