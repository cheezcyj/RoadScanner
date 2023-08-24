package com.roadscanner.controller.user;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.domain.user.list_admin;
import com.roadscanner.service.user.MailSendService;
import com.roadscanner.service.user.UserService;

/**
 * Handles requests for the application home page.
 */

@Controller
public class LoginController {	
	
	final Logger LOG = LoggerFactory.getLogger(LoginController.class);
	
	@Autowired
	UserService userService;
	
	@Autowired
	MailSendService mailSend;
	
	// 메인 페이지 접속
	@GetMapping("/main")
	public String main() {
		
		LOG.debug("메인페이지 시작");
		return "/login/main";		
	}
	
	// 로그인 페이지 접속
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String loginPageStart() {
		
		LOG.debug("로그인 화면 이동");
		return "/login/login";		
	}
	
	//관리자 페이지 접속
	@RequestMapping(value = "/admin", method = RequestMethod.GET)
	public String admin() {
		
		LOG.debug("관리자 화면 이동");
		return "/login/admin";	
	}
	
	// 회원가입 페이지 접속
	@RequestMapping("/registerpage")
    public String registerpage() {
		
		LOG.debug("회원가입 화면 이동");
        return "/login/registerpage";       
    }
	
	// ID & PW찾기 페이지 접속
	@RequestMapping(value = "/findIdPw", method = RequestMethod.GET)
	public String findIdPwStart() {
		
		LOG.debug("ID & PW 찾기 화면 이동");
		return "/login/findIdAndPw";
	}
	
	// 로그아웃 : 셰션 제거 호출
    @GetMapping("/logout")
    public String logoutButtonEvent(HttpSession session) {
    	
        LOG.debug("로그아웃 실행");
		session.invalidate();
		return "redirect:/login";	
	}
    
    // 회원가입 메일인증 서비스 호출
    @GetMapping("/**/mailCheck")
	@ResponseBody
	public String mailCheck(String email) throws UnsupportedEncodingException, MessagingException {
    	
		LOG.debug("회원가입 이메일 인증 요청 시작");
		return mailSend.joinEmail(email);
	}
    
    // 아이디 찾기 메일인증 서비스 호출
    @PostMapping("/**/toEmailFindId")
	@ResponseBody
	public String findId(String email, String id) throws UnsupportedEncodingException, MessagingException {
    	
    	LOG.debug("아이디찾기 이메일 인증 요청 시작");
		return mailSend.findId(email, id);
	}
	
    // 로그인 실행
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody         //해당 내용이 화면이 아닌 데이터만 던진자고 알려주는 것임
    public String loginButtonEvent(MemberVO user,list_admin page, Model model, HttpSession httpSession ) throws SQLException {        
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController login()                                │");

        String jsonString = "";        

        LOG.debug("│ user : "+user);
        MessageVO message = new MessageVO();
        
        // (1 : id 미입력)
        if(null == user.getId() || "".equals(user.getId())) {
            message.setMsgId("1");
            message.setMsgContents("아이디를 입력하세요.");
            return new Gson().toJson(message);        
        }
        // (2 : pass 미입력)
        if(null == user.getPassword() || "".equals(user.getPassword())) {
            message.setMsgId("2");
            message.setMsgContents("비밀번호를 입력하세요.");
            return new Gson().toJson(message);        
        }
        
        // 10: id 오류, 20: PW 오류, 30: 성공, 40: 정지 ID
        int status = this.userService.doLogin(user);        
        if(10 == status) {
            message.setMsgId("10");
            message.setMsgContents("아이디를 확인하세요.");
            
        } else if(20 == status) {      	
            message.setMsgId("20");
            message.setMsgContents("비밀번호를 확인하세요.");
            
        } else if(30 == status) {      	
            message.setMsgId("30");
            message.setMsgContents(user.getId()+"가 로그인 되었습니다.");
            page.setkeyword(user.getId());     
            //----------------------------------------------------------
            //- 사용자 정보 조회 : session처리
            //----------------------------------------------------------
            MemberVO userInfo = userService.selectUser(user);
            if(null!=userInfo) {
                httpSession.setAttribute("user", userInfo);
            }
            
        } else if(40 == status) {
        	message.setMsgId("40");
            message.setMsgContents(user.getId()+"가 정지되었습니다.");
            
        } else {
            message.setMsgId("99");
            message.setMsgContents("알수 없는 오류");            
        }
        
        jsonString = new Gson().toJson(message);
        
        LOG.debug("│ jsonString : "+jsonString);
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return jsonString;        		
	}
    
    // ID 찾기
    @RequestMapping(value = "/findId", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody         
    public String findId(MemberVO user, HttpSession httpSession) throws SQLException {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController findId()                               │");
     
        String jsonString = "";    
        MessageVO message = new MessageVO();       

        String result = "-1";
        result = this.userService.doSearchId(user);  
        
        // 10: id 오류
        if("-1".equals(result)) {         
            message.setMsgId("10");
            message.setMsgContents("이메일을 확인해주세요.");
            
        } else {                    
            message.setMsgId("30");
            message.setMsgContents(result);
            
        }
        
        jsonString = new Gson().toJson(message);        
        LOG.debug("│ jsonString : "+jsonString);
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return jsonString;    
    }

    // 비밀번호 찾기
    @RequestMapping(value = "/findPw", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody     
    public String findPw(MemberVO user, HttpSession httpSession) throws SQLException {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController findPw()                               │");

        String jsonString = "";    
        MessageVO message = new MessageVO();

        String pwresult = "-1";
        pwresult = this.userService.doSearchPw(user);   
        
        // 10: id 오류
        if("-1".equals(pwresult)) {         
            message.setMsgId("10");
            message.setMsgContents("아이디와 이메일을 확인해주세요.");
            
        } else {                    
            message.setMsgId("30");
            message.setMsgContents("비밀번호 재설정 페이지로 이동합니다.");
        }
        
        jsonString = new Gson().toJson(message);        
        LOG.debug("│ jsonString : "+jsonString);
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return jsonString;    
    }
    
    // 회원가입 아이디 중복체크
    @RequestMapping(value = "/idDulpCheck", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String membershipIdCheck(MemberVO user, HttpSession httpSession) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController membershipIdCheck()                    │");
     
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        int result = 0;
        result = this.userService.doIdDuplCheck(user);
        
        // 10: 중복 존재, 20: 중복 없음
        if(10 == result) {
        	message.setMsgId("10");
        	message.setMsgContents("해당 ID는 사용할 수 없습니다");
        } else if(20 == result) {
        	message.setMsgId("20");
        	message.setMsgContents("사용할 수 있는 ID입니다");
        } 
        
       jsonString = new Gson().toJson(message);
       LOG.debug("│ jsonString : "+jsonString);
       LOG.debug("└────────────────────────────────────────────────────────┘");
        
        return jsonString;
	}
    
    // 회원가입 이메일 중복체크
    @RequestMapping(value = "/emailDulpCheck", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String emailDulpCheck(MemberVO user, HttpSession httpSession) throws SQLException {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController membershipIdCheck()                    │");

        String jsonString = "";
        MessageVO message = new MessageVO();
        
        int result = 0;
        result = this.userService.doEmailDuplCheck(user);
        
        // 10: 중복 존재, 20: 중복 없음
        if(10 == result) {
        	message.setMsgId("10");
        	message.setMsgContents("해당 이메일은 사용할 수 없습니다");
        	
        } else if(20 == result) {
        	message.setMsgId("20");
        	message.setMsgContents("사용할 수 있는 이메일입니다");
        } 
        
       jsonString = new Gson().toJson(message);
       LOG.debug("│ jsonString : "+jsonString);
       LOG.debug("└────────────────────────────────────────────────────────┘");
        
       return jsonString;
	}
	
    // 회원가입
	@RequestMapping(value = "/register", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String membershipRegister(MemberVO user) throws Exception {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController membershipRegister()                    │");
        LOG.debug("│ user : "+ user.toString());
        LOG.debug("└────────────────────────────────────────────────────────┘");
       
		int flag = this.userService.register(user);
		
		String jsonString = "";
		MessageVO message = new MessageVO();
		
		if(10 == flag) {
			message.setMsgId("10");
			message.setMsgContents("축하합니다, 회원가입에 성공했습니다");
			
		} else if(20 == flag){
			message.setMsgId("20");
			message.setMsgContents("회원가입에 실패했습니다");
		}	
		
		jsonString = new Gson().toJson(message);
		
		return jsonString;
	}
		
}