package org.refactoringminer.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class HashingTest {
    private final static List<Pair<String, String>> TEST_CASES_LIST = new ArrayList<>();

    static {
        TEST_CASES_LIST.add(Pair.of("Hi There!",
                "48294c4b618d69d9af247a9feea33a0b3a809713699c9fc620a15086a5b4fdf45197ed5539f343892ee6ab37ed5a35d0d0a84c35451a1550f67fdfc7c96d839e"));
        TEST_CASES_LIST.add(Pair.of("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer nec ipsum a diam posuere scelerisque.",
                "c2dc96d862d1e90f21bcd96f58b58db64a5bd034cd91b2a318bbb7753b07fc9c6523dfed919e90c0d0d6c776ed63db10a944fecd9754e1e76014a15c3b3e024a"));
    }

    @Test
    public void testNumberOfChangedFiles() {
        for (Pair<String, String> testCase : TEST_CASES_LIST) {
            Assert.assertEquals(testCase.getRight(), Hashing.getSHA512(testCase.getLeft()));
        }
    }
}
