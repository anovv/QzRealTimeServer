package com.dirtyvalera.qzserver.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;

import com.dirtyvalera.qzserver.interfaces.OnCommandListener;

public class ServerHandler extends SimpleChannelInboundHandler<String>{

	private OnCommandListener onCommandListener;
	private static final AttributeKey<Long> KEY = AttributeKey.valueOf("id");
	
	public ServerHandler(OnCommandListener onCommandListener){
		this.onCommandListener = onCommandListener;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext context, String message)
			throws Exception {
		Channel channel = context.channel();
		String[] strs = message.split("#");
		String command = strs[0];
		String[] values = strs[1].split("_"); 
		
		if(command.equals("RAND")){
			//values: 1 - user id;
			//values: 2 - theme id;
			long id = Long.parseLong(values[0]);
			long themeId = Long.parseLong(values[1]);
			
			String qIds;
			if(values.length == 2){
				qIds = "";
			}else{
				qIds = values[2].replace(';', '_');
			}
			
			setIdForChannel(channel , id);
			
			onCommandListener.onRandomRequest(id, themeId, qIds, channel);
			return;
		}
		
		if(command.equals("REQUEST")){//new conn
			//values: 1 - id1 who makes request(or responds), 2 - id2 with who wants to play(to who responds), 3 - themeid, 4 - qIds
			long id = Long.parseLong(values[0]);
			long rid = Long.parseLong(values[1]);
			long themeId = Long.parseLong(values[2]);
			String qIds;
			if(values.length == 3){
				qIds = "";
			}else{
				qIds = values[3].replace(';', '_');
			}
			setIdForChannel(channel , id);
			
			onCommandListener.onNewRequest(id, rid, themeId, qIds, channel);
			return;
		}
		
		if(command.equals("ANS")){// one user to another					
			//values : 1 - id1 sender, 2 - id2 - reciever, 3 - ans number(1, 2, 3, 4), 4 - timestamp, 5 - round number
			long id = Long.parseLong(values[0]);
			long rid = Long.parseLong(values[1]);
			String answer = values[2];
			String time = values[3];
			String round = values[4];
			
			onCommandListener.onAnswer(id, rid, answer, time, round);
			return;
		}
		
		if(command.equals("NEXT")){// user is ready to get the next question
			//values : 1 - id user id 2 - op id
			long id = Long.parseLong(values[0]);
			long rid = Long.parseLong(values[1]);
			
			onCommandListener.onNextQuestion(id, rid);
			return;
		}
		
		if(command.equals("FINALIZE")){// end the game
			//values: 1 - user id, 2 - op id;
			long id = Long.parseLong(values[0]);
			long rid = Long.parseLong(values[1]);

			onCommandListener.onFinalize(id, rid);
			return;
		}
		
		if(command.equals("ERR")){
			long id = Long.parseLong(values[0]);
			long rid = Long.parseLong(values[1]);
			
			onCommandListener.onError(id, rid);
			return;
		}
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx){
    	Channel channel = ctx.channel();
    	long id = getChannelId(channel);
    	onCommandListener.closeChannel(id, channel, null);
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {

    	Channel channel = ctx.channel();
    	long id = getChannelId(channel);
    	
        if(cause instanceof ReadTimeoutException){
        	onCommandListener.closeChannel(id, channel, "");
        }else{
        	onCommandListener.closeChannel(id, channel, null);
        }
    }
	
	private static void setIdForChannel(Channel channel, long id){
		channel.attr(KEY).set(id);
	}
	
	private static long getChannelId(Channel channel){
		return channel.attr(KEY).get();
	}

}
