import java.util.concurrent.*;

/**
 * Created by liuhailin on 2017/4/18.
 */
public class Test {

	private static ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

	public static void main(String[] args) throws InterruptedException {

		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {

			    while (!Thread.currentThread().isInterrupted()){
                    System.out.println("11111");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
		});

        t.start();


        t.interrupt();
	}
}
