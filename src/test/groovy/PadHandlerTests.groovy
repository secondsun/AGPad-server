import org.jboss.aerogear.agpad.PadMergeUtil
import org.jboss.aerogear.agpad.handler.PadHandler
import org.jboss.aerogear.agpad.vo.Pad
import org.jboss.aerogear.agpad.vo.PadDiff
import org.jboss.aerogear.agpad.vo.Pair
import org.junit.Assert
import org.junit.Test


class PadHandlerTests {


    PadHandler handler = new PadHandler()
    String username = 'testUser'
    @Test
    void createPad() {
        String padName = UUID.randomUUID().toString();
        def pad = handler.createPad("""{"ownerName":"$username", "name":"$padName", "content":""}""")
        Assert.assertNotNull(pad);
        Assert.assertNotNull(pad._id)
    }

    @Test
    void createSession() {
        String padName = UUID.randomUUID().toString();
        handler.createPad("""{"ownerName":"$username", "name":"$padName", "content":""}""")
        def sessionId = handler.createSession(padName, username);
        Assert.assertNotNull(sessionId);
        Assert.assertNotNull(handler.openPads.get(new Pair(first: padName, second: username)))

        def pad = handler.openPads.get(new Pair(first: padName, second: username));
        Assert.assertNotNull(handler.padToShadows.get(pad))
        Assert.assertEquals(1, handler.padToShadows.get(pad).size())
        Assert.assertEquals(handler.padToShadows.get(pad).iterator().next(), handler.sessionToPadShadows.get(sessionId).second)
    }

    @Test
    void closeSession() {
        String padName = UUID.randomUUID().toString();
        handler.createPad("""{"ownerName":"$username", "name":"$padName", "content":""}""")
        def sessionId = handler.createSession(padName, username);
        handler.closeSession(sessionId);



        def pad = handler.openPads.get(new Pair(first: padName, second: username));
        Assert.assertNotNull(handler.padToShadows.get(pad))
        Assert.assertEquals(0, handler.padToShadows.get(pad).size())
        Assert.assertNull(handler.sessionToPadShadows.get(sessionId));
    }

    @Test
    void testDMP() {
        String text1 = """Hamlet: Do you see yonder cloud that's almost in shape of a camel?
Polonius: By the mass, and 'tis like a camel, indeed.
Hamlet: Methinks it is like a weasel.
Polonius: It is backed like a weasel.
Hamlet: Or like a whale?
Polonius: Very like a whale.
-- Shakespeare"""
        String text2 = """Hamlet: Do you see the cloud over there that's almost the shape of a camel?
Polonius: By golly, it is like a camel, indeed.
Hamlet: I think it looks like a weasel.
Polonius: It is shaped like a weasel.
Hamlet: Or like a whale?
Polonius: It's totally like a whale.
-- Shakespeare"""
        String expectedPatch = "@@ -16,21 +16,29 @@\n see \n-yonder\n+the\n  cloud \n+over there \n that\n@@ -47,18 +47,19 @@\n  almost \n-in\n+the\n  shape o\n@@ -86,24 +86,18 @@\n  By \n-the mass, and 't\n+golly, it \n is l\n@@ -129,21 +129,23 @@\n et: \n-Me\n+I \n think\n-s\n  it \n-i\n+look\n s li\n@@ -177,12 +177,12 @@\n  is \n-back\n+shap\n ed l\n@@ -234,11 +234,19 @@\n us: \n-Ver\n+It's totall\n y li\n"
        def dmp = new name.fraser.neil.plaintext.diff_match_patch();
        String actualDiff = dmp.patch_toText(dmp.patch_make(text1, text2));
        Assert.assertEquals(expectedPatch, actualDiff);

    }

    @Test
    void applyDiffs() {
        Pad serverText = new Pad(content:"""Kirk: Do you see yonder cloud that's almost in shape of a Klingon?
Spock: By the mass, and 'tis like a Klingon, indeed.
Kirk: Methinks it is like a Vulcan.
Spock: It is backed like a Vulcan.
Kirk: Or like a Romulan?
Spock: Very like a Romulan.
-- Trekkie""")
        Pad serverShadow = new Pad(content: """Hamlet: Do you see yonder cloud that's almost in shape of a camel?
Polonius: By the mass, and 'tis like a camel, indeed.
Hamlet: Methinks it is like a weasel.
Polonius: It is backed like a weasel.
Hamlet: Or like a whale?
Polonius: Very like a whale.
-- Shakespeare""")
        PadDiff clientDiffSet = new PadDiff(md5: "ac2a0714bb7b392fbbf9043d031938d4", diff: "@@ -16,21 +16,29 @@\n see \n-yonder\n+the\n  cloud \n+over there \n that\n@@ -47,18 +47,19 @@\n  almost \n-in\n+the\n  shape o\n@@ -86,24 +86,18 @@\n  By \n-the mass, and 't\n+golly, it \n is l\n@@ -129,21 +129,23 @@\n et: \n-Me\n+I \n think\n-s\n  it \n-i\n+look\n s li\n@@ -177,12 +177,12 @@\n  is \n-back\n+shap\n ed l\n@@ -234,11 +234,19 @@\n us: \n-Ver\n+It's totall\n y li\n")
        PadDiff expectedDiffRefult = new PadDiff(diff:"@@ -1,14 +1,12 @@\n-Hamlet\n+Kirk\n : Do you\n@@ -64,23 +64,22 @@\n f a \n-camel?%0APolonius\n+Klingon?%0ASpock\n : By\n@@ -103,13 +103,15 @@\n e a \n-camel\n+Klingon\n , in\n@@ -116,22 +116,20 @@\n indeed.%0A\n-Hamlet\n+Kirk\n : I thin\n@@ -150,24 +150,21 @@\n e a \n-weasel.%0APolonius\n+Vulcan.%0ASpock\n : It\n@@ -185,49 +185,46 @@\n e a \n-weasel.%0AHamlet: Or like a whale?%0APolonius\n+Vulcan.%0AKirk: Or like a Romulan?%0ASpock\n : It\n@@ -245,25 +245,23 @@\n e a \n-whale.%0A-- Shakespear\n+Romulan.%0A-- Trekki\n e\n", md5: "60775a9be9b8e041d8fee7ace3dbf9d3");

        PadDiff resultDiff = PadMergeUtil.updateAndDiffShadow(serverText, serverShadow, clientDiffSet);
        Assert.assertEquals(expectedDiffRefult.md5, resultDiff.md5)
        Assert.assertEquals(expectedDiffRefult.diff, resultDiff.diff)

    }
}


