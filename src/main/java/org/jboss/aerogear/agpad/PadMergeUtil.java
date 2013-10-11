package org.jboss.aerogear.agpad;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.jboss.aerogear.agpad.vo.Pad;
import org.jboss.aerogear.agpad.vo.PadDiff;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

public class PadMergeUtil {


    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     *
     * @param pad
     * @param shadow
     * @param incomingDiff
     * @throws OutOfSyncException
     * @return
     */
    public static PadDiff updateAndDiffShadow(Pad pad, Pad shadow, PadDiff incomingDiff) {
        synchronized (shadow) {
            diff_match_patch dmp = new diff_match_patch();

            String text2 = shadow.getContent();
            LinkedList<Patch> patch = new LinkedList<>(dmp.patch_fromText(incomingDiff.getDiff()));

            Object[] results = dmp.patch_apply(patch, text2);
            String shadowTextPostPatch = results[0].toString();
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] digest = md5.digest(shadowTextPostPatch.getBytes("UTF-8"));
                String checksum = toString(digest);
                if (!checksum.equals(incomingDiff.getMd5())) {
                    throw new OutOfSyncException(String.format("Expected %s but was %s", incomingDiff.getMd5(), checksum));
                }
                shadow.setContent(shadowTextPostPatch);
                synchronized (pad) {
                    String text1 = pad.getContent();
                    results = dmp.patch_apply(patch, text1);
                    pad.setContent(results[0].toString());
                    text1 = pad.getContent();

                    LinkedList<Patch> patch_list = dmp.patch_make(shadow.getContent(), text1);
                    String patch_text = dmp.patch_toText(patch_list);

                    shadow.setContent(text1);
                    md5 = MessageDigest.getInstance("MD5");
                    digest = md5.digest(text1.getBytes("UTF-8"));
                    checksum = toString(digest);
                    PadDiff padDiff = new PadDiff();
                    padDiff.setDiff(patch_text);
                    padDiff.setMd5(checksum);
                    return padDiff;
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

        }

    }

    private static String toString(byte[] digest) {
        return Hex.encodeHexString(digest);
    }

}
