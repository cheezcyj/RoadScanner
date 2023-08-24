package com.roadscanner.controller.user;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.MailSendService;
import com.roadscanner.service.user.UserService;

@Controller
public class UserInfoController {
	
	@Autowired
	UserService userService;
	
	@Autowired
	MailSendService mailSend;
	
	final Logger LOG = LogManager.getLogger(getClass());
	
	// 마이페이지 시작
	@RequestMapping(value = "/mypage", method = RequestMethod.GET)
	public String myPage(HttpServletRequest request) throws Exception {
		
		HttpSession session = request.getSession();
		MemberVO member = (MemberVO) session.getAttribute("user");	
		LOG.debug("로그인 성공, 마이페이지 시작");		
		return "/login/mypage";		
	}
	
	// 비밀번호 재설정 페이지
	@RequestMapping("/changePw")
    public String changePw() {
		
		LOG.debug("비밀번호 재설정 페이지 이동");
        return "/login/changePw";     
    }
	
    // 비밀번호 재설정 페이지 인증번호 전송
    @GetMapping("/**/change_mailCheck")
	@ResponseBody
	public String change_mailCheck(String email) throws UnsupportedEncodingException, MessagingException {
    	
		LOG.debug("비밀번호 재설정 페이지 이메일 인증 요청");
		return mailSend.change_mailCheck(email);
	}
	
	// 마이페이지 비밀번호 수정
	@RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doChangeInfo(MemberVO member) throws Exception {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ UserInfoController doChangeInfo()                      │");
        LOG.debug("└────────────────────────────────────────────────────────┘");
             
        int flag = this.userService.doChangeInfo(member);
        String jsonString = "";
        MessageVO message = new MessageVO();
              
        if(flag == 1) {
        	message.setMsgId("10");
        	message.setMsgContents("비빌번호를 수정했습니다");
        	
        }
        
        if(flag == 3) {
        	message.setMsgId("30");
        	message.setMsgContents("현재와 같은  비밀번호입니다.");

        }
        
        if(flag != 1 && flag != 3) {
        	message.setMsgId("20");
        	message.setMsgContents("회원정보 수정에 실패했습니다.");
        }
        
       jsonString = new Gson().toJson(message);
       return jsonString;
        
	}
	
	// 비밀번호 재설정할 때, 가입한 이메일 체크
	@RequestMapping(value = "/emailCheck", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String emailCheck(MemberVO user, HttpSession httpSession) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ UserInfoController emailCheck()                        │");
             
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        int result = 0;
        result = this.userService.doEmailDuplCheck(user);
        
        if(10 == result) {
        	message.setMsgId("10");
        	message.setMsgContents("가입된 이메일입니다, 인증번호를 전송하시겠습니까?");
        	
        } else if(20 == result) {
        	message.setMsgId("20");
        	message.setMsgContents("가입된 이메일이 없습니다.");
        } 
        
       jsonString = new Gson().toJson(message);
       LOG.debug("│ jsonString : "+jsonString);       
       LOG.debug("└────────────────────────────────────────────────────────┘");     
       return jsonString;
	}	
    
    // 비밀번호 재설정 페이지 > 비밀번호 수정
    @RequestMapping(value = "/changePassword", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String changePw(MemberVO member) throws Exception {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ UserInfoController changePw()                          │");
        LOG.debug("└────────────────────────────────────────────────────────┘");
        
        int flag = this.userService.changePw(member);
        
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        if(1 == flag) {
        	message.setMsgId("10");
        	message.setMsgContents("비빌번호를 재설정했습니다");
        	
        } else {
        	message.setMsgId("20");
        	message.setMsgContents("비밀번호 재설정에 실패했습니다.");
        }
        
       jsonString = new Gson().toJson(message);  
       return jsonString;
        
	}
	
}
