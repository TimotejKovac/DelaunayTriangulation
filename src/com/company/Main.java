package com.company;


import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.Iterables;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.mreutegg.laszip4j.LASPoint;
import com.github.mreutegg.laszip4j.LASReader;
import com.github.mreutegg.laszip4j.laslib.LASreaderPipeOn;
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
	    // write your code here


        rTree = RTree.star().create();
        //reader = new LASReader(new File("GK_374_129.laz"));
        reader = new LASReader(new File("GK_430_135.laz"));
        //reader = new LASReader(new File("GK_391_145.laz"));

        Triangle startingTriangle = getStartingTriangle();

        rTree = rTree.add(startingTriangle, startingTriangle.getBounds());
        //triangles.add(new Triangle(startingTriangle.getA(), startingTriangle.getB(), startingTriangle.getC()));


        int size = reader.getHeader().getLegacyNumberOfPointRecords();
        int count = 0;


        ArrayList<Point> realPoints = new ArrayList<>();
        for(LASPoint lasPoint : reader.getPoints()) {
            realPoints.add(new Point(lasPoint.getX(), lasPoint.getY(), lasPoint.getZ()));
        }
        Collections.shuffle(realPoints);

        System.out.println("REAL POINTS = " + realPoints.size());


        Random random = new Random();

        long start = System.currentTimeMillis();


        for (Point P : realPoints) {
        //for(Point P : realPoints.iterator().) {
        //for (LASPoint lasPoint : reader.getPoints()) {
                //for(Point point : points) {
                //Point P = new Point(point.x, point.y, point.z);
               /*Point P = new Point(lasPoint.getX(),
                        lasPoint.getY(),
                        lasPoint.getZ());*/
                //Point P = realPoints.remove(random.nextInt(realPoints.size()));

            //System.out.println("P = " + P.toString());

            //System.out.println("triangle = " + triangle);
                //Point P = realPoints.remove(0);
                getTriangle(P);

                /*if(count > 10)
                    break;
*/
                float percent = (count++ / (float) size) * 100;
                if(count % 1000 == 0) {
                    System.out.println(percent + " %");
                }
                /*if (percent > 1) {
                    //rTree.visualize(600, 600)
                      //      .save("mytree.png");
                    System.out.println("COUNT = " + count);
                    break;
                }*/
            }
        //}
        System.err.println("TIME TAKEN = " + (System.currentTimeMillis() - start));

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

        //System.out.println("Done " + triangles.size());
    }

    static int handle(Writer writer, HashMap<Point, Integer> indices, ArrayList<Integer> faces, Point P, int i) throws Exception {
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

    static HashSet<Triangle> triangles = new HashSet<>();

    private static void getTriangle(Point P) {
        List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.search(Geometries.point(P.x - offsetX, P.y - offsetY)));
        //List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.nearest(Geometries.point(P.x, P.y),1,10));
        //System.out.println("entries = " + entries.size());
        long timeStart = System.currentTimeMillis();
        boolean t1Added = false, t2Added = false, t3Added = false;
        for(Entry<Triangle, Geometry> entry : entries) {


            Triangle triangle = entry.value();

            if(triangle.contains(P))
                return;

            if (!triangle.has(P)) {
                continue;
            }

            //System.out.println("getTriangle: time taken = " + (System.currentTimeMillis() - timeStart));


            int result = 0;
            Triangle t1 = new Triangle();
            result = t1.set(triangle.getA(), P, triangle.getB());
            if(result == 0) {
                rTree = rTree.add(t1, t1.getBounds());
               // triangles.add(new Triangle(t1.getA(), t1.getB(), t1.getC()));
                t1Added = true;
            }


            Triangle t2 = new Triangle();
            result = t2.set(triangle.getB(), P, triangle.getC());
            if(result == 0) {
                rTree = rTree.add(t2, t2.getBounds());
                //triangles.add(new Triangle(t2.getA(), t2.getB(), t2.getC()));
                t2Added = true;
            }

            Triangle t3 = new Triangle();
            result = t3.set(triangle.getC(), P, triangle.getA());
            if(result == 0) {
                rTree = rTree.add(t3, t3.getBounds());
              //  triangles.add(new Triangle(t3.getA(), t3.getB(), t3.getC()));
                t3Added = true;

            }

            rTree = rTree.delete(triangle, triangle.getBounds());

            //triangles.remove(new Triangle(triangle.getA(), triangle.getB(), triangle.getC()));

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
        //rectangle.setBounds(0, 0, 1920, 1080);


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
        long timeStart = System.currentTimeMillis();

        List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.search(Geometries.point(p2.x - offsetX, p2.y - offsetY)));
        //System.out.println("entries = " + entries.size());
        for(Entry<Triangle, Geometry> entry : entries) {
            Triangle triangle = entry.value();
            if(triangle.contains(p1) && triangle.contains(p2) && !triangle.contains(not)) {
                //System.out.println("getAdjecent: time taken = " + (System.currentTimeMillis() - timeStart));
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

        //triangles.remove(new Triangle(t.getA(), t.getB(), t.getC()));
        //triangles.remove(new Triangle(adjacent.getA(), adjacent.getB(), adjacent.getC()));

        rTree = rTree.delete(t, t.getBounds());
        rTree = rTree.delete(adjacent, adjacent.getBounds());

        t.set(Pl, Pi, P);
        adjacent.set(Pl, Pj, P);

/*        if(triangles.contains(t) || triangles.contains(adjacent)) {
            throw new Exception("JFIOSAJDO");
        }
*/

        //triangles.add(new Triangle(t.getA(), t.getB(), t.getC()));
        //triangles.add(new Triangle(adjacent.getA(), adjacent.getB(), adjacent.getC()));

        rTree = rTree.add(t, t.getBounds());
        rTree = rTree.add(adjacent, adjacent.getBounds());



    }

    private static boolean isIllegal(Triangle t1, Point D) {
        //System.out.println("t1 = " + t1.toString() + ", t2 = " + t2.toString());
        Point A = t1.getA();
        Point B = t1.getB();
        Point C = t1.getC();

        //System.out.println("t1 = " + t1.toString() + ", t2 = " + t2.toString());
        double[][] matrixData = {
                {A.x - D.x, A.y - D.y, Math.pow(A.x - D.x, 2) + Math.pow(A.y - D.y, 2)},
                {B.x - D.x, B.y - D.y, Math.pow(B.x - D.x, 2) + Math.pow(B.y - D.y, 2)},
                {C.x - D.x, C.y - D.y, Math.pow(C.x - D.x, 2) + Math.pow(C.y - D.y, 2)}};
        RealMatrix realMatrix = MatrixUtils.createRealMatrix(matrixData);

        double determinant = new LUDecomposition(realMatrix).getDeterminant();

        boolean illegal = determinant > 0;
        //System.out.println("illegal = " + illegal);
        return illegal;
    }

    //https://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
    /*private static boolean isInsideTriangle(Point A, Point B, Point C, Point P) {
        int i;
        int j;
        boolean result = false;
        Point[] points = new Point[] {A, B, C};
        Point test = P;
        for (i = 0, j = points.length - 1; i < points.length; j = i++) {
            if ((points[i].y > test.y) != (points[j].y > test.y) &&
                    (test.x < (points[j].x - points[i].x) * (test.y - points[i].y) / (points[j].y-points[i].y) + points[i].x)) {
                result = !result;
            }
        }
        return result;
    }*/



   /* private static boolean isInsideTriangle(Point A, Point B, Point C, Point P) {
        //System.out.println("P = " + P.toString());
        float w1 = (A.x * (C.y - A.y) + (P.y - A.y) * (C.x - A.x) - P.x * (C.y - A.y)) / (float) ((B.y - A.y) * (C.x - A.x) - (B.x - A.x) * (C.y - A.y));
        float w2 = (P.y - A.y - w1 * (B.y - A.y)) / (float) (C.y - A.y);
        //System.out.println("w1 = " + w1 + ", w2 = " + w2);
        return w1 >= 0 && w2 >= 0 && (w1 + w2) <= 1;
    }*/
}
