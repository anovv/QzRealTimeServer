import com.dirtyvalera.qzserver.server.Server;


public class Test {
	
	public static void main(String[] args) throws Exception{
		Server server = new Server(2345);
		server.run();				
	}
}
