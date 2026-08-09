package com.roadscanner.service.user;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class MailSendService {

	private static final String FROM_NAME = "RoadScanner";
	private static final String UTF_8 = StandardCharsets.UTF_8.name();

	private final JavaMailSender mailSender;
	private final String fromAddress;

	@Autowired
	public MailSendService(
			JavaMailSender mailSender,
			@Value("${roadscanner.mail.from}") String fromAddress) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
	}

	public void sendRegistrationVerification(String email, String verificationCode)
			throws UnsupportedEncodingException, MessagingException {
		sendVerificationEmail(
				email,
				verificationCode,
				"회원 가입 인증 이메일 입니다.",
				"홈페이지를 방문해주셔서 감사합니다.\n");
	}
	
	public void sendPasswordResetVerification(String email, String verificationCode)
			throws UnsupportedEncodingException, MessagingException {
		sendVerificationEmail(
				email,
				verificationCode,
				"[RoadScanner] 비밀번호 재설정 확인 메일",
				"서비스를 이용해주셔서 감사합니다.\n");
	}
	
	// 찾는 아이디를 이메일로 전송
	public String findId(String email, String id) throws UnsupportedEncodingException, MessagingException {
		
		MimeMessage message = mailSender.createMimeMessage();
		
		message.setFrom(new InternetAddress(fromAddress, FROM_NAME, UTF_8));
		message.setSubject("[RoadScanner] ID 찾기 요청 메일", UTF_8);
		message.setText("저희 서비스를 이용해주셔서 감사합니다. \n" 
				 + "찾으시는 ID는 " + id + " 입니다.\n" 
				+ "감사합니다.", UTF_8);
		message.addRecipient(RecipientType.TO, checkedRecipient(email));
		this.mailSender.send(message);
		return email;
	}

	private void sendVerificationEmail(
			String email,
			String verificationCode,
			String subject,
			String introduction) throws UnsupportedEncodingException, MessagingException {
		if (verificationCode == null || !verificationCode.matches("\\d{6}")) {
			throw new IllegalArgumentException("verification code must contain six digits");
		}

		MimeMessage message = mailSender.createMimeMessage();
		message.setFrom(new InternetAddress(fromAddress, FROM_NAME, UTF_8));
		message.setSubject(subject, UTF_8);
		message.setText(introduction
				+ "인증 번호는 " + verificationCode + "입니다.\n"
				+ "해당 인증번호를 인증번호 확인란에 기입하여 주세요.", UTF_8);
		message.addRecipient(RecipientType.TO, checkedRecipient(email));
		this.mailSender.send(message);
	}

	private InternetAddress checkedRecipient(String email) throws MessagingException {
		if (email == null || email.indexOf('\r') >= 0 || email.indexOf('\n') >= 0) {
			throw new IllegalArgumentException("invalid email address");
		}

		InternetAddress recipient = new InternetAddress(email, true);
		recipient.validate();
		return recipient;
	}
	
}
