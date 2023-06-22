package bitedu.bipa.quiz.service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Scanner;

import bitedu.bipa.quiz.dao.LibraryDAO;
import bitedu.bipa.quiz.util.DateCalculation;
import bitedu.bipa.quiz.vo.BookUseStatusVO;
import bitedu.bipa.quiz.vo.BookVO;
import bitedu.bipa.quiz.vo.UserBookStatusVO;
import bitedu.bipa.quiz.vo.UserVO;

public class LibraryBookService {
	
	private LibraryDAO dao;
	private UserVO UVO;
	public LibraryBookService() {
		dao = new LibraryDAO();
	}
	//select * from book_use_status where user_id = 'user1' and borrow_start between '2023-6-1' and '2023-6-30';
	// select * from book_user
	 
	// 이용상태 대출정지기간의 정보를 정리
	private ArrayList<BookUseStatusVO> getNotReturnedBooks(ArrayList<BookUseStatusVO> bookList, Calendar criteriaDate){
		ArrayList<BookUseStatusVO> result = null;
		result = new ArrayList<BookUseStatusVO>();
		Timestamp stopServiceDate = null;
		//미반납도서 - 반납기간이 지나고 반납날짜가 비어있는 도서
		String userId = null;
		for(BookUseStatusVO book : bookList) {
			if( book.getBorrowEnd().getTime() <  criteriaDate.getTimeInMillis()&&book.getReturnDate()==null) {
				userId = book.getUserId();
				result.add(book);
				//System.out.println(book);
				Timestamp temp = DateCalculation.calcuStopDate(new Timestamp(criteriaDate.getTimeInMillis()), book.getBorrowEnd());
				if(stopServiceDate!=null) {
					stopServiceDate = temp.getTime()-stopServiceDate.getTime()>0?temp:stopServiceDate;
				} else {
					stopServiceDate = temp;
				}
			}
		}
		
		if(stopServiceDate!=null) {
			//DB user 정보 update
			try {
				dao.updateUserStopStatus(userId, stopServiceDate);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		return result;
	}
	
	private ArrayList<BookUseStatusVO> getReturnedBooks(ArrayList<BookUseStatusVO> bookList){
		ArrayList<BookUseStatusVO> result = null;
		//기반납도서
		result = new ArrayList<BookUseStatusVO>();
		for(BookUseStatusVO book : bookList) {
			if(book.getReturnDate()!=null&&book.getBorrowEnd().getTime() >= book.getReturnDate().getTime()) {
				result.add(book);
			}
		}
		return result;
	}
	
	private ArrayList<BookUseStatusVO> getExpectingReturnBooks(ArrayList<BookUseStatusVO> bookList, Calendar criteriaDate){
		ArrayList<BookUseStatusVO> result = null;
		//반납예정도서
		result = new ArrayList<BookUseStatusVO>();
		for(BookUseStatusVO book : bookList) {
			if(book.getReturnDate()==null&&book.getBorrowEnd().getTime() >= criteriaDate.getTimeInMillis()) {
				result.add(book);
				//System.out.println(book);
			}
		}
		return result;
	}
	
	
	public HashMap<String, Object> getUserStatus(String userId,String startMonth){
		HashMap<String, Object> result = null;
		result = new HashMap<String, Object>();
		
		UserVO user = null;
		ArrayList<BookUseStatusVO> list = null;
	
		try {
			
			list = dao.selectBookInfoByUser(userId, startMonth);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Calendar criteriaDate = Calendar.getInstance();
		LocalDate now = LocalDate.now();
		
		criteriaDate.set(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
		ArrayList<BookUseStatusVO> allReturned = this.getReturnedBooks(list);
		ArrayList<BookUseStatusVO> notReturned = this.getNotReturnedBooks(list, criteriaDate);
		ArrayList<BookUseStatusVO> expectingReturn = this.getExpectingReturnBooks(list,criteriaDate);
		
		try {
			user = dao.selectUser(userId);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HashMap<String, ArrayList<BookUseStatusVO>> bookInfo = new HashMap<>();
		bookInfo.put("total", list);
		bookInfo.put("allReturned", allReturned);
		bookInfo.put("notReturned", notReturned);
		bookInfo.put("expectingReturn", expectingReturn);
		
		UserBookStatusVO userState = new UserBookStatusVO();
		userState.setUserId(userId);
		userState.setAvailableBook(user.getAvailableBook());
		userState.setExpectingReturnBook(expectingReturn.size());
		userState.setTotalUsingBook(list.size());
		userState.setReturnedBook(allReturned.size());
		userState.setNotReturnedBook(notReturned.size());
		

		if(notReturned.size()>0) {
			userState.setUserState("대출정지");
			userState.setStopDate(DateCalculation.getDate(user.getServiceStop()));
		} else {
			userState.setUserState("정상");
			userState.setStopDate("-");
		}
		HashMap<String, UserBookStatusVO> userInfo = new HashMap<>();
		
		userInfo.put("user", userState);
		result.put("userInfo", userInfo);
		result.put("bookInfo", bookInfo);
		
		return result;
	}

	public boolean rent_Book(String userId,int bookNum){
		boolean flag = false;
		boolean result = false;
		flag = dao.rent_Book(bookNum);

		if(flag){
			if(dao.rent_Book_commit(userId,bookNum)){
				result = true;
			}else{

			}
		}
		return result;
	}
	
	public boolean return_book(String userId, int bookNum){
		boolean flag = false;
		flag = dao.return_book(bookNum, userId);

		if(flag){
			flag = true;
		}else{
		}
		return flag;
	}
	
	
}
