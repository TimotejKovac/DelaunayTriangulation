package com.company;

import java.util.Objects;

public class Point {

    public float x, y, z;

    public Point(Point p) {
        this.x = p.x;
        this.y = p.y;
        this.z = p.z;
    }

    public Point(float x, float y, float z) {
        this.x = x;
        this.y = y;
        //this.z = 0;
        this.z = z;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x &&
                y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
