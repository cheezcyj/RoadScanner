package com.roadscanner.service.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.dao.upload.FileUploadDao;
import com.roadscanner.domain.upload.FileUploadVO;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class FileUploadServiceImpl implements PcwkLogger, FileUploadService {

	@Autowired
	@Qualifier(value = "fileUploadDaoImpl")
	FileUploadDao dao;

	//@Autowired
	FileUploadVO uploadVO;
	
    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;
    
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    
    @Value("${cloud.aws.s3.bucket2}")
    private String bucket2;

	// 사진 저장하면 checked = 1, S3 버킷 이동
	@Override
	public int checkedUpdate(FileUploadVO inVO) throws SQLException, IOException {

		int flag = 0;

		String sourceKey = inVO.getName();
		String destinationKey = inVO.getName();

		// AWS credentials 설정
		BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(region).build();
		try {
			// 객체 복사 시작
			CopyObjectRequest copyObjRequest = new CopyObjectRequest(bucket, sourceKey, bucket2,
					destinationKey);

			CopyObjectResult copyObjectResult = s3Client.copyObject(copyObjRequest);

			// 복사가 성공했다면 원본 객체 삭제, url 버킷 변경
			if (copyObjectResult != null) {

				s3Client.deleteObject(bucket, sourceKey);

				StringBuffer newUrl = new StringBuffer();
				newUrl.append(inVO.getUrl());
				newUrl.insert(31, "2");
				LOG.debug("newUrl: " + newUrl.toString());

				inVO.setChecked(1);
				inVO.setUrl(newUrl.toString());
				LOG.debug("inVO: " + inVO.getUrl());

				flag = dao.doUpdate(inVO);

				LOG.debug("***********");
				LOG.debug("파일 이동 완료");
				LOG.debug("***********");

			} else {
				LOG.debug("파일 이동 실패");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return flag;
	}

	@Override
	public List<FileUploadVO> monthlyFeedback(FileUploadVO inVO) throws SQLException {

		return dao.monthlyFeedback(inVO);
	}

	@Override
	public FileUploadVO totalFeedback(FileUploadVO inVO) throws SQLException {

		return dao.totalFeedback(inVO);
	}

	@Override
	public List<FileUploadVO> doRetrieveByCategory(FileUploadVO inVO) throws SQLException {

		return dao.doRetrieveByCategory(inVO);
	}
	
	@Override
	public List<FileUploadVO> doRetrieve(FileUploadVO inVO) throws SQLException {

		return dao.doRetrieve(inVO);
	}

	@Override
	public FileUploadVO doSelectOne(FileUploadVO inVO) throws SQLException {

		return dao.doSelectOne(inVO);
	}

	@Override
	public int doUpdate(FileUploadVO inVO) throws SQLException {

		return dao.doUpdate(inVO);
	}

	@Override
	public int doDelete(FileUploadVO inVO) throws SQLException, IOException {

		uploadVO = deleteFileToS3(inVO);

		return dao.doDelete(uploadVO);
	}

	private FileUploadVO deleteFileToS3(FileUploadVO inVO) throws IOException {
		LOG.debug("┌────────────────────┐");
		LOG.debug("│   deleteFileToS3   │");
		LOG.debug("└────────────────────┘");

		String objectKey = inVO.getName(); // 파일의 키

		// AWS credentials 설정
		BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

		// Amazon S3 클라이언트 생성
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(region) // 적절한 리전 설정
				.build();

		try {
			// S3 객체 삭제
			s3Client.deleteObject(bucket, objectKey);
			LOG.debug("***********");
			LOG.debug("S3 버킷에서 삭제");
			LOG.debug("***********");
		} catch (Exception e) {
			e.printStackTrace();
			LOG.debug("S3에서 삭제 실패: " + e.getMessage());
		}

		return inVO;

	}

	@Override
	public String doSave(MultipartFile file, FileUploadVO inVO) throws SQLException, IOException {
		uploadVO = uploadFileToS3(file, inVO);

		if (1 == dao.doSave(uploadVO)) {
			return uploadVO.getName();
		} else {
			return "0";
		}
	}

	private FileUploadVO uploadFileToS3(MultipartFile file, FileUploadVO uploadVO) throws IOException {
		LOG.debug("┌────────────────────┐");
		LOG.debug("│UploadFile to Bucket│");
		LOG.debug("│Bucket properties: "+region+bucket);

		// 파일 이름 형식(yyyyMMddHH24MISS_원본파일명), 등록일, url, 파일 크기 설정
		Date currentDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

		String datestr = dateFormat.format(currentDate);
		String fileName = datestr + "_" + file.getOriginalFilename();
		String url = "https://" + bucket + ".S3." + region + ".amazonaws.com/" + fileName;
		double fileSize = file.getSize() / 1024.0;

		File convertedFile = new File("/tmp/" + file.getOriginalFilename()); // 임시 디렉토리에 저장
		try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
		    fos.write(file.getBytes());
		} catch (IOException e) {
		    e.printStackTrace();
		    LOG.debug("Error: " + e.getMessage());
		}

		S3Client s3Client = S3Client.builder().region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.build();
		
		LOG.debug("└────────────────────┘");

		try {
			PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(fileName).build();

			s3Client.putObject(request, convertedFile.toPath());
			LOG.debug("**********************");
			LOG.debug("*File upload Success!*");
			LOG.debug("**********************");

			uploadVO.setUploadDate(datestr);
			uploadVO.setName(fileName);
			uploadVO.setUrl(url);
			uploadVO.setFileSize(fileSize);

			String voString = uploadVO.toString();
			LOG.debug("┌──────────┐");
			LOG.debug("│ uploadVO: " + voString);
			LOG.debug("└──────────┘");

			return uploadVO;
		} catch (S3Exception e) {
			System.err.println("Error: " + e.getMessage());
			throw e;

		} finally {
			// 클라이언트 리소스 정리
			s3Client.close();
		}
	}

}