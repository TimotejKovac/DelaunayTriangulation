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
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class Main {

    static RTree<Triangle, Geometry> rTree;
    static LASReader reader;

    public static void main(String[] args) throws Exception {
	    // write your code here


        rTree = RTree.star().create();
       // reader = new LASReader(new File("GK_430_136.laz"));
        reader = new LASReader(new File("GK_391_145.laz"));

        Triangle startingTriangle = getStartingTriangle();
        rTree = rTree.add(startingTriangle, startingTriangle.getBounds());


        Random random = new Random();
        ArrayList<Point> points = new ArrayList<>();
        for(int i = 0 ; i < 1000; i++) {
            points.add(new Point(random.nextInt(1920), random.nextInt(1080), 0));
        }


        /*points.add(new Point(297,57,0));
        points.add(new Point(137,375,0));
        points.add(new Point(174,617,0));
        points.add(new Point(373,494,0));
        points.add(new Point(459,617,0));
        points.add(new Point(492,334,0));
        points.add(new Point(576,134,0));
        points.add(new Point(658,535,0));
        points.add(new Point(695,334,0));
        points.add(new Point(695,94,0));*/


        int size = reader.getHeader().getLegacyNumberOfPointRecords();
        int count = 0;


        System.out.println("O: " + reader.getHeader().getXOffset() + " " + reader.getHeader().getYOffset() + " " + reader.getHeader().getZOffset());
        System.out.println("S: " + reader.getHeader().getXScaleFactor() + " " + reader.getHeader().getYScaleFactor() + " " + reader.getHeader().getZScaleFactor());


        //while (pointReal.size() > 0) {
            //LASPoint lasPoint = pointReal.remove(random.nextInt(pointReal.size()));
        for (LASPoint lasPoint : reader.getPoints()) {

            /*Point P = new Point((float)(lasPoint.getX() * reader.getHeader().getXScaleFactor()),
                    (float)(lasPoint.getY() * reader.getHeader().getYScaleFactor()),
                    (float)(lasPoint.getZ() * reader.getHeader().getZScaleFactor()));*/
            Point P = new Point((lasPoint.getX()),
                    (lasPoint.getY()),
                    (lasPoint.getZ()));

            //System.out.println("P = " + P.toString());

            getTriangle(P);

            float percent = (count++ / (float) size) * 100;
            //if(count % 1000 == 0) {
                System.out.println(percent + " %");
            //}
            //if (count > 1000000 || percent > 5.031) {
                //rTree.visualize(600, 600)
                  //      .save("mytree.png");
              //  break;
            //}
        }

        write("test.obj", true);
        write("test_zero.obj", false);

        //System.out.println("Done " + triangles.size());
    }

    private static void write(String filename, boolean addZ) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filename), StandardCharsets.UTF_8))) {

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


            for (Triangle triangle : saving) {
                writer.write("v " + triangle.getA().x + " " + triangle.getA().y + " " + (addZ ? triangle.getA().z : 0) + "\n");
                writer.write("v " + triangle.getB().x + " " + triangle.getB().y + " " + (addZ ? triangle.getB().z : 0) + "\n");
                writer.write("v " + triangle.getC().x + " " + triangle.getC().y + " " + (addZ ? triangle.getC().z : 0) + "\n");

            }
            for(int i = 1; i < saving.size() * 3; i+=3) {
                writer.write("f " + i + " " + (i + 1) + " " + (i + 2) + "\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getTriangle(Point P) throws Exception {
        List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.search(Geometries.point(P.x, P.y)));
        //List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.nearest(Geometries.point(P.x, P.y),1,10));
        //System.out.println("entries = " + entries.size());
        boolean t1Added = false, t2Added = false, t3Added = false;

        for (Entry<Triangle, Geometry> entry : entries) {

            Triangle triangle = entry.value();
            if (triangle.has(P)) {

                if (triangle.contains(P)) {
                    //System.out.println("OHNO!");
                    return;
                }


                /*if (P.equals(new Point(39153335, 14504901, 0))) {
                    System.out.println(P.toString());
                    System.out.println(triangle.toString());
                    throw new Exception("HERE");
                }*/


                int result = 0;
                Triangle t1 = new Triangle();
                result = t1.set(triangle.getA(), P, triangle.getB());
                if (result == 0) {
                    rTree = rTree.add(t1, t1.getBounds());
                    t1Added = true;
                }

                Triangle t2 = new Triangle();
                result = t2.set(triangle.getB(), P, triangle.getC());
                if (result == 0) {
                    rTree = rTree.add(t2, t2.getBounds());
                    t2Added = true;
                }

                Triangle t3 = new Triangle();
                result = t3.set(triangle.getC(), P, triangle.getA());
                if (result == 0) {
                    rTree = rTree.add(t3, t3.getBounds());
                    t3Added = true;
                }

                rTree = rTree.delete(triangle, triangle.getBounds());


                if (t1Added) {
                    legalizeEdge(P, triangle.getA(), triangle.getB(), t1);

                }
                if (t2Added) {
                    legalizeEdge(P, triangle.getB(), triangle.getC(), t2);

                }
                if (t3Added) {
                    legalizeEdge(P, triangle.getC(), triangle.getA(), t3);

                }

                return;

            }
        }

    }

    private static Triangle getStartingTriangle() throws Exception{
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

        System.out.println("start = " + result.toString());
        return result;
    }

    private static Triangle getAdjacent(Point p1, Point p2, Triangle not) {
        List<Entry<Triangle, Geometry>> entries = Iterables.toList(rTree.search(Geometries.point(p1.x, p1.y)));
        for(Entry<Triangle, Geometry> entry : entries) {
            Triangle triangle = entry.value();
            if(triangle.contains(p1) && triangle.contains(p2) && !triangle.equals(not))
                return triangle;
        }
        return null;
    }

    static HashSet<String> logs = new HashSet<>();

    private static void legalizeEdge(Point P, Point Pi, Point Pj, Triangle t) throws Exception {

        Triangle adjacent = getAdjacent(Pi, Pj, t);
        if(adjacent == null) {
            return;
        }


        //System.out.println("T = " + t.toString());
        //System.out.println("A = " + adjacent.toString());

        //System.out.println("orientation = " + orientation(t.getA(), t.getB(), t.getC()));
        if(isIllegal(t, adjacent.notIn(t))) {
            change(adjacent, t, P);

            ArrayList<Point> connected = adjacent.getConnected(t);
            connected.remove(P);
            Point Pl = connected.get(0);


            if(t.contains(Pi) && t.contains(Pl)) {
                legalizeEdge(P, Pi, Pl, t);
                legalizeEdge(P, Pl, Pj, adjacent);
            }
            else if(adjacent.contains(Pi) && adjacent.contains(Pl)){
                legalizeEdge(P, Pi, Pl, adjacent);
                legalizeEdge(P, Pl, Pj, t);
            }
            else  {
                throw new Exception("ERR");
            }

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
        int val = (p2.y - p1.y) * (p3.x - p2.x) -
                (p2.x - p1.x) * (p3.y - p2.y);



        if (val == 0) {
            System.out.println("COLINEAR!");
            return 0;  // colinear
        }

        if(Math.abs(val) < 0.001) {
            System.out.println("val = " + val);
        }

        // clock or counterclock wise
        return (val > 0)? 1: 2;
    }

    private static void change(Triangle adjacent, Triangle t, Point P) throws Exception {
        //System.out.println(adjacent.toString());
        //System.out.println(t.toString());

        //System.out.println(deleted.contains(adjacent) || deleted.contains(t));

        ArrayList<Point> connected = t.getConnected(adjacent);

        Point two = t.notIn(adjacent);
        Point one = adjacent.notIn(t);


        rTree = rTree.delete(t, t.getBounds());
        rTree = rTree.delete(adjacent, adjacent.getBounds());


        int result = t.set(one, connected.get(0), two);
        int result2 = adjacent.set(one, connected.get(1), two);


        /*if(t.getConnected(adjacent).size() != 2)
            throw new Exception("SHIT");

        if(orientation(t.getA(), t.getB(), t.getC()) == 0) {
            System.out.println(t.toString());
            throw new Exception("COUNTER CLOCKWISE!");
        }
        if(orientation(adjacent.getA(), adjacent.getB(), adjacent.getC()) == 0) {
            System.out.println(adjacent.toString());
            throw new Exception("COUNTER CLOCKWISE!");
        }*/



        rTree = rTree.add(t, t.getBounds());
        rTree = rTree.add(adjacent, adjacent.getBounds());
    }

    private static boolean isIllegal(Triangle t1, Point D) throws Exception{
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
