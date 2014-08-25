package edu.internet2.middleware.shibboleth.serviceprovider;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import junit.framework.TestCase;

/**
 * A base class that sets up Log4J. Real tests extend it.
 */
public class SPTestCase extends TestCase {
    
    public SPTestCase() {
        Logger root = Logger.getRootLogger();
        Layout initLayout = new PatternLayout("%d{HH:mm} %-5p %m%n");
        ConsoleAppender consoleAppender= new ConsoleAppender(initLayout,ConsoleAppender.SYSTEM_OUT);
        root.addAppender(consoleAppender);
        root.setLevel(Level.ERROR);
    }

}
