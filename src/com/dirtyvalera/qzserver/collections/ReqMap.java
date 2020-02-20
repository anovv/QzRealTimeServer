package com.dirtyvalera.qzserver.collections;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.dirtyvalera.qzserver.server.Server;

public class ReqMap {
	
	private Map<Long, Map<Long, Long>> reqs;
				//k1, (k2 -> themeId2), 
				//    (k3 -> themeId3).
				//    (k4 -> themeI4)
	            //        ...
	
	private Map<Long, Long> calls;
			//k2 -> k1
			//k3 -> k1
			//k4 -> k1
			// ...
	
	public ReqMap(){
		reqs = new ConcurrentHashMap<Long, Map<Long, Long>>();
		calls = new ConcurrentHashMap<Long, Long>();
	}
	
	public void putRequest(long id, long rid, long themeId){
		if(reqs.containsKey(rid)){
			reqs.get(rid).put(id, themeId);
		}else{
			Map<Long, Long> req = new ConcurrentHashMap<Long, Long>();
			req.put(id, themeId);
			reqs.put(rid, req);
		}
		calls.put(id, rid);
	}
	
	public boolean handleRequest(long id, long rid, long themeId){
		if(reqs.containsKey(id)){
			Map<Long, Long> rs = reqs.get(id);
			if(rs.containsKey(rid) && rs.get(rid) == themeId){
				reqs.remove(id);
				for(Entry<Long, Long> req: rs.entrySet()){
					calls.remove(req.getKey());
				}
				return true;
			}else{
				reqs.remove(id);
				for(Entry<Long, Long> req: rs.entrySet()){
					calls.remove(req.getKey());
				}
				putRequest(id, rid, themeId);
				return false;
			}
		}else{
			putRequest(id, rid, themeId);
			return false;
		}
	}
	
	public void removeFromCalls(long id){
		if(calls.containsKey(id)){
			long rid = calls.get(id);
			calls.remove(id);
			if(reqs.containsKey(rid)){
				Map<Long, Long> rs = reqs.get(rid);
				rs.remove(id);
				if(rs.isEmpty()){
					reqs.remove(rid);
				}
			}
		}
	}
	
	public int reqSize(){
		return reqs.size();
	}
	
	public int callSize(){
		return calls.size();
	}
}
