package com.roadscanner.service.upload;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.roadscanner.domain.upload.FileUploadVO;

public interface FileUploadService {
	
	// 피드백 분기별 그래프
	List<FileUploadVO> quarterlyFeedback(FileUploadVO inVO) throws SQLException;

	// 피드백 누적 그래프, 표
	FileUploadVO totalFeedback(FileUploadVO inVO) throws SQLException;

	// 사진 목록 조회
	List<FileUploadVO> doRetrieve(FileUploadVO inVO) throws SQLException;

	// 사진 상세 조회
	FileUploadVO doSelectOne(FileUploadVO inVO) throws SQLException;
	
	// 사진 구분, 싫어요 이유, 검토 여부 수정
	int doUpdate(FileUploadVO inVO) throws SQLException;
	
	// 사진 저장하면 checked = 1, S3 버킷 이동
	int checkedUpdate(FileUploadVO inVO) throws SQLException, IOException;
	
	// S3와 DB에서 단 건 사진 삭제
	int doDelete(FileUploadVO inVO) throws SQLException, IOException;

	// S3에 사진 업로드, 정보는 DAO 전송
	String doSave(MultipartFile file, FileUploadVO inVO) throws SQLException, IOException;

}
