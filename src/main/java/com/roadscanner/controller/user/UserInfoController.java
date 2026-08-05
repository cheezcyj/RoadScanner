package com.roadscanner.controller.user;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

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
import com.roadscanner.service.user.MailSendService;
import com.roadscanner.service.user.UserService;

@Controller
public class UserInfoController {
	
	@Autowired
	UserService userService;
	
	@Autowired
	MailSendService mailSend;

	@Autowired
	EmailVerificationService emailVerificationService;

	@Autowired(required = false)
	ClientAddressResolver clientAddressResolver;
	
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
	
	// 비밀번호 재설정 인증번호 전송. 인증번호는 HTTP 응답에 포함하지 않는다.
    @RequestMapping(
			value = "/change_mailCheck",
			method = RequestMethod.POST,
			produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String change_mailCheck(
			String email,
			HttpSession session,
			HttpServletRequest request)
			throws UnsupportedEncodingException, MessagingException, SQLException {
    	
		LOG.debug("비밀번호 재설정 페이지 이메일 인증 요청");
		String normalizedEmail;
		try {
			normalizedEmail = emailVerificationService.normalizeEmail(email);
		} catch (IllegalArgumentException exception) {
			return messageJson("20", "올바른 이메일 주소를 입력해주세요.");
		}

		MemberVO emailQuery = new MemberVO();
		emailQuery.setEmail(normalizedEmail);
		boolean registeredEmail = 10 == userService.doEmailDuplCheck(emailQuery);

		String verificationCode;
		try {
			verificationCode = emailVerificationService.createChallenge(
					session,
					normalizedEmail,
					Purpose.PASSWORD_RESET,
					clientAddress(request));
		} catch (RateLimitExceededException exception) {
			return messageJson("20", "인증번호 전송 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
		}

		if (!registeredEmail) {
			// 가입 여부를 노출하지 않되, 동일한 발송 제한 상태를 적용한다.
			emailVerificationService.clearChallenge(
					session,
					normalizedEmail,
					Purpose.PASSWORD_RESET,
					verificationCode);
			return messageJson("10", "가입된 이메일이라면 인증번호가 전송됩니다.");
		}

		try {
			mailSend.sendPasswordResetVerification(normalizedEmail, verificationCode);
		} catch (UnsupportedEncodingException | MessagingException | MailException exception) {
			emailVerificationService.clearChallenge(
					session,
					normalizedEmail,
					Purpose.PASSWORD_RESET,
					verificationCode);
			LOG.warn("password_reset_mail_delivery_failed");
		} catch (RuntimeException exception) {
			emailVerificationService.clearChallenge(
					session,
					normalizedEmail,
					Purpose.PASSWORD_RESET,
					verificationCode);
			throw exception;
		}
		return messageJson("10", "가입된 이메일이라면 인증번호가 전송됩니다.");
	}

	String change_mailCheck(String email, HttpSession session)
			throws UnsupportedEncodingException, MessagingException, SQLException {
		return change_mailCheck(email, session, null);
	}

	@RequestMapping(
			value = "/change_mailCheck/verify",
			method = RequestMethod.POST,
			produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String verifyPasswordResetEmail(
			String email,
			String code,
			HttpSession session,
			HttpServletResponse response) {
		VerificationOutcome outcome = emailVerificationService.verify(
				session,
				email,
				Purpose.PASSWORD_RESET,
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
	
	// 마이페이지 비밀번호 수정
	@RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String doChangeInfo(MemberVO member,
			@SessionAttribute("user") MemberVO authenticatedUser,
			HttpSession session) throws Exception {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ UserInfoController doChangeInfo()                      │");
        LOG.debug("└────────────────────────────────────────────────────────┘");
             
        if (!CredentialPolicy.isValidPassword(member.getPassword())) {
            return messageJson("20", "비밀번호는 8~20자의 문자, 숫자, 특수문자를 포함해야 합니다.");
        }

        member.setId(authenticatedUser.getId());
        member.setEmail(authenticatedUser.getEmail());
        int flag = this.userService.doChangeInfo(member);
        String jsonString = "";
        MessageVO message = new MessageVO();
              
        if(flag == 1) {
			session.invalidate();
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
             
		String jsonString;
		try {
			emailVerificationService.normalizeEmail(user.getEmail());
		} catch (IllegalArgumentException exception) {
			return messageJson("20", "올바른 이메일 주소를 입력해주세요.");
		}

		jsonString = messageJson("10", "가입된 이메일이라면 인증번호를 전송합니다. 계속하시겠습니까?");
       LOG.debug("└────────────────────────────────────────────────────────┘");     
       return jsonString;
	}	
    
    // 비밀번호 재설정 페이지 > 비밀번호 수정
    @RequestMapping(value = "/changePassword", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String changePw(MemberVO member, String verificationToken, HttpSession session) throws Exception {
		LOG.debug("┌────────────────────────────────────────────────────────┐");
        LOG.debug("│ UserInfoController changePw()                          │");
        LOG.debug("└────────────────────────────────────────────────────────┘");

		if (!CredentialPolicy.isValidPassword(member.getPassword())) {
			return messageJson("20", "비밀번호는 8~20자의 문자, 숫자, 특수문자를 포함해야 합니다.");
		}

		String normalizedEmail;
		try {
			normalizedEmail = emailVerificationService.normalizeEmail(member.getEmail());
		} catch (IllegalArgumentException exception) {
			return messageJson("20", "올바른 이메일 주소를 입력해주세요.");
		}
		if (!emailVerificationService.consume(
				session,
				normalizedEmail,
				Purpose.PASSWORD_RESET,
				verificationToken)) {
			return messageJson("20", "인증이 완료된 동일한 이메일만 재설정할 수 있습니다.");
		}
		member.setEmail(normalizedEmail);
        
        int flag = this.userService.changePw(member);
        
        String jsonString = "";
        MessageVO message = new MessageVO();
        
        if(1 == flag) {
			session.invalidate();
        	message.setMsgId("10");
        	message.setMsgContents("비빌번호를 재설정했습니다");
        	
        } else {
        	message.setMsgId("20");
        	message.setMsgContents("비밀번호 재설정에 실패했습니다.");
        }
        
       jsonString = new Gson().toJson(message);  
       return jsonString;
        
	}

	private String messageJson(String id, String contents) {
		return new Gson().toJson(new MessageVO(id, contents));
	}

	private String clientAddress(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		return clientAddressResolver == null
				? request.getRemoteAddr()
				: clientAddressResolver.resolve(request);
	}

}
