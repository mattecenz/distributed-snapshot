package polimi.ds.dsnapshot.Utilities;

import org.junit.jupiter.api.Test;

public class ThreadPoolTest {


    @Test
    public void test(){
        ThreadPool.submit(this::testMethod);
        System.out.println(Thread.currentThread().getName());
    }

    private void testMethod(){
        System.out.println("Testing thread pool..." + Thread.currentThread().getName()) ;
    }
}
