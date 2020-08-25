package com.snet.smore.transformer;

import com.snet.smore.common.domain.Agent;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ListPerformanceTest {
    @Test
    @Ignore
    public void test() {
        List<Agent> agentArray = new ArrayList<>();
        List<Agent> agentLinked = new LinkedList<>();

        long start1 = System.currentTimeMillis();

        for(int i = 0; i < 100000; i++) {
            agentArray.add(new Agent());
        }

        long end1 = System.currentTimeMillis();
        System.out.println("Add agentArray : " + (end1 - start1));




        long start2 = System.currentTimeMillis();

        for (int i = 0; i < agentArray.size(); i++) {
            agentArray.remove(0);
        }

        long end2 = System.currentTimeMillis();
        System.out.println("put agentArray : " + (end2 - start2));



        long start3 = System.currentTimeMillis();

        for(int i = 0; i < 100000; i++) {
            agentLinked.add(new Agent());
        }

        long end3 = System.currentTimeMillis();
        System.out.println("Add agentLinked : " + (end3 - start3));



        long start4 = System.currentTimeMillis();

        for (int i = 0; i < agentLinked.size(); i++) {
            agentLinked.remove(0);
        }

        long end4 = System.currentTimeMillis();
        System.out.println("Put agentLinked : " + (end4 - start4));
        agentLinked.clear();

    }
}
