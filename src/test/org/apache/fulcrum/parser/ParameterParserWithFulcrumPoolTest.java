package org.apache.fulcrum.parser;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.avalon.framework.component.ComponentException;
import org.junit.jupiter.api.BeforeEach;

/**
 * Basic test that ParameterParser instantiates.
 *
 * @author <a href="epugh@opensourceconnections.com">Eric Pugh</a>
 * @version $Id: ParameterParserTest.java 1848895 2018-12-13 21:04:26Z painter $
 */
public class ParameterParserWithFulcrumPoolTest extends ParameterParserTest
{

    @Override
    @BeforeEach
    public void setUpBefore() throws Exception
    {
        try
        {
            setConfigurationFileName("src/test/TestComponentConfigWithFulcrumPool.xml");
            
            setRoleFileName( "src/test/TestRoleConfigWithFulcrumPool.xml");
            
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            parameterParser = parserService.getParser(DefaultParameterParser.class);

        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
