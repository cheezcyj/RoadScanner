package com.roadscanner.board.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.google.gson.Gson;

public class StringUtil {

	
	
	
	/**
	 * Backand메시지 처리 
	 * @param msgId
	 * @param msgContents
	 * @return json문자
	 */
	public static String validMessageToJson(String msgId, String msgContents) {
		return new Gson().toJson(new MessageVO(msgId, msgContents));
	}
	
	/**
	 * 
	 * @param maxNum :  총글수
	 * @param currPageNo : 현제페이지
	 * @param rowPerPage : 10/20/30/50/100/200
	 * @param bottomCount: 10
	 * @param url        : server 호출 url
	 * @param scriptName : javascript function Name
	 * @return 
	 */
	public static String renderPaging(int maxNum, int currPageNo, int rowPerPage, int bottomCount, String url, String scriptName) {
//	    <!-- 페이징 -->
//	    <div class="d-flex justify-content-center">
//				<nav aria-label="Page navigation example">
//				  <ul class="pagination">
//				    <li class="page-item">
//				      <a class="page-link" href="#" aria-label="Previous">
//				        <span aria-hidden="true">&laquo;</span>
//				      </a>
//				    </li>
//				    <li class="page-item"><a class="page-link" href="#">1</a></li>
//				    <li class="page-item"><a class="page-link" href="#">2</a></li>
//				    <li class="page-item"><a class="page-link" href="#">3</a></li>
//				    <li class="page-item">
//				      <a class="page-link" href="#" aria-label="Next">
//				        <span aria-hidden="true">&raquo;</span>
//				      </a>
//				    </li>
//				  </ul>
//				</nav>    
//	    </div>
//	    <!--// 페이징 ---------------------------------------------------------------->
		
//      << < 1 2 3 4 5 6 7 8 9 10 > >>		
		//maxNum = 21
		//currPageNo = 1
		//rowPerPage = 10
		//bottomCount = 10
		
		int maxPageNo   = ((maxNum-1)/rowPerPage) +1;//3
		int startPageNo = ((currPageNo - 1)/bottomCount) * bottomCount +1;//1,11,21,...
		int endPageNo   = ((currPageNo - 1)/bottomCount+1) * bottomCount;//10,20,30,....
		
		int  nowBlockNo = ((currPageNo - 1)/bottomCount)+1;//1
		int  maxBlockNo = ((maxNum - 1)/bottomCount)+1;//3
		
		StringBuilder html=new StringBuilder(2000);//html 생성
		
		
		if(currPageNo > maxPageNo) {
			return "";
		}
		
		
//		html.append("<table   class=''> \n");
//		html.append("<tr> \n");
//		html.append("<td class='text-center' > \n");//가운데 정렬
		html.append("<ul class=\"pagination\"> \n");
		//-- << < 1 2 3 4 5 6 7 8 9 10 > >>		--------------------------------
		
		//<<
		if(nowBlockNo > 1 && nowBlockNo <= maxBlockNo) {
			html.append("<li  class=\"page-item\"> \n");
			html.append("<a class=\"page-link\" href=\"javascript:"+scriptName+"('"+url+"',1); \"> \n"); //doRetrive('user/doRetrieve.do',1)
			html.append("&laquo;");
			html.append("</a> \n");
			html.append("</li> \n");
		}
		
		
		//<
		if(startPageNo > bottomCount) {
			html.append("<li  class=\"page-item\"> \n");
			html.append("<a class=\"page-link\" href=\"javascript:"+scriptName+"('"+url+"',"+(startPageNo - bottomCount)+"); \"> \n"); 
			html.append("&lt;");
			html.append("</a> \n");
			html.append("</li> \n");			
		}
		
		//1 2 3 4 5 6 7 8 9 10
		int inx = 0;
		for(inx = startPageNo;inx<=maxPageNo && inx <=endPageNo;inx++) {
			if(inx == currPageNo) {//현제 페이지 이면 
				html.append("<li  class=\"page-item\"> \n");
				html.append("<a class=\"page-link disabled\" href='#'> \n");
				html.append(inx);
				html.append("</a> \n");
				html.append("</li> \n");
			}else {
				html.append("<li  class=\"page-item\"> \n");
				html.append("<a class=\"page-link\" href=\"javascript:"+scriptName+"('"+url+"',"+inx+"); \"> \n");
				html.append(inx);
				html.append("</a> \n");
				html.append("</li> \n");				
				
			}
		}
		
		
		//>
		if(maxPageNo > inx) {
			html.append("<li  class=\"page-item\"> \n");
			html.append("<a class=\"page-link\" href=\"javascript:"+scriptName+"('"+url+"',"+((nowBlockNo*bottomCount)+1)+"); \"> \n");
			html.append("&gt;");
			html.append("</a> \n");
			html.append("</li> \n");				
		}
		
		//>>
		if(maxPageNo > inx) {
			html.append("<li  class=\"page-item\"> \n");
			html.append("<a class=\"page-link\" href=\"javascript:"+scriptName+"('"+url+"',"+(maxPageNo)+"); \"> \n");
			html.append("&raquo;");
			html.append("</a> \n");
			html.append("</li> \n");				
		}
		
		
		
		//----------------------------------------------------------------------
		html.append("</ul> \n");
//		html.append("</td> \n");
//		html.append("</tr> \n");
//		html.append("</table> \n");
		
		
		
		return html.toString();
		
	}
	
	/**
	 * 날짜 pattern 리턴
	 * @param pattern  
	 * @return yyyyMMdd
	 */
	public static String getCurrentDate(String pattern) {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
	}
	
	/**
	 * PK를 날짜(8)+UUID(32)
	 * @return
	 */
	public static String getPK() {
		return getDate("yyyyMMdd")+getUUID();
	}
	
	/**
	 * 날짜를 형식문자로 생성
	 * default : yyyyMMdd
	 * @param format(yyyy-MM-dd HH:mm:ss)
	 * @return 
	 */
	public static String getDate(String format) {
		String date = "";
		if(null == format || format.equals("")) {
			format="yyyyMMdd";
		}
		
		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
		date = currentDateTime.format(formatter);
		return date;
	}
	
	/**
	 * 32byte uuid생성
	 * @return 32bit uuid
	 */
	public static String getUUID() {
		String pk = "";
		
		UUID uuid = UUID.randomUUID();
		pk = uuid.toString();
		pk = pk.replace("-", "");
		
		return pk;
	}
	
	
	
	
	
	
}

