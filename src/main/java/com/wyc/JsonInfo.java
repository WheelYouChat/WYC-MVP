package com.wyc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonInfo {
	public static void main(String args[]) throws JsonProcessingException, IOException {
		String fileName = args[0];
		System.out.println("File : " + fileName);
		ObjectMapper mapper = new ObjectMapper();
		File file = new File(fileName);
		JsonNode node = mapper.readTree(file);
		
		Map<String, Long> info = new HashMap<>();
		Map<String, Integer>[] attrsInfo = new Map[15];
		for(int i = 0; i < attrsInfo.length; i++) {
			attrsInfo[i] = new HashMap<>();
		}
		analyse(node, info, attrsInfo, 0);
		
		List<String> lines = FileUtils.readLines(file);
		System.out.println("lines count : " + lines.size());
		System.out.println("Max length size = " + lines.stream().map(s -> s.length()).max(Integer::compareTo).get());
		System.out.println("Average length size = " + file.length() / lines.size());
		
		System.out.println("Big lines count = " + lines.stream().map(s -> s.length()).filter(i -> i > 1000).count());
		
		System.out.println("File is parsed " + info);
	}
	
	public static void analyse(JsonNode node, Map<String, Long> info, Map<String, Integer>[] attrsInfo, int level) {
		int length = node == null || node.toString() == null ? 0 : node.toString().length();
		if(level == 1) {
			System.out.println(length);
		}
		// putValue("SIZE_" + level, info, length);
		putValue("COUNT_" + level, info, 1);
		if(node instanceof ObjectNode) {
			ObjectNode objectNode = (ObjectNode) node;
			
			for(Iterator<JsonNode> iter = objectNode.iterator(); iter.hasNext(); ) {
				JsonNode child = iter.next();
				analyse(child, info, attrsInfo, level + 1);
				
			}
		} else {
			for(int i = 0; i < node.size(); i++) {
				JsonNode child = node.get(i);
				if(child != null) {
					analyse(child, info, attrsInfo, level + 1);
				}
			}
		}
	}

	private static void putValue(String key, Map<String, Long> info, long value) {
		Long oldValue = info.get(key);
		oldValue = oldValue == null ? 0 : oldValue;
		oldValue += value;
		info.put(key, oldValue);

	}
}


