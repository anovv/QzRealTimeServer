package com.dirtyvalera.qzserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dirtyvalera.qzserver.collections.CustomConcurrentHashMap;
import com.dirtyvalera.qzserver.collections.RandomReqMap;
import com.dirtyvalera.qzserver.collections.ReqMap;
import com.dirtyvalera.qzserver.interfaces.OnCommandListener;
import com.dirtyvalera.qzserver.models.GameState;

public class Server implements OnCommandListener{
	
	//private static final int MAX_NUMBER_OF_QUESTIONS = 6;
	//private static final int MAX_NUMBER_OF_GENERATIONS = 1000;
	private static final String LOG_FILE_PATH = "log.txt";
	//private static final String QUESTION_IDS_FILE_PATH = "ids.txt";
	public static final int READ_TIMEOUT_SECONDS = 5*60;
	public static final long UPDATE_LOG_MILLIS = 30*60*1000;//30 minutes
	
	//private static Map<Long, List<String>> questionIdRanges;// range of question ids for each theme
	
	private int port;
		
	private RandomReqMap randomRequests;
	private ReqMap requests;
	private CustomConcurrentHashMap<Long, GameState> gameStates;//current games
	private Map<Long, Channel> channels;//active channels
	private Map<Long, String> questionIds;//generated ids
	private Thread logThread;
	private boolean isServerRunning = true;
	
	private volatile long gamesPlayedCounter = 0;
	private volatile long randomGamesCounter = 0;
	private volatile long versusGamesCounter = 0;
	
	//private volatile int counter = 0;
	
	public Server(int port){
		this.port = port;
				
		//questionIdRanges = new ConcurrentHashMap<Long, List<String>>();
		//updateQuestionIds();
		randomRequests = new RandomReqMap();
		questionIds = new ConcurrentHashMap<Long, String>();
		//randomRequests.init(questionIdRanges);
		requests = new ReqMap();
		gameStates = new CustomConcurrentHashMap<Long, GameState>();
		channels = new ConcurrentHashMap<Long, Channel>();
		logThread = new Thread(new Runnable(){

			@Override
			public void run() {
				while(isServerRunning){
					
					updateLog();
					try{
						Thread.sleep(UPDATE_LOG_MILLIS);
					}catch(Exception e){}
				}
			}
		});
	}
	
	public void run(){
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try{
			ServerBootstrap bootstrap = new ServerBootstrap()
				.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ServerInitializer(this));
			
			new Thread(new Runnable(){
				
				@Override
				public void run() {
					while(true){
						System.out.println("Running. Channels: " + channels.size() + " |Requests: " + requests.reqSize() + " |Random Requests: " + randomRequests.size(10) + " | Games: " + gameStates.size() + " |Questions: " + questionIds.size());
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}				
			}).start();
			logThread.start();
			bootstrap.bind(port).sync().channel().closeFuture().sync();
		}catch(Exception e){
			
		}	
		finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	@Override
	public void onRandomRequest(long id, long themeId, String qIds, Channel incoming) {
		
		long rid = randomRequests.get(themeId);
		channels.put(id, incoming);
		
		if(rid != -1){
			randomRequests.remove(rid);
			if(channels.containsKey(id) && channels.containsKey(rid)){
				randomGamesCounter++;
				GameState gameState = new GameState(id, rid);
				gameStates.put(id, rid, gameState);
				
				Channel a = channels.get(id);
				Channel b = channels.get(rid);
				String aMsg = "RQIDS#" + qIds + "_" + rid + "\n";
				String bMsg = "RQIDS#" + qIds + "_" + id + "\n";

				a.writeAndFlush(aMsg);
				b.writeAndFlush(bMsg);
				
			}else if(channels.containsKey(id)){
				//notify a
				Channel a = channels.get(id);
				a.writeAndFlush("ERR#1\n");
				finalize(id, rid);
			}else if(channels.containsKey(rid)){
				//notify b
				Channel b = channels.get(rid);
				b.writeAndFlush("ERR#1\n");
				finalize(id, rid);
			}else{
				finalize(id, rid);
			}
		}else{
			randomRequests.put(themeId, id);
		}
	}

	@Override
	public void onNewRequest(long id, long rid, long themeId, String qIds, Channel channel) {
		boolean isPaired = requests.handleRequest(id, rid, themeId);
		channels.put(id, channel);	
		if(isPaired){
			versusGamesCounter++;
			GameState gameState = new GameState(id, rid);
			gameStates.put(id, rid, gameState);
			
			String rqIds = questionIds.get(rid);
			questionIds.remove(rid);
			String msg;
			if(qIds == null || qIds.length() == 0){
				msg = "QIDS#" + rqIds + "\n";
			}else{
				msg = "QIDS#" + qIds + "\n";
			}

			Channel a = channel;
			Channel b = channels.get(rid);
			a.writeAndFlush(msg);
			b.writeAndFlush(msg);	
		}else{
			questionIds.put(id, qIds);
		}
	}

	/*Error code: 1 - op disconnected
				  2 - connection timeout*/

	@Override
	public void onAnswer(long id, long rid, String answer, String time, String round) {
		
		if(channels.containsKey(rid)){
			Channel reciever = channels.get(rid);
			reciever.writeAndFlush("ANS#" + answer + "_" + time + "_" + round + "\n");		
		}else{						
			Channel u = channels.get(id);
			u.writeAndFlush("ERR#1\n");
			finalize(id, rid);
		}
	}

	@Override
	public void onNextQuestion(long id, long rid) {
		
		GameState gameState = gameStates.get(id);
		
		if(gameState != null){
			gameState.setOneReady(id);
			if(gameState.areBothReady()){
				Channel a = channels.get(id);
				Channel b = channels.get(rid);
				a.writeAndFlush("NEXT\n");
				b.writeAndFlush("NEXT\n");
				gameState.setBothNotReady();
			}
		}else{
			Channel c = channels.get(id);
			c.writeAndFlush("ERR#1\n");
			finalize(id, rid);
		}
	}

	@Override
	public void onFinalize(long id, long rid) {
		gamesPlayedCounter++;
		if(channels.containsKey(rid)){
			Channel op = channels.get(rid);
			op.writeAndFlush("FIN\n");
		}
		finalize(id, rid);
	}

	@Override
	public void onError(long id, long rid) {
		if(channels.containsKey(rid)){
			Channel op = channels.get(rid);
			op.writeAndFlush("ERR#1\n");
		}
		finalize(id, rid);
	}
	

	@Override
	public void closeChannel(long id, Channel channel, String message){
		questionIds.remove(id);
		if(message != null){
			channel.writeAndFlush("ERR#2\n");
		}
		channel.close();
		if(channels.containsKey(id)){
			Channel c = channels.get(id);
			c.close();
			channels.remove(id);
		}
		randomRequests.remove(id);
		requests.removeFromCalls(id);
		
		if(gameStates.containsKey(id)){
			long rid = gameStates.get(id).getRId(id);
			if(channels.containsKey(rid)){
				Channel op = channels.get(rid);
				op.writeAndFlush("ERR#1\n");
				op.close();
				channels.remove(rid);
			}
			if(gameStates.containsKeyAsAPrimary(id)){
				gameStates.remove(id);
			}else if(gameStates.containsKeyAsAPrimary(rid)){
				gameStates.remove(rid);
			}
		}
	}
	
	private void finalize(long id, long rid){
		questionIds.remove(id);
		questionIds.remove(rid);
		if(channels.containsKey(id)){
			Channel c = channels.get(id);
			c.close();
			channels.remove(id);
		}
		
		if(channels.containsKey(rid)){
			Channel c = channels.get(rid);
			c.close();
			channels.remove(rid);
		}
		
		if(gameStates.containsKeyAsAPrimary(id)){
			gameStates.remove(id);
		}else if(gameStates.containsKeyAsAPrimary(rid)){
			gameStates.remove(rid);
		}
	}
	
	private String getLogString(){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String dateStr = dateFormat.format(date);
		String res = dateStr + " " 
				+ "Requests: " + requests.reqSize() + " "
				+ "Games being played: " + gameStates.size() + " "
				+ "Random Games Requested: " + randomGamesCounter + " "
				+ "Reg Games Requested: " + versusGamesCounter + " "
				+ "Games Finished: " + gamesPlayedCounter;
		
		return res;
	}

	private void updateLog(){
		File f = new File(LOG_FILE_PATH);
		if(!f.exists()) {
		    try {
				f.createNewFile();
			} catch (IOException e) {
				System.err.println("Problems creating log file");
			}
		} 
		try {
			FileWriter fw = new FileWriter(f.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.append(getLogString());
			bw.newLine();
			bw.close();
		} catch (Exception e) {
			System.err.println("Problems writing to log");
		} 
	}
}

