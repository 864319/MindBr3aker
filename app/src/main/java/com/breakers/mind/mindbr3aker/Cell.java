package com.breakers.mind.mindbr3aker;

public class Cell {

    private int row;
    private int column;
    private boolean directions[];
    private int visited;
    private boolean blind;

    public Cell(int r, int c){
        row = r;
        column = c;
        visited = 0;
        blind = false;
    }

    public int isVisited() {
        return visited;
    }

    public void setVisited() {
        this.visited++;
    }

    public boolean isBlind() {
        return blind;
    }

    public void setBlind(boolean blind) {
        this.blind = blind;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public boolean[] getDirections() {
        return directions;
    }

    public void setDirections(int d, boolean free) {
        this.directions[d] = free;
    }
}
