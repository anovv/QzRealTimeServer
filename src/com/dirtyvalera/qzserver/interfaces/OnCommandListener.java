package com.dirtyvalera.qzserver.interfaces;

import io.netty.channel.Channel;

public interface OnCommandListener {

	public void onRandomRequest(long id, long themeId, String qIds, Channel incoming);
	
	public void onNewRequest(long id, long rid, long themeId, String qIds, Channel channel);
	
	public void onAnswer(long id, long rid, String answer, String time, String round);
	
	public void onNextQuestion(long id, long rid);
	
	public void onFinalize(long id, long rid);
	
	public void onError(long id, long rid);
	
	public void closeChannel(long id, Channel channel, String message);
}
