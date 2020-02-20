package com.dirtyvalera.qzserver.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import com.dirtyvalera.qzserver.interfaces.OnCommandListener;

public class ServerInitializer extends ChannelInitializer<SocketChannel>{

	private OnCommandListener onCommandListener; 
	
	
	public ServerInitializer(OnCommandListener onCommandListener){
		this.onCommandListener = onCommandListener;
	}
	
	@Override
	protected void initChannel(SocketChannel arg0) throws Exception {
		ChannelPipeline pipeline = arg0.pipeline();
		
		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		pipeline.addLast("decoder", new StringDecoder());
		pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("timeout", new ReadTimeoutHandler(Server.READ_TIMEOUT_SECONDS));
		pipeline.addLast("handler", new ServerHandler(onCommandListener));		
	}
}
