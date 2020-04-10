package syncgateway.rest.calls;


import com.fasterxml.jackson.core.JsonProcessingException;

public class UpdateBulkSyncGateway {


	public static void main(String[] args) throws JsonProcessingException {
	
		for(int j=0; j< 1; j++) {
			Runnable r = new MyRunnable(j);
			new Thread(r).start();
		}
	}
}
