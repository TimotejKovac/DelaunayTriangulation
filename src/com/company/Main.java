package com.company;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.Iterables;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class Main {

    static RTree<Triangle, Geometry> rTree;
    static LASReader reader;

    static float offsetX ;
    static float offsetY;

    public static void main(String[] args) {

        // Use RTree for improved performance of searching already established triangles.
        rTree = RTree.star().create();

        // Time needed for Delaunay triangulation of file GK_430_136.laz which contained 12M points was 40 minutes on Intel i7 6700K.
        reader = new LASReader(new File("GK_430_135.laz"));

        Triangle startingTriangle = getStartingTriangle();
        rTree = rTree.add(startingTriangle, startingTriangle.getBounds());

        int size = reader.getHeader().getLegacyNumberOfPointRecords();
        int count = 0;

        // Randomize the points to give the R-tree a more scattered data.
        ArrayList<Point> realPoints = new ArrayList<>();
        for(LASPoint lasPoint : reader.getPoints()) {
            realPoints.add(new Point(lasPoint.getX(), lasPoint.getY(), lasPoint.getZ()));
        }
        Collections.shuffle(realPoints);

        long start = System.currentTimeMillis();

        for (Point P : realPoints) {
            handlePoint(P);

            float percent = (count++ / (float) size) * 100;

            if(count % 1000 == 0) {
                System.out.println(percent + " %");
            }
        }

        System.out.println("TIME TAKEN = " + (System.currentTimeMillis() - start));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("test.obj"), StandardCharsets.UTF_8))) {

            ArrayList<Triangle> saving = new ArrayList<>();
            Triangle starting = getStartingTriangle();

            Iterable<Entry<Triangle, Geometry>> entries = rTree.entries();
            for(Entry<Triangle, Geometry> entry : entries) {
                Triangle triangle = entry.value();
                if(triangle.contains(starting.getA()) || triangle.contains(starting.getB()) || triangle.contains(starting.getC()))  {
                    continue;
                }
                saving.add(triangle);
            }
            rTree = null;

            System.out.println("Saving size = " + saving.size());

            HashMap<Point, Integer> indices = new HashMap<>();

            ArrayList<Integer> faces = new ArrayList<>();
            int i = 1;
            for (Triangle triangle : saving) {
                i = handle(writer, indices, faces, triangle.getA(), i);
                i = handle(writer, indices, faces, triangle.getB(), i);
                i = handle(writer, indices, faces, triangle.getC(), i);
            }
            for(i = 0; i < faces.size(); i+=3) {
                writer.write("f " + faces.get(i) + " " + faces.get(i + 1) + " " + faces.get(i + 2) + "\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int handle(Writer writer, HashMap<Point, Integer> indices, ArrayList<Integer> faces, Point P, int i) throws Exception {
        // Check if this vertex has already been written and just reference that index. Else write it in the file.
        if(!indices.containsKey(P)) {
            writer.write("v " + P.x + " " + P.y + " " + P.z + "\n");
            indices.put(P, i);
            faces.add(i);
            i++;
        }
        else {
            int index = indices.get(P);
            faces.add(index);
        }
        return i;
    }
    
    private static void handlePoint(Point P) {
        List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.search(Geometries.point(P.x - offsetX, P.y - offsetY)));
        boolean t1Added = false, t2Added = false, t3Added = false;
        for(Entry<Triangle, Geometry> entry : entries) {

            Triangle triangle = entry.value();

            // If this dot with it's x and y is already part of the triangle this point is a duplicate and will not be added.
            if(triangle.contains(P))
                return;

            // If the triangle does not contain the point continue with the search.
            if (!triangle.has(P)) {
                continue;
            }

            int result;
            Triangle t1 = new Triangle();
            result = t1.set(triangle.getA(), P, triangle.getB());
            if(result == 0) {
                rTree = rTree.add(t1, t1.getBounds());
                t1Added = true;
            }


            Triangle t2 = new Triangle();
            result = t2.set(triangle.getB(), P, triangle.getC());
            if(result == 0) {
                rTree = rTree.add(t2, t2.getBounds());
                t2Added = true;
            }

            Triangle t3 = new Triangle();
            result = t3.set(triangle.getC(), P, triangle.getA());
            if(result == 0) {
                rTree = rTree.add(t3, t3.getBounds());
                t3Added = true;

            }

            // Delete the parent
            rTree = rTree.delete(triangle, triangle.getBounds());

            // Legalize adjacent edges of all the added triangles.
            if(t1Added)
                legalizeEdge(P, triangle.getA(), triangle.getB(), t1);
            if(t2Added)
                legalizeEdge(P, triangle.getB(), triangle.getC(), t2);
            if(t3Added)
                legalizeEdge(P, triangle.getC(), triangle.getA(), t3);

            return;
        }
    }

    private static Triangle getStartingTriangle() {
        double width = ((reader.getHeader().getMaxX() - reader.getHeader().getMinX())) * (1 / reader.getHeader().getXScaleFactor());
        double height = ((reader.getHeader().getMaxY() - reader.getHeader().getMinY()) * (1 / reader.getHeader().getYScaleFactor()));

        Rectangle rectangle = new Rectangle();
        rectangle.setRect(reader.getHeader().getMinX() * (1 / reader.getHeader().getXScaleFactor()),
                (reader.getHeader().getMinY() * (1 / reader.getHeader().getYScaleFactor())),
                width,
                height);

        // The largest triangle is computed as being twice the width and twice the height of the bounding box of the area of points.
        Triangle result = new Triangle();
        result.set(new Point((int)(rectangle.getCenterX() - rectangle.getWidth()), (int)(rectangle.getY()), 0),
                new Point((int)(rectangle.getCenterX() + rectangle.getWidth()), (int)(rectangle.getY()), 0),
                new Point((int)rectangle.getCenterX(), (int)(rectangle.getY() + 2 * rectangle.getHeight()),0));
        com.github.davidmoten.rtree2.geometry.Rectangle bounds = result.getBounds();
        offsetX = (float) bounds.x1();
        offsetY = (float) bounds.y1();
        return result;
    }


    private static Triangle getAdjacent(Point p1, Point p2, Point not) {
        List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.search(Geometries.point(p2.x - offsetX, p2.y - offsetY)));
        for(Entry<Triangle, Geometry> entry : entries) {
            Triangle triangle = entry.value();
            if(triangle.contains(p1) && triangle.contains(p2) && !triangle.contains(not)) {
                return triangle;
            }
        }
        return null;
    }

    private static void legalizeEdge(Point P, Point Pi, Point Pj, Triangle t) {

        Triangle adjacent = getAdjacent(Pi, Pj, P);
        if(adjacent == null) {
            return;
        }

        Point Pl = adjacent.notIn(t);
        if(isIllegal(t, Pl)) {
            change(adjacent, t, P, Pi, Pj, Pl);

            ArrayList<Point> connected = adjacent.getConnected(t);
            connected.remove(P);
            Pl = connected.get(0);
            // This might not be needed but because "recursion" I added it anyway.
            connected = null;

            legalizeEdge(P, Pi, Pl, t);
            legalizeEdge(P, Pl, Pj, adjacent);
        }
    }

    // https://www.geeksforgeeks.org/orientation-3-ordered-points/
    // To find orientation of ordered triplet
    // (p1, p2, p3). The function returns
    // following values
    // 0 --> p, q and r are colinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    public static int orientation(Point p1, Point p2, Point p3) {
        // See 10th slides from following link
        // for derivation of the formula
        float val = (p2.y - p1.y) * (p3.x - p2.x) -
                (p2.x - p1.x) * (p3.y - p2.y);

        if (val == 0) return 0;  // colinear

        // clock or counterclock wise
        return (val > 0)? 1: 2;
    }

    private static void change(Triangle adjacent, Triangle t, Point  P, Point Pi, Point Pj, Point Pl) {
        // Remove both triangles from the tree
        rTree = rTree.delete(t, t.getBounds());
        rTree = rTree.delete(adjacent, adjacent.getBounds());

        // Change connected points
        t.set(Pl, Pi, P);
        adjacent.set(Pl, Pj, P);

        // Add them back into the tree
        rTree = rTree.add(t, t.getBounds());
        rTree = rTree.add(adjacent, adjacent.getBounds());
    }

    private static boolean isIllegal(Triangle t1, Point D) {
        Point A = t1.getA();
        Point B = t1.getB();
        Point C = t1.getC();

        // If the determinant of this matrix is positive the edge is illegal.
        double[][] matrixData = {
                {A.x - D.x, A.y - D.y, Math.pow(A.x - D.x, 2) + Math.pow(A.y - D.y, 2)},
                {B.x - D.x, B.y - D.y, Math.pow(B.x - D.x, 2) + Math.pow(B.y - D.y, 2)},
                {C.x - D.x, C.y - D.y, Math.pow(C.x - D.x, 2) + Math.pow(C.y - D.y, 2)}};
        RealMatrix realMatrix = MatrixUtils.createRealMatrix(matrixData);

        double determinant = new LUDecomposition(realMatrix).getDeterminant();

        return determinant > 0;
    }

}
