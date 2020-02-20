package com.dirtyvalera.qzserver.models;

public class GameState {
	
	private long aId;
	private long bId;
	private boolean aIsSet;
	private boolean bIsSet;
	
	public GameState(long aId, long bId){
		this.aId = aId;
		this.bId = bId;
		aIsSet = false;
		bIsSet = false;
	}
				
	public boolean areBothReady(){
		return aIsSet&&bIsSet;
	}
	
	public void setOneReady(long id){
		if(aId == id){
			aIsSet = true;
		}
		
		if(bId == id){
			bIsSet = true;
		}
	}
	
	public void setBothNotReady(){
		aIsSet = false;
		bIsSet = false;
	}
	
	public long getRId(long id){
		if(aId == id){
			return bId;
		}else{
			return aId;
		}
	}
}
