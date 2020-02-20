package com.dirtyvalera.qzserver.collections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RandomReqMap {
	private Map<Long, Long> reqs;
	private Map<Long, Long> calls;
	
	public RandomReqMap(){
		reqs = new ConcurrentHashMap<Long, Long>();
		calls = new ConcurrentHashMap<Long, Long>();
	}
	
	public void put(long themeId, long id){
		reqs.put(themeId, id);
		calls.put(id, themeId);
	}
	
	public void remove(long id){
		if(calls.containsKey(id)){
			long themeId = calls.get(id);
			calls.remove(id);
			reqs.put(themeId, (long) -1);
		}
	}
	
	public long get(long themeId){
		if(reqs.containsKey(themeId)){
			return reqs.get(themeId);
		}else{
			return -1;
		}
	}
	
	public int size(long themeId){
		if(!reqs.containsKey(themeId)){
			return 0;
		}
		return (reqs.get(themeId) == -1) ? 0 : 1;
	}
	
	public int size(){
		return reqs.size();
	}
}
