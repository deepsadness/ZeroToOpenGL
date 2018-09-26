package com.cry.zerotoopengl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        long preTime = System.nanoTime();
        System.out.println(preTime);
        try {
            Thread.sleep(16);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long curTime = System.nanoTime();
        System.out.println(curTime);
        System.out.println(curTime - preTime);
        assertEquals(4, 2 + 2);
    }
}