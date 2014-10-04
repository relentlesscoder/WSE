package edu.nyu.cs.cs2580;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoggingHandler implements HttpHandler {

  private static final Logger logger = LogManager
      .getLogger(LoggingHandler.class);

  @Override
  public void handle(HttpExchange exchange) throws IOException {

  }

}
