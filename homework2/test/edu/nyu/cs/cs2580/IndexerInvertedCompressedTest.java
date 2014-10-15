package edu.nyu.cs.cs2580;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class IndexerInvertedCompressedTest extends TestCase {
  IndexerInvertedCompressed indexer;

  @Before
  public void setUp() throws Exception {
    indexer = new IndexerInvertedCompressed();
  }

  @After
  public void tearDown() throws Exception {

  }
}