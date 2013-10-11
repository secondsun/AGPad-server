import org.jboss.aerogear.agpad.handler.PadHandler
import org.jboss.aerogear.agpad.vo.Pair
import org.junit.Assert
import org.junit.Test


class PadHandlerTests {

    String padName = UUID.randomUUID().toString();
    PadHandler handler = new PadHandler()
    String username = 'testUser'
    @Test
    void createPad() {

        def pad = handler.createPad("{'ownerName':'$username', 'name':'$padName', 'content':''}")
        Assert.assertNotNull(pad);
        Assert.assertNotNull(pad._id)
    }

    @Test
    void createSession() {
        def sessionId = handler.createSession(padName, username);
        Assert.assertNotNull(sessionId);
        Assert.assertNotNull(handler.openPads.get(new Pair(first: padName, second: username)))

        def pad = handler.openPads.get(new Pair(first: padName, second: username));
        Assert.assertNotNull(handler.padToShadows.get(pad))
        Assert.assertEquals(1, handler.padToShadows.get(pad).size())
        Assert.assertEquals(handler.padToShadows.get(pad).get(0), handler.sessionToPadShadows.get(sessionId))
    }

    @Test
    void closeSession() {
        Assert.fail('not implements')
    }

    @Test
    void applyDiffs() {
        Assert.fail('not implements')
    }
}
