package bitedu.bipa.quiz.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class DateCalculation {
	static LocalDateTime now = LocalDateTime.now();
	
	//기준일_반납일자에 대하여 정지기간을 계산
	public static Timestamp calcuStopDate(Timestamp returnDate, Timestamp endDate) {
	
		int differ = getDiffDays(endDate, returnDate);
		return getEndnDate(returnDate, differ);
	}
	
	public static int calcuDiffDate(Timestamp returnDate, Timestamp endDate) {

		return getDiffDays(endDate, returnDate);
	}
	public static Timestamp getEndnDate(Timestamp startDate, int day) {		
		Calendar c1 = Calendar.getInstance();
		c1.setTimeInMillis(startDate.getTime());
		c1.add(Calendar.DATE, day);
		return new Timestamp(c1.getTimeInMillis());
	}
	
	public static String getDate(Timestamp ts) {
		String date = null;
		date = "-";
		if(ts!=null) {
			Date temp = new Date(ts.getTime());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			date = sdf.format(temp);
		}
		return date;
	}
	public static String getDate() {
		String date = null;
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd");
        
		return sdf.format(timestamp);
	}
	
	public static String get_return_day() {
		LocalDateTime return_day = now.plusDays(13);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return return_day.format(dtf);
	}
	private static int getDiffDays(Timestamp before, Timestamp after) {
		Calendar c1 = Calendar.getInstance();
		c1.setTimeInMillis(before.getTime());
		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(after.getTime());
		int result = c2.get(Calendar.DAY_OF_YEAR) - c1.get(Calendar.DAY_OF_YEAR);
		//System.out.println(c2.get(Calendar.DAY_OF_MONTH));
		//System.out.println(c1.get(Calendar.DAY_OF_MONTH));
		return result;
	}
}
