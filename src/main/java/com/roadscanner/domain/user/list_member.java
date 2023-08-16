package com.roadscanner.domain.user;


public class list_member {
	// 현재 페이지 번호
	private int num;
	
	// 게시물 총 갯수
	private int count;

	// 한 페이지에 출력할 게시물 갯수
	private int postNum = 5;

	// 하단 페이징 번호 ([ 게시물 총 갯수 ÷ 한 페이지에 출력할 갯수 ]의 올림)
	private int pageNum;

	// 출력할 게시물
	private int dpPost;

	// 한번에 표시할 페이징 번호의 갯수
	private int pageNumCnt = 5;

	// 표시되는 페이지 번호 중 마지막 번호
	private int endPageNum;

	// 표시되는 페이지 번호 중 첫번째 번호
	private int startPageNum;

	// 다음/이전 표시 여부
	private boolean prev;
	private boolean next;
	
	public void setNum(int num) {
	 this.num = num;
	}


	public int getPostNum() {
	 return postNum;
	}
	
	
	
	public int getPageNum() {
	 return pageNum;
	}

	public int getdpPost() {
	 return dpPost;
	}

	public int getPageNumCnt() {
	 return pageNumCnt;
	}

	public int getEndPageNum() {
	 return endPageNum;
	}

	public int getStartPageNum() {
	 return startPageNum;
	}

	public boolean getPrev() {
	 return prev;
	} 

	public boolean getNext() {
	 return next;
	}
	

	public void setCount(int count) {
	 this.count = count;
	 dataCalc();
	}
	
	public int getCount() {
		 return count;
		}
	
	private void dataCalc() {
	
		 endPageNum = (int)(Math.ceil((double)num / (double)pageNumCnt) * pageNumCnt);
		 
		 // 시작 번호
		 startPageNum = endPageNum - (pageNumCnt - 1);
		 
		 // 마지막 번호 재계산
		 int endPageNum_tmp = (int)(Math.ceil((double)count / (double)postNum));
		 
		 if(endPageNum > endPageNum_tmp) {
		  endPageNum = endPageNum_tmp;
		 }
		 
		 prev = startPageNum == 1 ? false : true;
		 next = endPageNum * postNum >= count ? false : true;
		 
		 dpPost = ((num - 1) * postNum)+1;
		 
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
		
        private String pageid;
        
        private String exclude; 
        
        
		public String getExclude() {
			return exclude;
		}


		public void setExclude(String exclude) {
			this.exclude = exclude;
		}


		public String getPageid() {
			return pageid;
		}


		public void setPageid(String pageid) {
			this.pageid = pageid;
		}


		public void setKeyword(String keyword) {
		 this.keyword = keyword;  
		} 

		public String getKeyword() {
		 return keyword;
		}
		
}


