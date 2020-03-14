package com.company;



import com.github.davidmoten.rtree2.geometry.Geometries;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Objects;

import static com.company.Main.orientation;

public class Triangle {
    private Point A, B, C;
    Path2D.Float polygon;

    public Triangle() {}

    public Triangle(Point A, Point B, Point C) {
        this.A = new Point(A);
        this.B = new Point(B);
        this.C = new Point(C);
    }


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
        Rectangle bounds = polygon.getBounds();
        return Geometries.rectangle(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
        //return Geometries.point(bounds.getCenterX(), bounds.getCenterY());
    }

    public boolean has(Point P) {
        return polygon.contains(P.x, P.y);
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

        polygon = new Path2D.Float();
        polygon.moveTo(A.x, A.y);
        polygon.lineTo(B.x, B.y);
        polygon.lineTo(C.x, C.y);
        polygon.lineTo(A.x, A.y);
        polygon.closePath();

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
