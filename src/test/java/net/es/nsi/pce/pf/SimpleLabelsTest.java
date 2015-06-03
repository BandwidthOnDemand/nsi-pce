/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class SimpleLabelsTest {

    public SimpleLabelsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of fromString method, of class SimpleLabels.
     */
    @Test
    public void testFromString() {

    }

    /**
     * Test of toString method, of class SimpleLabels.
     */
    @Test
    public void testToString() {
        SimpleStp stp = new SimpleStp("urn:ogf:network:cipo.rnp.br:2013:testbed:PMW:ge-2_3_4:+:out?vlan=1600,1800-1803,1805,1808-1809,1900-1915,2000,2004");
        String labels = SimpleLabels.toString(stp.getLabels());
        System.out.println(labels);
    }

    /**
     * Test of sortLabels method, of class SimpleLabels.
     */
    @Test
    public void testSortLabels() {

    }

    /**
     * Test of equals method, of class SimpleLabels.
     */
    @Test
    public void testEquals() {

    }

    /**
     * Test of contains method, of class SimpleLabels.
     */
    @Test
    public void testContains() {

    }

    /**
     * Test of stringToIntegerSet method, of class SimpleLabels.
     */
    @Test
    public void testStringToIntegerSet() {

    }

}
