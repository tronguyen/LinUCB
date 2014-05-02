package com.smu.linucb.global;

public class GlobalSQLQuery {
	public static String GETVIEWTAG = "SELECT * FROM view_bookmark_tag";
	public static String GETBOOKMARK_TAG = "SELECT bookmarkID, tagID, tagWeight, value FROM tbl_bookmark_tags btl, tbl_tags tg where btl.tagID = tg.id";
	public static String GETTAG = "SELECT * FROM tbl_tags";
	// change user_taggedbookmarks to tbl_user_taggedbookmarks
	public static String GETUSER = "SELECT distinct userID from tbl_user_taggedbookmarks";
	public static String GETBM4USER = "SELECT distinct bookmarkID FROM tbl_user_taggedbookmarks where userID = ?";
}
