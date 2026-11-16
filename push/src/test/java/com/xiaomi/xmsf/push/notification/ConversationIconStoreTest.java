package com.xiaomi.xmsf.push.notification;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class ConversationIconStoreTest {

    private File directory;
    private ConversationIconStore store;

    @Before
    public void setUp() throws Exception {
        directory = Files.createTempDirectory("conversation-icons").toFile();
        store = new ConversationIconStore(directory);
    }

    @After
    public void tearDown() {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        directory.delete();
    }

    @Test
    public void savedIconIsReturnedForTheSameAddress() {
        byte[] png = new byte[]{1, 2, 3, 4};

        store.write("http://p.qlogo.cn/gh/684866903/684866903/100", png);

        assertArrayEquals(png, store.read("http://p.qlogo.cn/gh/684866903/684866903/100"));
    }

    @Test
    public void addressWithoutSavedIconReturnsNothing() {
        assertNull(store.read("http://p.qlogo.cn/gh/1/1/100"));
    }

    @Test
    public void separateAddressesKeepTheirOwnIcon() {
        store.write("http://p.qlogo.cn/gh/1/1/100", new byte[]{1});
        store.write("http://p.qlogo.cn/gh/2/2/100", new byte[]{2});

        assertArrayEquals(new byte[]{1}, store.read("http://p.qlogo.cn/gh/1/1/100"));
        assertArrayEquals(new byte[]{2}, store.read("http://p.qlogo.cn/gh/2/2/100"));
    }

    @Test
    public void writingTheSameAddressReplacesTheSavedIcon() {
        store.write("http://p.qlogo.cn/gh/1/1/100", new byte[]{1});
        store.write("http://p.qlogo.cn/gh/1/1/100", new byte[]{3});

        assertArrayEquals(new byte[]{3}, store.read("http://p.qlogo.cn/gh/1/1/100"));
        assertEquals(1, directory.listFiles().length);
    }

    @Test
    public void expiredIconIsRemovedOnNextWrite() {
        String stale = "http://p.qlogo.cn/gh/9/9/100";
        store.write(stale, new byte[]{9});
        File[] stored = directory.listFiles();
        assertTrue(stored != null && stored.length == 1);
        assertTrue(stored[0].setLastModified(System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000));

        store.write("http://p.qlogo.cn/gh/1/1/100", new byte[]{1});

        assertNull(store.read(stale));
        assertArrayEquals(new byte[]{1}, store.read("http://p.qlogo.cn/gh/1/1/100"));
    }
}
