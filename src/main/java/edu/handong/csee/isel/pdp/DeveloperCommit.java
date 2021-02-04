package edu.handong.csee.isel.pdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class DeveloperCommit {
	//number of commit
	//key - data
	//commit time - commit hash : tree map <string, arrayList>
	ArrayList<String> commitHashs;
	HashMap<String,String> key_data;
	TreeMap<String,ArrayList<String>> commitTime_commitHash;
	
	public DeveloperCommit() {
		commitHashs = new ArrayList<>();
		key_data = new HashMap<>();
		commitTime_commitHash = new TreeMap<>();
	}
	

	public ArrayList<String> getCommitHashs() {
		return commitHashs;
	}
	
	public int getContCommit() {
		return commitHashs.size();
	}

	public void setCommitHashs(String commitHash) {
		this.commitHashs.add(commitHash);
	}

	public HashMap<String, String> getKey_data() {
		return key_data;
	}

	public void setKey_data(String key, String data) {
		this.key_data.put(key, data);
	}

	public TreeMap<String, ArrayList<String>> getCommitTime_key() {
		return commitTime_commitHash;
	}

	public void setCommitTime_key(String commitTime, String commitHash) {
		ArrayList<String> keys;
		if(this.commitTime_commitHash.containsKey(commitTime)) {
			keys = this.commitTime_commitHash.get(commitTime);
			keys.add(commitHash);
		}else {
			keys = new ArrayList<String>();
			keys.add(commitHash);
			this.commitTime_commitHash.put(commitTime, keys);
		}
	}

}
