package org.apache.fulcrum.parser.pool;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.fulcrum.parser.DefaultCookieParser;
import org.apache.fulcrum.parser.ParserService;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



/**
 * Test the CookieParserFactory and CookieParserPool classes.
 *
 * @author <a href="mailto:painter@apache.org">Jeffery Painter</a>
 * @version $Id: CookieParserPoolTest.java 222043 2019-01-17 08:17:33Z painter $
 */
public class CookieParserPoolTest extends BaseUnit5Test 
{
	private DefaultCookieParser parser;
    private ParserService parserService;
    
    /** 
     * Use commons pool to manage value parsers 
     */
    private CookieParserPool cookieParserPool;

    /**
     * Performs any initialization that must happen before each test is run.
     * @throws Exception if parser service not found
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        try
        {
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            

    		// Define the default configuration
    		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    		config.setMaxIdle(1);
    	    config.setMaxTotal(1);

    	    // init the pool
    	    cookieParserPool 
    	    	= new CookieParserPool(new CookieParserFactory(), config);

        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Clean up after each test is run.
     */
    @AfterEach
    public void tearDown()
    {
        // pool object already explicitely released by call to returnObject in test
        // will throw java.lang.IllegalStateException, as pool is external to parserService
        //parserService.putParser(parser);
        this.release(parserService);
    }
    
    /**
     * @throws Exception generic exception
     */
    @Test
    public void testFactoryMethods() throws Exception 
    {
    	try
    	{
    		// borrow a new parser and assign it to the parser service
    		parser = cookieParserPool.borrowObject();
    		parser.setParserService(parserService);
    		
            // Populate parser with mock servlet data
            HttpServletRequest request = getMockRequest();
            HttpServletResponse response = mock(HttpServletResponse.class);
            parser.setData(request, response);
    		
    		// test setting cookies
    		parser.set("test1", "val1");

    		// clear the parser for reset
    		parser.clear();
    		assertTrue(parser.isValid());
    		
    		cookieParserPool.returnObject( parser );
    		
    	} catch ( Exception e )
    	{
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

}
