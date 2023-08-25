package com.roadscanner.domain.user;


public class list_admin {
	// 현재 페이지 번호
	private int num;
	
	// 게시물 총 갯수
	private int count;

	// 한 페이지에 출력할 게시물 갯수
	private int postnum = 5;

	// 하단 페이징 번호 ([ 게시물 총 갯수 ÷ 한 페이지에 출력할 갯수 ]의 올림)
	private int pagenum;

	// 출력할 게시물
	private int dpPost;

	// 한번에 표시할 페이징 번호의 갯수
	private int pageNumCnt = 5;

	// 표시되는 페이지 번호 중 마지막 번호
	private int endPagenum;

	// 표시되는 페이지 번호 중 첫번째 번호
	private int startPagenum;

	// 다음/이전 표시 여부
	private boolean prev;
	private boolean next;
	
	public void setnum(int num) {
	 this.num = num;
	}


	public int getPostnum() {
	 return postnum;
	}
	
	
	
	public int getPagenum() {
	 return pagenum;
	}

	public int getdpPost() {
	 return dpPost;
	}

	public int getpageNumCnt() {
	 return pageNumCnt;
	}

	public int getEndPagenum() {
	 return endPagenum;
	}

	public int getStartPagenum() {
	 return startPagenum;
	}

	public boolean getprev() {
	 return prev;
	} 

	public boolean getnext() {
	 return next;
	}
	

	public void setcount(int count) {
	 this.count = count;
	 dataCalc();
	}
	
	public int getcount() {
		 return count;
		}
	
	private void dataCalc() {
	
		 endPagenum = (int)(Math.ceil((double)num / (double)pageNumCnt) * pageNumCnt);
		 
		 // 시작 번호
		 startPagenum = endPagenum - (pageNumCnt - 1);
		 
		 // 마지막 번호 재계산
		 int endPageNum_tmp = (int)(Math.ceil((double)count / (double)postnum));
		 
		 if(endPagenum > endPageNum_tmp) {
		  endPagenum = endPageNum_tmp;
		 }
		 
		 prev = startPagenum == 1 ? false : true;
		 next = endPagenum * postnum >= count ? false : true;
		 
		 dpPost = ((num - 1) * postnum)+1;
		 
		}

	
	public String getSearchTypeKeywordBox() {
		 
		 if(keyword.equals("")
			) {
		  return ""; 
		 }else{
		  return "&keyword="+keyword;			
		 	}	
		}
	
		private String keyword; 	
		private String exclude; 	

        public String getexclude() {
			return exclude;
		}


		public void setexclude(String exclude) {
			this.exclude = exclude;
		}

		private String pageid;

		public String getpageid() {
			return pageid;
		}


		public void setpageid(String pageid) {
			this.pageid = pageid;
		}


		public void setkeyword(String keyword) {
		 this.keyword = keyword;  
		} 

		public String getkeyword() {
		 return keyword;
		}
		
}


