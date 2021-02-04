package edu.handong.csee.isel.pdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class DeveloperCommit {
	//number of commit
	//key - data
	//commit time - commit hash : tree map <string, arrayList>
	TreeSet<String> commitHashs;
	HashMap<String,String> key_data;
	TreeMap<String,TreeSet<String>> commitTime_commitHash;
	
	public DeveloperCommit() {
		commitHashs = new TreeSet<>();
		key_data = new HashMap<>();
		commitTime_commitHash = new TreeMap<>();
	}
	

	public TreeSet<String> getCommitHashs() {
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

	public TreeMap<String, TreeSet<String>> getCommitTime_CommitID() {
		return commitTime_commitHash;
	}

	public void setCommitTime_CommitID(String commitTime, String commitHash) {
		TreeSet<String> keys;
		if(this.commitTime_commitHash.containsKey(commitTime)) {
			keys = this.commitTime_commitHash.get(commitTime);
			keys.add(commitHash);
		}else {
			keys = new TreeSet<String>();
			keys.add(commitHash);
			this.commitTime_commitHash.put(commitTime, keys);
		}
	}

}
