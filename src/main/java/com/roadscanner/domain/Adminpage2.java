package com.roadscanner.domain;


public class Adminpage2 {
	// 현재 페이지 번호
	private int num2;
	
	// 게시물 총 갯수
	private int count2;

	// 한 페이지에 출력할 게시물 갯수
	private int postNum2 = 5;

	// 하단 페이징 번호 ([ 게시물 총 갯수 ÷ 한 페이지에 출력할 갯수 ]의 올림)
	private int pageNum2;

	// 출력할 게시물
	private int displayPost2;

	// 한번에 표시할 페이징 번호의 갯수
	private int pageNumCnt2 = 5;

	// 표시되는 페이지 번호 중 마지막 번호
	private int endPageNum2;

	// 표시되는 페이지 번호 중 첫번째 번호
	private int startPageNum2;

	// 다음/이전 표시 여부
	private boolean prev2;
	private boolean next2;
	
	public void setNum2(int num2) {
	 this.num2 = num2;
	}


	public int getPostNum2() {
	 return postNum2;
	}
	
	
	
	public int getPageNum2() {
	 return pageNum2;
	}

	public int getDisplayPost2() {
	 return displayPost2;
	}

	public int getPageNumCnt2() {
	 return pageNumCnt2;
	}

	public int getEndPageNum2() {
	 return endPageNum2;
	}

	public int getStartPageNum2() {
	 return startPageNum2;
	}

	public boolean getPrev2() {
	 return prev2;
	} 

	public boolean getNext2() {
	 return next2;
	}
	

	public void setCount2(int count2) {
	 this.count2 = count2;
	 dataCalc2();
	}
	
	public int getCount2() {
		 return count2;
		}
	
	private void dataCalc2() {
	
		 endPageNum2 = (int)(Math.ceil((double)num2 / (double)pageNumCnt2) * pageNumCnt2);
		 
		 // 시작 번호
		 startPageNum2 = endPageNum2 - (pageNumCnt2 - 1);
		 
		 // 마지막 번호 재계산
		 int endPageNum_tmp2 = (int)(Math.ceil((double)count2 / (double)postNum2));
		 
		 if(endPageNum2 > endPageNum_tmp2) {
		  endPageNum2 = endPageNum_tmp2;
		 }
		 
		 prev2 = startPageNum2 == 1 ? false : true;
		 next2 = endPageNum2 * postNum2 >= count2 ? false : true;
		 
		 displayPost2 = ((num2 - 1) * postNum2)+1;
		 
		}

	
	public String getSearchTypeKeywordBox() {
		 
		 if(keyword2.equals("")
			) {
		  return ""; 
		 }else{
		  return "&keyword2="+keyword2;			
		 	}	
		}
	
		private String keyword2; 	
		private String nekeyword; 	

        public String getNekeyword() {
			return nekeyword;
		}


		public void setNekeyword(String nekeyword) {
			this.nekeyword = nekeyword;
		}

		private String pageid2;

		public String getPageid2() {
			return pageid2;
		}


		public void setPageid2(String pageid2) {
			this.pageid2 = pageid2;
		}


		public void setKeyword2(String keyword2) {
		 this.keyword2 = keyword2;  
		} 

		public String getKeyword2() {
		 return keyword2;
		}
		
}


