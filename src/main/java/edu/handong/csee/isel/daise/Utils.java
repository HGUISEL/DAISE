package edu.handong.csee.isel.daise;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

public class Utils {
	
	public static String getStringDateTimeFromCommit(RevCommit commit) {	
		
		SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date commitDate = commit.getAuthorIdent().getWhen();

		TimeZone GMT = commit.getCommitterIdent().getTimeZone();
		ft.setTimeZone(GMT);

		return ft.format(commitDate);
	}
	
	public static List<DiffEntry> diff(RevCommit parent, RevCommit commit, Repository repo) {

  		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
 		df.setRepository(repo);
 		df.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS));
 		df.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
 		df.setDetectRenames(true);
 		List<DiffEntry> diffs = null;
 		try {
 			diffs = df.scan(parent.getTree(), commit.getTree());
 		} catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}

  		return diffs;
 	}
	
	public static String parseAuthorID(String authorId) {
		Pattern pattern = Pattern.compile(".+\\[.+,(.+),.+\\]");
		Matcher matcher = pattern.matcher(authorId);
		while(matcher.find()) {
			authorId = matcher.group(1);
		}
		return authorId;
	}

}
