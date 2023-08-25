package com.roadscanner.service.user;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class MailSendService {

	@Autowired
	private JavaMailSenderImpl mailSender;
	private int authNumber;

	// 랜덤 난수(인증번호) 발행
	public void makeRandomNumber() {
		// 난수의 범위 111111 ~ 999999 (6자리 난수)
		Random r = new Random();
		int checkNum = r.nextInt(888888) + 111111;
		System.out.println("인증번호 : " + checkNum);
		authNumber = checkNum;
	}

	// 이메일 보낼 양식 작성
	public String joinEmail(String email) throws UnsupportedEncodingException, MessagingException {
		makeRandomNumber();
		
		MimeMessage message =  mailSender.createMimeMessage();
		//sitery@naver.com
		message.setFrom(new InternetAddress("sitery@naver.com","팀 로드 스캐너","UTF-8")); // email-config에 설정한 자신의 이메일 주소를 입력
		message.setSubject("회원 가입 인증 이메일 입니다.");// 이메일 제목
		message.setText("홈페이지를 방문해주셔서 감사합니다.\n" 
				 + "인증 번호는 " + authNumber + "입니다.\n" 
				+ "해당 인증번호를 인증번호 확인란에 기입하여 주세요."); // 이메일 내용 삽입
		message.addRecipient(RecipientType.TO, new InternetAddress(email,"","UTF-8"));
		this.mailSender.send(message);	
		return Integer.toString(authNumber);
	}
	
	// 비밀번호 재설정 페이지 이메일 양식
	public String change_mailCheck(String email)  throws UnsupportedEncodingException, MessagingException {
		makeRandomNumber();
		
		MimeMessage message =  mailSender.createMimeMessage();
		//sitery@naver.com
		message.setFrom(new InternetAddress("sitery@naver.com","팀 로드 스캐너","UTF-8")); // email-config에 설정한 자신의 이메일 주소를 입력
		message.setSubject("[RoadScanner] 비밀번호 재설정 확인 메일");// 이메일 제목
		message.setText("서비스를 이용해주셔서 감사합니다.\n" 
				 + "인증 번호는 " + authNumber + "입니다.\n" 
				+ "해당 인증번호를 인증번호 확인란에 기입하여 주세요."); // 이메일 내용 삽입
		message.addRecipient(RecipientType.TO, new InternetAddress(email,"","UTF-8"));
		this.mailSender.send(message);	
		return Integer.toString(authNumber);
	}
	
	// 찾는 아이디를 이메일로 전송
	public String findId(String email, String id)  throws UnsupportedEncodingException, MessagingException {
		
		MimeMessage message =  mailSender.createMimeMessage();
		
		message.setFrom(new InternetAddress("sitery@naver.com","팀 로드 스캐너","UTF-8"));  // email-config에 설정한 자신의 이메일 주소를 입력
		message.setSubject("[RoadScanner] ID 찾기 요청 메일");// 이메일 제목
		message.setText("저희 서비스를 이용해주셔서 감사합니다. \n" 
				 + "찾으시는 ID는 " + id + " 입니다.\n" 
				+ "감사합니다."); // 이메일 내용 삽입
		message.addRecipient(RecipientType.TO, new InternetAddress(email,"","UTF-8"));
		this.mailSender.send(message);
		return email;
	}
	
}
