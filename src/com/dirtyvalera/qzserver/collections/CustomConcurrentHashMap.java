package com.dirtyvalera.qzserver.collections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomConcurrentHashMap<K, V> {
	private Map<K, K> keyKey;
	private Map<K, V> keyValue;
	
	public CustomConcurrentHashMap(){
		keyKey = new ConcurrentHashMap<K, K>();
		keyValue = new ConcurrentHashMap<K, V>();
	}
	
	public boolean containsKeyAsAPrimary(K key){
		return keyKey.containsKey(key);
	}
	
	public boolean containsKey(K key){
		if(keyValue.containsKey(key)){
			return true;
		}else{
			if(keyKey.containsKey(key)){
				K secKey = keyKey.get(key);
				return keyValue.containsKey(secKey);
			}else{
				return false;
			}
		}
	}
	
	public void put(K firstKey, K secondKey, V value){
		keyValue.put(secondKey, value);
		keyKey.put(firstKey, secondKey);
	}
	
	public V get(K key){
		if(keyValue.containsKey(key)){
			return keyValue.get(key);
		}else{
			if(keyKey.containsKey(key)){
				K secKey = keyKey.get(key);
				return keyValue.get(secKey);
			}else{
				return null;
			}
		}
	}
	
	public int size(){
		return keyValue.size();
	}
	
	public void remove(K primaryKey){
		K secondaryKey = keyKey.get(primaryKey);
		if(secondaryKey == null){
			return;
		}
		keyKey.remove(primaryKey);
		keyValue.remove(secondaryKey);
	}
}
