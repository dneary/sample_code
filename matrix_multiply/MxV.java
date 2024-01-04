/**
 * Copyright (C) 2023 Ampere Computing LLC. 
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  1. Redistributions of source code must retain the above copyright notice, this list 
 *     of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice, this 
 *     list of conditions and the following disclaimer in the documentation and/or other 
 *     materials provided with the distribution.
 *  3. Neither the name of the copyright holder nor the names of its contributors may 
 *     be used to endorse or promote products derived from this software without specific 
 *     prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH 
 * DAMAGE.
 */

import java.io.PrintStream;
import java.text.NumberFormat;

/** Multiply two matrices, in spite of the name based on "matrix times vector". */
public class MxV {

    /** A start time initialized when this class is loaded. */
    final static long startNanos = System.nanoTime();

    /** What to do with each row-column component */
    long multiplyAdd(long partial, long left, long right) {
        long result = partial + (left * right);
        return result;
    }

    /** Compute one cell of the product. */
    void oneCell(long[][] product, long[][] left, long[][] right, int row, int column) {
        final int rightRows = rowCount(right);
        long partial = 0;
        for (int i = 0; i < rightRows; i += 1) {
            partial = multiplyAdd(partial, left[row][i], right[i][column]);
        }
        product[row][column] = partial;
    }

    /** Matrix multiply. */
    void multiply(long[][] product, long[][] left, long[][] right) {
        final int leftRows = rowCount(left);
        final int leftColumns = columnCount(left);
        final int rightRows = rowCount(right);
        final int rightColumns = columnCount(right);
        if (leftColumns != rightRows) {
            throw new RuntimeException("leftRows (" + leftRows + ") != rightColumns (" + rightColumns + ")");
        }
        final int resultRows = leftRows;
        final int resultColumns = rightColumns;
        for (int row = 0; row < resultRows; row += 1) {
            for (int column = 0; column < resultColumns; column += 1) {
                oneCell(product, left, right, row, column);
            }
        }
    }

    public static void main(String[] arg) {
        // Do not use static methods.
        MxV instance = new MxV();

        // Default values for command-line arguments.
        final int DEFAULT_ROWS        = 5000;
        final int DEFAULT_COLUMNS     = 4000;
        final int DEFAULT_REPETITIONS = 10;
        final int DEFAULT_SLEEPMILLIS = 900;

        // Sleep to separate JVM startup from application work.
        try {
            Thread.sleep(1200);
        } catch (InterruptedException ie) {
            // Nothing to do.
        }
        // Parse arguments:
        int rows = DEFAULT_ROWS;
        if (arg.length > 0) {
            if (arg[0].equals("-m")) {
                try {
                    rows = Integer.parseInt(arg[1]);
                } catch (NumberFormatException nfe) {
                    /* Nothing to do: rows is set correctly. */
                }
            }
        }
        int columns = DEFAULT_COLUMNS;
        if (arg.length > 2) {
            if (arg[2].equals("-n")) {
                try {
                    columns = Integer.parseInt(arg[3]);
                } catch (NumberFormatException nfe) {
                    /* Nothing to do: columns is set correctly. */
                }
            }
        }
        int repetitions = DEFAULT_REPETITIONS;
        if (arg.length > 4) {
            if (arg[4].equals("-r")) {
                try {
                    repetitions = Integer.parseInt(arg[5]);
                } catch (NumberFormatException nfe) {
                    /* Nothing to do: repetitions is set correctly. */
                }
            }
        }
        int sleepMillis = DEFAULT_SLEEPMILLIS;
        if (arg.length > 6) {
            if (arg[6].equals("-s")) {
                try {
                    sleepMillis = Integer.parseInt(arg[7]);
                } catch (NumberFormatException nfe) {
                    /* Nothing to do: repetitions is set correctly. */
                }
            }
        }
        instance.test(rows, columns, repetitions, sleepMillis);

        // Sleep to separate application work from JVM shutdown.
        try {
            Thread.sleep(1200);
        } catch (InterruptedException ie) {
            // Nothing to do.
        }
     }

    void test(int rows, int columns, int repetitions, int sleepMillis) {
        // Create matrices using the row and column from the command line.
        //   Initialized to 1, so that the expected result is easy to compute.
        final int leftRows       = rows;
        final int leftColumns    = columns;
        final int rightRows      = columns;
        final int rightColumns   = 1;
        final int productRows    = leftRows;
        final int productColumns = rightColumns;
        long[][] left            = allocate(leftRows, leftColumns, 1);
        long[][] right           = allocate(rightRows, rightColumns, 1);
        long[][] expectedResult  = allocate(productRows, productColumns, columns);

        // Print out the command line arguments.
        System.out.printf("MxV:");
        System.out.printf("  rows: %d", rows);
        System.out.printf("  columns: %d", columns);
        System.out.printf("  repetitions: %d", repetitions);
        System.out.printf("  sleepMillis: %d", sleepMillis);
        System.out.printf("\n");

        // Allocate the product matrix.
        long[][] product        = allocate(leftRows, rightColumns, 0);

        // Time many matrix multiplies.
        for (int repeat = 0; repeat < repetitions; repeat += 1) {
            if (sleepMillis > 0) {
                // Sleep to separate multiplies in the profile.
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException ie) {
                    // Nothing to do.
                }
            }
            final long repetitionNanos = System.nanoTime() - startNanos;
            final long deltaNanos = timeMultiply(product, left, right);
            System.out.printf("  repetition: %2d", repeat);
            System.out.printf("  started at: %12d nanoseconds", repetitionNanos);
            System.out.printf("  took: %10d nanoseconds", deltaNanos);
            System.out.printf("\n");
        }

        verify(System.err, expectedResult, product);
    }

    long timeMultiply(long[][] product, long[][] left, long[][] right) {
        // Initialize the product matrix.
        initialize(product, 0);

        // Time the multiply.
        final long beforeNanos = System.nanoTime();
        multiply(product, left, right);
        final long afterNanos = System.nanoTime();

        final long deltaNanos = afterNanos - beforeNanos;
        return deltaNanos;
    }

    /** Allocate and initialize a matrix. */
    long[][] allocate(int rows, int columns, int value) {
        long[][] result = new long[rows][];
        for (int row = 0; row < rows; row += 1) {
            result[row] = new long[columns];
        }
        return initialize(result, value);
    }

    /** Initialize a matrix. */
    long[][] initialize(long[][] result, int value) {
        int resultRows = rowCount(result);
        int resultColumns = columnCount(result);
        for (int row = 0; row < resultRows; row += 1) {
            for (int column = 0; column < resultColumns; column += 1) {
                result[row][column] = value;
            }
        }
        return result;
    }

    /** Display a matrix.  Do not use for large matrices. */
    void display(PrintStream out, long[][] matrix) {
        final int rows = rowCount(matrix);
        final int columns = columnCount(matrix);
        out.printf("  matrix:  rows: %d X columns: %d\n", rows, columns);
        for (int row = 0; row < rows; row += 1) {
            out.printf("    ");
            for (int column = 0; column < columns; column += 1) {
                out.printf("  %4d", matrix[row][column]);
            }
            out.printf("\n");
        }
    }

    /** Verify that a matrix matches the expected value. */
    boolean verify(PrintStream out, long[][] expected, long[][] matrix) throws RuntimeException {
        final int expectedRows = rowCount(expected);
        final int expectedColumns = columnCount(expected);
        final int matrixRows = rowCount(matrix);
        final int matrixColumns = columnCount(matrix);
        if (expectedRows != matrixRows) {
            throw new RuntimeException("verify fails:" +
                                       " expectedRows: " + expectedRows +
                                       " != " +
                                       " matrixRows: " + matrixRows);
        }
        if (expectedColumns != matrixColumns) {
            throw new RuntimeException("verify fails:" +
                                       " expectedColumns: " + expectedColumns +
                                       " != " +
                                       " matrixColumns: " + matrixColumns);
        }
        for (int row = 0; row < expectedRows; row += 1) {
            for (int column = 0; column < expectedColumns; column += 1) {
                if (expected[row][column] != matrix[row][column]) {
                    out.printf("  verify fails at");
                    out.printf(" expected[ %d ][ %d ]: %d",
                               row, column, expected[row][column]);
                    out.printf(" != ");
                    out.printf(" matrix[ %d ][ %d ]: %d",
                               row, column, matrix[row][column]);
                    out.printf("\n");
                    throw new RuntimeException("verify fails at" +
                                               " matrix[ " + row + " ][ " + column + " ]: " +
                                               matrix[row][column]);
                }
            }
        }
        return true;
    }

    int rowCount(long[][] matrix) {
        return matrix.length;
    }

    int columnCount(long[][] matrix) {
        return matrix[0].length;
    }
}
