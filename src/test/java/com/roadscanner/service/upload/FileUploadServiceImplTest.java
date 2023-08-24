package com.roadscanner.service.upload;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.dao.upload.FileUploadDao;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.service.upload.FileUploadService;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/root-context.xml"
                                  ,"file:src/main/webapp/WEB-INF/servlet-context.xml"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileUploadServiceImplTest implements PcwkLogger {
	
	@Autowired
	FileUploadDao dao;
	@Autowired
	FileUploadService service;
	
	FileUploadVO uploadVO; //임의의 파일 업로드 테스트용
	FileUploadVO inVO;     //올라간 파일 단건 조회 조건용
	
	MockMultipartFile mockFile;

	@Before
	public void setUp() throws Exception {
		LOG.debug("┌───────────────────┐");
		LOG.debug("│      setUp()      │");
		LOG.debug("└───────────────────┘");
		
        // 업로드할 가상의 파일 생성
        mockFile = new MockMultipartFile(
            "dog.jpg",     // 파일명
            "dog.jpg",     // 오리지널 파일명
            "image/jpeg",  // 파일 타입
            getClass().getResourceAsStream("/img/selectButton.jpg") // 파일 스트림
        );

        // 업로드할 파일과 관련된 정보 설정
        uploadVO = new FileUploadVO(1, "admin", 10, "20000215143200", "", "", 0, 0, 0, 0, 0);
        
        inVO = new FileUploadVO();
	}

	@Test
	public void uploadAndGet() throws SQLException, IOException {
		// 1. 파일 1건 업로드
		// 2. 업로드 여부 확인
		// 3. DB에 저장된 파일과 S3에 저장된 파일의 파일명 비교
		
		LOG.debug("┌────────────────────┐");
		LOG.debug("│      upload()      │");
		LOG.debug("└────────────────────┘");
		
		// 1.
		// 3.단계에 S3에서 불러올 파일명 만들어두기
		Date currentDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String datestr  = dateFormat.format(currentDate);
		String thisFileName = datestr+"_"+mockFile.getOriginalFilename();
		// doSave 실행
		String resultUpload = service.doSave(mockFile, uploadVO);
		
		// 2.
		assertNotEquals("0", resultUpload);
		
		// 3.
		// 3.1.이번 테스트에서 DB에 저장된 파일 outVO
		inVO.setName(thisFileName);
		FileUploadVO outVO = service.doSelectOne(inVO);
		
		// 3.2.S3에서 이번에 업로드한 파일 불러오기
        String resource = "config/upload.properties";
        Properties properties = new Properties();
        Reader reader = Resources.getResourceAsReader(resource);
        properties.load(reader);
        
		String thisUrl = getFileUrl(properties.getProperty("cloud.aws.s3.bucket"),
				   properties.getProperty("cloud.aws.region.static"),
				   properties.getProperty("cloud.aws.credentials.accessKey"),
				   properties.getProperty("cloud.aws.credentials.secretKey"),
				   resultUpload);
		
		// 3.1./3.2. 비교
		assertEquals(outVO.getName(), thisUrl.substring(64));
		
	}
	
	// S3에서 url 불러오기
    public static String getFileUrl(String bucketName, String region, String accessKey, String secretKey, String fileName) {
        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();

        try {
            String url = s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(fileName)).toExternalForm();
            s3Client.close();
        	LOG.debug("*************************");
        	LOG.debug("*Get Url Success!       *");
        	LOG.debug("*Url: "+url);
        	LOG.debug("*S3 uploaded fileName: "+url.substring(64));
        	LOG.debug("*************************");
            return url;
        } catch (S3Exception e) {
            System.err.println("Error getting file URL: " + e.getMessage());
            throw e;
        }
    }

}
