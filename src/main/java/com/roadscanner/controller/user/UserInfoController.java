package com.roadscanner.controller.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.domain.MemberVO;
import com.roadscanner.service.user.UserService;

@Controller
public class UserInfoController {
	
	@Autowired
	UserService userService;
	
	@RequestMapping(value = "/mypage", method = RequestMethod.GET)
	public String myPage(HttpServletRequest request) throws Exception {
		
		HttpSession session = request.getSession();
		MemberVO memberVO = (MemberVO) session.getAttribute("user");
		
		System.out.println("= = = = = = = = = = = = = = = = = =");
		System.out.println("로그인 성공, 마이페이지 시작");
		System.out.println("= = = = = = = = = = = = = = = = = =");
		
		return "mypage";
		
	}
	
	@RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doChangeInfo(MemberVO member) throws Exception {
		System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│ doChangeInfo()                                   │");
        System.out.println("└────────────────────────────────────────────────────────┘");
        
        int flag = this.userService.doChangeInfo(member);
        
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        if(1 == flag) {
        	message.setMsgId("10");
        	message.setMsgContents("비빌번호를 수정했습니다");
        } else {
        	message.setMsgId("20");
        	message.setMsgContents("회원정보 수정에 실패했습니다.");
        }
        
       jsonString = new Gson().toJson(message);
       
       return jsonString;
        
	}
	
}
