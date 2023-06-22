package bitedu.bipa.quiz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import bitedu.bipa.quiz.util.ConnectionManager;
import bitedu.bipa.quiz.util.DateCalculation;
import bitedu.bipa.quiz.vo.BookUseStatusVO;
import bitedu.bipa.quiz.vo.BookVO;
import bitedu.bipa.quiz.vo.UserVO;

public class LibraryDAO {

	private ConnectionManager manager;
	
	// 비즈니스 판별은 로직에서
	public LibraryDAO() {
		this.manager = ConnectionManager.getInstance();
	}
	
	
	public UserVO selectUser(String userId) throws SQLException {
		UserVO user = null;
		String sql = "select user_status,max_book,service_stop from book_user where user_id = ?";
		Connection con = manager.getConnection();
		PreparedStatement pstmt = con.prepareStatement(sql);
		pstmt.setString(1, userId);
		ResultSet rs = pstmt.executeQuery();
		while(rs.next()) {
			user = new UserVO();
			user.setUserId(userId);
			user.setUserState(rs.getString(1));
			user.setAvailableBook(rs.getInt(2));
			user.setServiceStop(rs.getTimestamp(3));
		}
		
		manager.closeConnection(rs, pstmt, con);
		return user;
	}
	

	public ArrayList<BookUseStatusVO> selectBookInfoByUser(String userId,String startMonth) throws SQLException {
		ArrayList<BookUseStatusVO> list = null;
		list = new ArrayList<BookUseStatusVO>();
		StringBuilder sb = new StringBuilder("select i.book_isbn,i.book_title,i.book_author,s.* ");
		sb.append("from book_copy c  inner join (book_info i) on c.book_isbn = i.book_isbn ");
		sb.append("inner join book_use_status s on s.book_seq = c.book_seq ");
		sb.append("where s.user_id = ? and s.borrow_start between '2023-6-1' and '2023-6-30'");
		String sql = sb.toString();
		Connection con = manager.getConnection();
		PreparedStatement pstmt = con.prepareStatement(sql);
		pstmt.setString(1, userId);
		ResultSet rs = pstmt.executeQuery();
		BookUseStatusVO vo = null;
		while(rs.next()) {
			vo = new BookUseStatusVO(rs.getInt(4), rs.getString(5), rs.getTimestamp(6), rs.getTimestamp(7), rs.getTimestamp(8));
			vo.setBookIsbn(rs.getString(1));
			vo.setBookTitle(rs.getString(2));
			vo.setBookAuthor(rs.getString(3));
			list.add(vo);
		}
		
		manager.closeConnection(rs, pstmt, con);
		
		return list;
	}
	
	public boolean updateUserStopStatus(String userId, Timestamp stopDate) throws SQLException {
		boolean flag = false;
		String sql = "update book_user set user_status= ?, service_stop = ? where user_id = ?";
		try {
			Connection con = manager.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql);
			pstmt.setString(1, "01");
			pstmt.setTimestamp(2, stopDate);
			pstmt.setString(3, userId);
			int affectedCount = pstmt.executeUpdate();
			if(affectedCount>0) {
				flag = true;
				//System.out.println("success");
			}
			manager.closeConnection(null, pstmt, con);
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return flag;
	}
	
	// book seq를 통해 book_isbn 가지고옴
			public String get_bookisbn(int index){
				Connection con = manager.getConnection();
				String sql = new StringBuilder().append("select book_isbn ")
												.append("from book_copy ")
												.append("where book_seq = ?")
												.toString();
				String result=null;
				try{
					PreparedStatement pstmt = con.prepareStatement(sql);
					pstmt.setInt(1, index);
					ResultSet rs = pstmt.executeQuery();
					if(rs.next()){
						result = rs.getString("book_isbn");
					}else{
						System.out.println("찾지못함");
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				return result;
			}
			
			// 책 대출 book_info 테이블과 book_copy 테이블 조인해서 책 상태 확인하고 해당 인덱스 번호에 해당하는 책 대출 가능한지 -> 책 상태가 BM-0001이면 대출가능 아니면 대출불가로 구분
			public boolean rent_Book(int index){
				Connection con = manager.getConnection();
				BookVO BVO = null;
				boolean flag = false;
				
				String sql = new StringBuilder().append("select a.book_isbn, book_title, book_author, book_published_date, book_seq, book_position, book_status ")
												.append("from book_info a ")
												.append("join book_copy b ")
												.append("on ? = b.book_isbn;")
												.toString();
				try{
					PreparedStatement pstmt = con.prepareStatement(sql);
					
					pstmt.setString(1, get_bookisbn(index));
					ResultSet rs = pstmt.executeQuery();
					if(rs.next()){
						BVO = new BookVO(rs.getInt("book_seq")
										, rs.getString("book_position")
										, rs.getString("book_status")
										, rs.getString("book_isbn"));
					}
					if(BVO.getBookStatus().equals("BM-0001")){
						flag = true;
					}else{
						flag = false;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				return flag;
			}
			
			// 주어진 사용자 id와 index 번호 받아서 대출하게 하는 것
			public boolean rent_Book_commit(String UserID, int bookNum){
				boolean flag = false;
				
				PreparedStatement pstmt = null;
				System.out.println(UserID);
				String sql = new StringBuilder().append("INSERT INTO book_use_status (book_seq, user_id, borrow_start, borrow_end) VALUES (?, ?, ?, ?);")
												.toString();

				try{
					Connection con = manager.getConnection();
					pstmt = con.prepareStatement(sql);
					pstmt.setInt(1, bookNum);
					pstmt.setString(2, UserID);
					pstmt.setString(3, DateCalculation.getDate());
					pstmt.setString(4, DateCalculation.get_return_day());
					
					
					if(pstmt.executeUpdate()>0){
						flag = true;
						bookcopy_update(UserID,bookNum, 1);
					}else{
						flag = false;
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
				return flag;
			}
			
			// 대출시 book_use_status 테이블의 책상태 변경하는 쿼리문
			public boolean bookcopy_update(String UserID, int bnum, int toggle){
				boolean flag = false;
				
				PreparedStatement pstmt = null;
				System.out.println(UserID);
				
				try{
					Connection con = manager.getConnection();
					if(toggle == 1) {
						String updateSql = "UPDATE book_copy SET book_position = CONCAT('BB-', ?) WHERE book_seq = ?";
						pstmt = con.prepareStatement(updateSql);
						pstmt.setString(1, UserID);
						pstmt.setInt(2, bnum);
					}
					else if(toggle == 2){
						String updateSql = "UPDATE book_copy SET book_position = 'BS-0001' WHERE book_seq = ?";
						pstmt = con.prepareStatement(updateSql);
						pstmt.setInt(1, bnum);
					}
					
			
					if(pstmt.executeUpdate()>0){
						flag = true;
					}else{
						flag = false;
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
				return flag;
			}
			
			
			// 책 반납 -> 사용자 아이디, 책 인덱스 번호 받아서 해당하는 book_use_status 테이블의 return_date 데이터 수정
			public boolean return_book(int index, String ID){
				boolean flag = false;
				// UPDATE book_use_status SET return_date = date_format(NOW(),'%Y-%m-%d')  WHERE user_id = 'user1' AND book_seq = 10;
				String sql = new StringBuilder().append("UPDATE book_use_status ")
												.append("SET return_date = date_format(NOW(),'%Y-%m-%d') ")
												.append("WHERE user_id = ? AND book_seq = ?")
												.toString();
				try{
					Connection con = manager.getConnection();
					PreparedStatement pstmt = con.prepareStatement(sql);
					
					pstmt.setString(1, ID);
					pstmt.setInt(2, index);
					
					if(pstmt.executeUpdate()>0){
						flag = true;
						bookcopy_update(ID,index, 2);
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
				return flag;
			}
			
			// 사용자 ID로 정보 받아서 조건에 맞는 사용자의 정보를 불러옴
			public UserVO getUSerVO(String ID){
				PreparedStatement pstmt = null;
				String sql = new StringBuilder().append("select * ")
												.append("from book_user ")
												.append("where user_id = ? ")										
												.toString();
				UserVO UVO = null;
				try{
					Connection con = manager.getConnection();
					pstmt = con.prepareStatement(sql);
					pstmt.setString(1, ID);			
					ResultSet rs = pstmt.executeQuery();
					if(rs.next()){
						UVO = new UserVO(rs.getInt("user_seq"), rs.getString("user_id"),
										 rs.getString("user_pass"), rs.getString("user_phone_number"),
										 rs.getString("user_status"), rs.getString("user_grade"),
										 rs.getInt("max_book"), null);
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
				
				return UVO;
			}
			
			
			
			
			
			
			
		
}
