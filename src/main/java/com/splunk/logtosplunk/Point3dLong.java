package com.splunk.logtosplunk;

/**
 * Rolling our own point because we dont need decimals for position...
 */
public class Point3dLong {
    public long xCoord;
    public long yCoord;
    public long zCoord;

    /**
     * Constructs a new point.
     */
    public Point3dLong(double x, double y, double z) {
        //Noticed performance issues on using Math.Round, apparently slower than this in Java 6 according to:
        // http://stackoverflow.com/questions/12091014/what-is-the-most-efficient-way-to-round-a-float-value-to-the
        // -nearest-integer-in , maybe add in http://labs.carrotsearch.com/junit-benchmarks.html to investigate

        xCoord = (long) (x + 0.5);
        yCoord = (long) (y + 0.5);
        zCoord = (long) (z + 0.5);
    }

    @Override
    public String toString() {
        return "Point3dInt{" +
                "xCoord=" + xCoord +
                ", yCoord=" + yCoord +
                ", zCoord=" + zCoord +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Point3dLong that = (Point3dLong) o;

        if (xCoord != that.xCoord) {
            return false;
        }
        if (yCoord != that.yCoord) {
            return false;
        }
        if (zCoord != that.zCoord) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (xCoord ^ (xCoord >>> 32));
        result = 31 * result + (int) (yCoord ^ (yCoord >>> 32));
        result = 31 * result + (int) (zCoord ^ (zCoord >>> 32));
        return result;
    }
}
