package com.roadscanner.dao.result;

import com.roadscanner.domain.result.ResultImgVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dao-test-context.xml")
@Transactional
public class ResultImgDaoImplTest {

    @Autowired
    private ResultImgDao dao;

    @Test
    public void findsResultImageByNumber() throws Exception {
        ResultImgVO found = dao.getResultImg(new ResultImgVO(1, null, null, null));

        assertThat(found).isNotNull();
        assertThat(found.getNo()).isEqualTo(1);
        assertThat(found.getName()).isEqualTo("cross.png");
        assertThat(found.getContent()).isEqualTo("十자형 교차로");
        assertThat(found.getUrl()).isEqualTo("https://example.test/cross.png");
    }

    @Test
    public void returnsNullWhenResultImageDoesNotExist() throws Exception {
        assertThat(dao.getResultImg(new ResultImgVO(999, null, null, null))).isNull();
    }
}
