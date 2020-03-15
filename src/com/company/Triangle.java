package com.company;

import com.github.davidmoten.rtree2.geometry.Geometries;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Objects;

import static com.company.Main.orientation;

public class Triangle {
    private Point A, B, C;

    public Triangle() {}

    public Point notIn(Triangle t1) {
        if(!t1.contains(A))
            return A;
        if(!t1.contains(B))
            return B;
        if(!t1.contains(C))
            return C;
        return null;
    }

    public ArrayList<Point> getConnected(Triangle adjacent) {
        ArrayList<Point> connected = new ArrayList<>();
        if(contains(adjacent.getA())) {
            connected.add(adjacent.getA());
        }
        if(contains(adjacent.getB())) {
            connected.add(adjacent.getB());
        }
        if(contains(adjacent.getC())) {
            connected.add(adjacent.getC());
        }
        return connected;
    }

    public com.github.davidmoten.rtree2.geometry.Rectangle getBounds() {
        Rectangle bounds = getPolygon().getBounds();
        return Geometries.rectangle(bounds.x - Main.offsetX,
                bounds.y - Main.offsetY,
                bounds.x - Main.offsetX + bounds.width,
                bounds.y - Main.offsetY + bounds.height);
    }

    private Path2D.Float getPolygon() {
        Path2D.Float polygon = new Path2D.Float();
        polygon.moveTo(A.x, A.y);
        polygon.lineTo(B.x, B.y);
        polygon.lineTo(C.x, C.y);
        polygon.lineTo(A.x, A.y);
        polygon.closePath();
        return polygon;
    }

    public boolean has(Point P) {
        return getPolygon().contains(P.x, P.y);
    }

    public int set(Point A, Point B, Point C) {

        int orientation = orientation(A, B, C);
        if (orientation == 0)
            return -1;

        if(orientation == 1) {
            Point temp = C;
            C = B;
            B = temp;
        }

        this.A = A;
        this.B = B;
        this.C = C;

        return 0;
    }
    public boolean contains(Point T) {
        return A.equals(T) || B.equals(T) || C.equals(T);
    }

    public Point getA() {
        return A;
    }

    public Point getB() {
        return B;
    }

    public Point getC() {
        return C;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triangle triangle = (Triangle) o;
        return getConnected(triangle).size() == 3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(A, B, C);
    }

    @Override
    public String toString() {
        return "Triangle{" +
                "A=" + A.x + "," + A.y +
                ", B=" + B.x + "," + B.y +
                ", C=" + C.x + "," + C.y +
                '}';
    }
}
