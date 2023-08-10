package com.roadscanner.domain;


public class Adminpage3 {
	// 현재 페이지 번호
	private int num3;
	
	// 게시물 총 갯수
	private int count3;

	// 한 페이지에 출력할 게시물 갯수
	private int postNum3 = 5;

	// 하단 페이징 번호 ([ 게시물 총 갯수 ÷ 한 페이지에 출력할 갯수 ]의 올림)
	private int pageNum3;

	// 출력할 게시물
	private int displayPost3;

	// 한번에 표시할 페이징 번호의 갯수
	private int pageNumCnt3 = 5;

	// 표시되는 페이지 번호 중 마지막 번호
	private int endPageNum3;

	// 표시되는 페이지 번호 중 첫번째 번호
	private int startPageNum3;

	// 다음/이전 표시 여부
	private boolean prev3;
	private boolean next3;
	
	public void setNum3(int num3) {
	 this.num3 = num3;
	}


	public int getPostNum3() {
	 return postNum3;
	}
	
	
	
	public int getPageNum3() {
	 return pageNum3;
	}

	public int getDisplayPost3() {
	 return displayPost3;
	}

	public int getPageNumCnt3() {
	 return pageNumCnt3;
	}

	public int getEndPageNum3() {
	 return endPageNum3;
	}

	public int getStartPageNum3() {
	 return startPageNum3;
	}

	public boolean getPrev3() {
	 return prev3;
	} 

	public boolean getNext3() {
	 return next3;
	}
	

	public void setCount3(int count3) {
	 this.count3 = count3;
	 dataCalc3();
	}
	
	public int getCount3() {
		 return count3;
		}
	
	private void dataCalc3() {
	
		 endPageNum3 = (int)(Math.ceil((double)num3 / (double)pageNumCnt3) * pageNumCnt3);
		 
		 // 시작 번호
		 startPageNum3 = endPageNum3 - (pageNumCnt3 - 1);
		 
		 // 마지막 번호 재계산
		 int endPageNum_tmp3 = (int)(Math.ceil((double)count3 / (double)postNum3));
		 
		 if(endPageNum3 > endPageNum_tmp3) {
		  endPageNum3 = endPageNum_tmp3;
		 }
		 
		 prev3 = startPageNum3 == 1 ? false : true;
		 next3 = endPageNum3 * postNum3 >= count3 ? false : true;
		 
		 displayPost3 = ((num3 - 1) * postNum3)+1;
		 
		}

	
	public String getSearchTypeKeywordBox() {
		 
		 if(keyword3.equals("")
			) {
		  return ""; 
		 }else{
		  return "&keyword3="+keyword3;			
		 	}	
		}
	
		private String keyword3; 	

        private String pageid3;

		public String getPageid3() {
			return pageid3;
		}


		public void setPageid3(String pageid3) {
			this.pageid3 = pageid3;
		}


		public void setKeyword3(String keyword3) {
		 this.keyword3 = keyword3;  
		} 

		public String getKeyword3() {
		 return keyword3;
		}
		
}


