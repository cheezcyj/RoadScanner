package com.roadscanner.controller.user;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.roadscanner.domain.Adminpage;
import com.roadscanner.domain.Adminpage2;
import com.roadscanner.domain.Adminpage3;
import com.roadscanner.domain.MemberVO;
import com.roadscanner.service.user.AdminService;

@Controller
public class AdminpageController {

@Inject
AdminService service;
	
	@RequestMapping(value = "/memberAdmin", method = RequestMethod.GET)
	public void memberAdmin (Model model, 
			@RequestParam(value = "num",required = false, defaultValue = "1") int num,
			@RequestParam(value = "keyword",required = false, defaultValue = "") String keyword,	
			@RequestParam(value = "pageid",required = false, defaultValue = "") String pageid) throws Exception {
		
	Adminpage page = new Adminpage();
	
	page.setNum(num);
	page.setCount(service.searchCountBox(keyword)); 
	page.setKeyword(keyword);
	page.setPageid(pageid);
	
	List<MemberVO> list = null;
	list = service.listPageSearchBox(page.getDisplayPost(), page.getPostNum(), keyword);
		
	model.addAttribute("list", list);   
	model.addAttribute("page", page);
	model.addAttribute("select", num);


			
}

@RequestMapping(value = "/memberAdmin2", method = RequestMethod.GET)
public void memberAdmin2 
		(Model model, 
		@RequestParam(value = "num2",required = false, defaultValue = "1") int num2,
		@RequestParam(value = "keyword2",required = false, defaultValue = "") String keyword2,
		@RequestParam(value = "nekeyword",required = false, defaultValue = "") String nekeyword,
		@RequestParam(value = "pageid2",required = false, defaultValue = "") String pageid2
		
				) 
		 
		throws Exception {
	
Adminpage2 page2 = new Adminpage2();

page2.setNum2(num2);

page2.setCount2(service.searchCountBox2(
		nekeyword,
		keyword2
		)); 

page2.setKeyword2(keyword2);
page2.setPageid2(pageid2);

List<MemberVO> list2 = null;
list2 = service.listPageSearchBox2(
		page2.getDisplayPost2(), 
		page2.getPostNum2(), 
		nekeyword,
		keyword2
);



model.addAttribute("list2", list2);   
model.addAttribute("page2", page2);
model.addAttribute("select2", num2);
}


	@RequestMapping(value = "/memberAdmin3", method = RequestMethod.GET)
	public void memberAdmin3(Model model, 
			@RequestParam(value = "num3",required = false, defaultValue = "1") int num3,
			@RequestParam(value = "keyword3",required = false, defaultValue = "") String keyword3,	
			@RequestParam(value = "pageid3",required = false, defaultValue = "") String pageid3) throws Exception {
		
		Adminpage3 page3 = new Adminpage3();
		
		page3.setNum3(num3);
		page3.setCount3(service.searchCountBox3(keyword3)); 
		page3.setKeyword3(keyword3);
		page3.setPageid3(pageid3);
		
		List<MemberVO> list3 = null;
		list3 = service.listPageSearchBox3(page3.getDisplayPost3(), page3.getPostNum3(), keyword3);
		
		model.addAttribute("list3", list3);   
		model.addAttribute("page3", page3);
		model.addAttribute("select3", num3);
				
		}

}		