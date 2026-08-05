package com.roadscanner.controller.user;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.roadscanner.cmn.MessageVO;
import com.roadscanner.cmn.validation.CredentialPolicy;
import com.roadscanner.config.ClientAddressResolver;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.service.user.EmailVerificationService;
import com.roadscanner.service.user.EmailVerificationService.Purpose;
import com.roadscanner.service.user.EmailVerificationService.RateLimitExceededException;
import com.roadscanner.service.user.EmailVerificationService.VerificationOutcome;
import com.roadscanner.service.user.EmailVerificationService.VerificationResult;
import com.roadscanner.service.user.LoginAttemptGuard;
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

	@Autowired
	EmailVerificationService emailVerificationService;

	@Autowired(required = false)
	ClientAddressResolver clientAddressResolver;

	LoginAttemptGuard loginAttemptGuard = new LoginAttemptGuard();

	@Autowired
	void setLoginAttemptGuard(LoginAttemptGuard loginAttemptGuard) {
		this.loginAttemptGuard = loginAttemptGuard;
	}
	
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
    @PostMapping("/logout")
    public String logoutButtonEvent(HttpSession session) {
    	
        LOG.debug("로그아웃 실행");
		session.invalidate();
		return "redirect:/login";	
	}
    
	// 회원가입 인증번호 전송. 인증번호는 HTTP 응답에 포함하지 않는다.
    @PostMapping(value = "/mailCheck", produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String mailCheck(String email, HttpSession session, HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException {
    	
		LOG.debug("회원가입 이메일 인증 요청 시작");
		String normalizedEmail;
		try {
			normalizedEmail = emailVerificationService.normalizeEmail(email);
		} catch (IllegalArgumentException exception) {
			return messageJson("20", "올바른 이메일 주소를 입력해주세요.");
		}

		String verificationCode;
		try {
			verificationCode = emailVerificationService.createChallenge(
					session,
					normalizedEmail,
					Purpose.REGISTRATION,
					clientAddress(request));
		} catch (RateLimitExceededException exception) {
			return messageJson("20", "인증번호 전송 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
		}
		try {
			mailSend.sendRegistrationVerification(normalizedEmail, verificationCode);
		} catch (UnsupportedEncodingException | MessagingException | RuntimeException exception) {
			emailVerificationService.clearChallenge(
					session,
					normalizedEmail,
					Purpose.REGISTRATION,
					verificationCode);
			throw exception;
		}
		return messageJson("10", "인증번호가 전송되었습니다.");
	}

	String mailCheck(String email, HttpSession session)
			throws UnsupportedEncodingException, MessagingException {
		return mailCheck(email, session, null);
	}

	@PostMapping(value = "/mailCheck/verify", produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String verifyRegistrationEmail(
			String email,
			String code,
			HttpSession session,
			HttpServletResponse response) {
		VerificationOutcome outcome = emailVerificationService.verify(
				session,
				email,
				Purpose.REGISTRATION,
				code);
		VerificationResult result = outcome.getResult();
		if (VerificationResult.VERIFIED == result) {
			response.setHeader("Cache-Control", "no-store");
			response.setHeader("Pragma", "no-cache");
			response.setHeader("X-Email-Verification-Token", outcome.getProofToken());
			return messageJson("10", "이메일 인증이 완료되었습니다.");
		}
		if (VerificationResult.EXPIRED == result) {
			return messageJson("20", "인증번호가 만료되었습니다. 다시 전송해주세요.");
		}
		if (VerificationResult.LOCKED == result) {
			return messageJson("20", "인증 시도 횟수를 초과했습니다. 다시 전송해주세요.");
		}
		return messageJson("20", "이메일 또는 인증번호를 확인해주세요.");
	}
    
    // 로그인 실행
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody         //해당 내용이 화면이 아닌 데이터만 던진자고 알려주는 것임
    public String loginButtonEvent(MemberVO user, HttpServletRequest request) throws SQLException {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController login()                                │");

        String jsonString = "";        

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

		String clientAddress = clientAddress(request);
		if (loginAttemptGuard.isBlocked(user.getId(), clientAddress)) {
			return loginFailureJson();
		}
        
        // 10: id 오류, 20: PW 오류, 30: 성공, 40: 정지 ID
        int status = this.userService.doLogin(user);        
        if(10 == status || 20 == status || 40 == status) {
			loginAttemptGuard.recordFailure(user.getId(), clientAddress);
            message.setMsgId("20");
            message.setMsgContents("로그인 정보 또는 계정 상태를 확인하세요.");
            
        } else if(30 == status) {      	
            message.setMsgId("30");
            message.setMsgContents("로그인되었습니다.");
            //----------------------------------------------------------
            //- 사용자 정보 조회 : session처리
            //----------------------------------------------------------
            MemberVO userInfo = userService.selectUser(user);
			if (userInfo == null
					|| userInfo.getCredentialVersion() != user.getCredentialVersion()
					|| userInfo.getGrade() != user.getGrade()) {
				loginAttemptGuard.recordFailure(user.getId(), clientAddress);
				return loginFailureJson();
            }
			loginAttemptGuard.recordSuccess(user.getId());
			// 인증 정보는 세션이나 JSP 모델에 보관하지 않는다.
			userInfo.setPassword(null);
			HttpSession existingSession = request.getSession(false);
			if (existingSession != null) {
				existingSession.invalidate();
			}
			request.getSession(true).setAttribute("user", userInfo);
            
        } else {
			loginAttemptGuard.recordFailure(user.getId(), clientAddress);
			message.setMsgId("20");
			message.setMsgContents("로그인 정보 또는 계정 상태를 확인하세요.");
        }
        
        jsonString = new Gson().toJson(message);
        
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return jsonString;        		
	}
    
    // ID 찾기
    @RequestMapping(value = "/findId", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody         
    public String findId(MemberVO user, HttpSession httpSession, HttpServletRequest request) {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController findId()                               │");
     
        String jsonString = "";    
        MessageVO message = new MessageVO();       

        try {
            String normalizedEmail = emailVerificationService.normalizeEmail(user.getEmail());
            emailVerificationService.reserveSend(
                    httpSession,
                    normalizedEmail,
                    Purpose.ID_RECOVERY,
                    clientAddress(request));
            user.setEmail(normalizedEmail);
            String foundId = this.userService.doSearchId(user);
            if (!"-1".equals(foundId) && !"2".equals(foundId)) {
                mailSend.findId(normalizedEmail, foundId);
            }
        } catch (RateLimitExceededException exception) {
            LOG.debug("id_recovery_rate_limited");
        } catch (Exception exception) {
            // 계정 존재 여부와 메일 전송 결과를 HTTP 응답으로 노출하지 않는다.
			LOG.warn("id_recovery_mail_delivery_failed");
        }

	    	message.setMsgId("30");
	    	message.setMsgContents("가입된 계정이 있다면 입력한 이메일로 안내를 보냈습니다.");
        
        jsonString = new Gson().toJson(message);        
        LOG.debug("└────────────────────────────────────────────────────────┘");
        return jsonString;    
    }

	String findId(MemberVO user, HttpSession session) {
		return findId(user, session, null);
	}

	private String loginFailureJson() {
		return messageJson("20", "로그인 정보 또는 계정 상태를 확인하세요.");
	}

	private String clientAddress(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		return clientAddressResolver == null
				? request.getRemoteAddr()
				: clientAddressResolver.resolve(request);
	}

    // 비밀번호 찾기
    @RequestMapping(value = "/findPw", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody     
    public String findPw(MemberVO user, HttpSession httpSession) {
        LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController findPw()                               │");

        String jsonString = "";    
        MessageVO message = new MessageVO();

	    	message.setMsgId("30");
	    	message.setMsgContents("입력 정보를 공개하지 않고 비밀번호 재설정 페이지로 이동합니다.");
        
        jsonString = new Gson().toJson(message);        
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
        
        // Do not expose whether a registration email already belongs to an account.
        // The verified registration transaction remains the authority for uniqueness.
        int result = 20;
        
        // 10: 중복 존재, 20: 중복 없음
        if(10 == result) {
        	message.setMsgId("10");
        	message.setMsgContents("해당 이메일은 사용할 수 없습니다");
        	
        } else if(20 == result) {
        	message.setMsgId("20");
        	message.setMsgContents("사용할 수 있는 이메일입니다");
        } 
        
       jsonString = new Gson().toJson(message);
       LOG.debug("└────────────────────────────────────────────────────────┘");
        
       return jsonString;
	}
	
    // 회원가입
	@RequestMapping(value = "/register", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String membershipRegister(MemberVO user, String verificationToken, HttpSession session) throws Exception {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ LoginController membershipRegister()                    │");
        LOG.debug("└────────────────────────────────────────────────────────┘");

		if (!CredentialPolicy.isValidUserId(user.getId())
				|| !CredentialPolicy.isValidPassword(user.getPassword())) {
			return messageJson("20", "아이디 또는 비밀번호 형식을 확인해주세요.");
		}

		String normalizedEmail;
		try {
			normalizedEmail = emailVerificationService.normalizeEmail(user.getEmail());
		} catch (IllegalArgumentException exception) {
			return messageJson("20", "올바른 이메일 주소를 입력해주세요.");
		}
		if (!emailVerificationService.consume(
				session,
				normalizedEmail,
				Purpose.REGISTRATION,
				verificationToken)) {
			return messageJson("20", "인증이 완료된 동일한 이메일로 가입해주세요.");
		}
		user.setEmail(normalizedEmail);
       
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

	private String messageJson(String id, String contents) {
		return new Gson().toJson(new MessageVO(id, contents));
	}

}
